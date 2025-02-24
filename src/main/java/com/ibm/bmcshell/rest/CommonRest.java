package com.ibm.bmcshell.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.CustomPromptProvider;
import com.ibm.bmcshell.EthCommands;
import com.ibm.bmcshell.Utils.Util;
import com.ibm.bmcshell.redfish.MockUpFetcher;

import jakarta.servlet.http.HttpServletRequest;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.yaml.snakeyaml.Yaml;

import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import reactor.util.retry.Retry;

import java.time.Duration;

@RestController
public class CommonRest {

    public interface Converter {
        void convert(String path, InputStream input);
    }

    @Autowired
    private ApplicationContext applicationContext;

    WebClient client;
    String token;

    @Autowired
    private HttpServletRequest request;

    CommonRest() throws IOException, UnrecoverableKeyException, CertificateException, KeyStoreException,
            NoSuchAlgorithmException {

        if (client == null) {
            client = Util.createWebClient();
        }
    }

    String base() {
        return Util.base(CommonCommands.machine);
    }

    CustomPromptProvider getPromptProvider() {
        return applicationContext.getBean(CustomPromptProvider.class);
    }

    List<String> listOfMachines() throws IOException {
        return Util.listOfMachines().stream().map(a -> a.url).collect(Collectors.toList());
    }

    protected Mono<ResponseEntity<String>> makeGetRequestMono(String target) throws URISyntaxException {
        var auri = new URI(base() + target);
        return Util.tryUntil(3, () -> {
            try {
                return client.get()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .toEntity(String.class).doOnSubscribe(s -> {
                            if (token == null) {
                                resetToken();
                            }
                        }).doOnError(ex -> resetToken());
            } catch (Exception ex) {
                resetToken();
                throw ex;
            }

        });

    }

    public String getToken() {

        if (token == null) {
            try {
                token = CommonCommands.getAuthToken(client);

            } catch (Exception ex) {
                System.out.println("Could not get token");
                getPromptProvider()
                        .setShellData(new CustomPromptProvider.ShellData(CommonCommands.machine, AttributedStyle.RED));
            }

        }
        return token;

    }

    void resetToken() {
        token = null;
        try {
            getToken();
        } catch (Exception ex) {
            System.out.println("Token generation failed");
        }

    }

    protected void machine(String m) throws IOException {
        token = null;

    }

    @RequestMapping("/apis")
    Mono<String> restApis() throws URISyntaxException, JsonProcessingException {
        return makeGetRequestMono("").map(res -> {
            return getResource(res);
        });

    }

    @RequestMapping("/metrics")
    Mono<String> metrics(@RequestBody String requestData) throws URISyntaxException, JsonProcessingException {

        System.out.println(requestData);
        return Mono.just("Success");
    }

    @PostMapping("/events")
    public Mono<String> createResource(@RequestBody String requestData) throws JsonProcessingException {

        // System.out.println(requestData);
        ObjectMapper mapper = new ObjectMapper();
        var tree = mapper.readTree(requestData);
        var filtered = StreamSupport.stream(tree.get("Events").spliterator(), false)
                .filter(a -> {
                    if (Util.eventFilters().equals("*")) {
                        return true;
                    }
                    return Arrays.stream(Util.eventFilters().split(",")).filter(f -> {
                        return a.get("OriginOfCondition").asText().contains(f);
                    }).count() > 0;
                }).collect(Collectors.toList());

        filtered.forEach(a -> System.out.println(a.toPrettyString()));

        String message = "Resource created successfully!";
        return Mono.just(message);
    }

    @PostMapping("/testpost")
    public Mono<String> testpost(@RequestBody String requestData) throws JsonProcessingException {

        System.out.println(requestData);

        return Mono.just(requestData);
    }

    @RequestMapping("/testget")
    public Mono<String> testget() throws JsonProcessingException {

        return Mono.just("hello");
    }
    @RequestMapping("/mockup/{arg}")
    public Mono<ResponseEntity<Map<String, JsonNode>>> getMockups(@PathVariable String arg) throws IOException {
        Map<String, JsonNode> schemas = MockUpFetcher.fetch(arg);
        return Mono.just(ResponseEntity.ok(schemas));
    }
    @RequestMapping("/schemas")
    public Mono<ResponseEntity<Map<String, JsonNode>>> getSchemas() throws IOException {
        Map<String, JsonNode> schemas = readAllSchemaFiles(null);
        return Mono.just(ResponseEntity.ok(schemas));
    }

    @RequestMapping("/schema/{arg}")
    public Mono<ResponseEntity<Map<String, JsonNode>>> getSchemasByRegex(@PathVariable String arg) throws IOException {
        Map<String, JsonNode> schemas = readAllSchemaFiles(arg);
        return Mono.just(ResponseEntity.ok(schemas));
    }

    // Method to read all schema files
    private Map<String, JsonNode> readAllSchemaFiles(String regex) throws IOException {
        Map<String, JsonNode> schemas = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        getFileData(regex, Util.schemaroot, ".json",
                (String path, InputStream input) -> {
                    try {
                        JsonNode schema = objectMapper.readTree(input);
                        String id = schema.path("$id").asText();
                        schemas.put(id, schema);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return schemas;
    }

    @RequestMapping("/yaml/{arg}")
    public Mono<ResponseEntity<Map<String, Map<String, Object>>>> getYamlsByRegex(@PathVariable String arg)
            throws IOException {
        Map<String, Map<String, Object>> yamlList = readAllYamlFiles(arg);
        return Mono.just(ResponseEntity.ok(yamlList));
    }

    // Method to read and merge all YAML files
    // Method to read and append all YAML files
    private Map<String, Map<String, Object>> readAllYamlFiles(String regex) throws IOException {
        Map<String, Map<String, Object>> yamlList = new HashMap<>();
        Yaml yaml = new Yaml();
        getFileData(regex, Util.yamlRoot, ".yaml",
                (String path, InputStream input) -> {
                    try {
                        Map<String, Object> yamlData = yaml.load(input);
                        var seg = path.split("/");
                        yamlList.put(seg[seg.length - 1], yamlData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        return yamlList;
    }

    private void mergeYamlMaps(Map<String, Object> base, Map<String, Object> toMerge) {
        for (Map.Entry<String, Object> entry : toMerge.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (base.containsKey(key) && base.get(key) instanceof Map && value instanceof Map) {
                mergeYamlMaps((Map<String, Object>) base.get(key), (Map<String, Object>) value);
            } else {
                base.put(key, value);
            }
        }
    }

    private void getFileData(String regex, String dir, String suffix, Converter converter) throws IOException {
        Pattern pattern = regex != null && !regex.equals("*") ? Pattern.compile(regex) : null;
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(suffix))
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        System.out.println("File Name: " + fileName);
                        if (pattern != null) {
                            Matcher matcher = pattern.matcher(fileName);
                            boolean matches = matcher.find();
                            return matches;
                        }
                        return true;
                    })
                    .forEach(path -> {
                        try {
                            converter.convert(path.toString(), Files.newInputStream(path));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @RequestMapping(value = { "/redfish", "/redfish/**" }, method = { RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS,
            RequestMethod.TRACE })
    @ResponseBody
    public Mono<ResponseEntity<String>> forwardRedfishRequest(@RequestBody(required = false) String body)
            throws URISyntaxException, SSLException {

        String path = request.getRequestURI(); // Extract the path
        String queryString = request.getQueryString(); // Extract the query string
        String url = path + (queryString != null ? "?" + queryString : ""); // Recreate the full URL

        URI targetUri = new URI(base() + url);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.add(headerName, headerValue);
        }
        System.out.println("Try forwarding with retries: " + targetUri.toString());
        return makeRequest(targetUri.toString(), headers, body, HttpMethod.valueOf(request.getMethod()))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()));
    }

    public Mono<ResponseEntity<String>> makeRequest(String targetUri, HttpHeaders headers, Object body,
            HttpMethod method) throws URISyntaxException, SSLException {
        WebClient.RequestBodySpec requestSpec = Util.createWebClient().method(method)
                .uri(targetUri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> httpHeaders.addAll(headers));

        if (body != null) {
            requestSpec.bodyValue(body);
        }

        return requestSpec.retrieve()
                .toEntity(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().is5xxServerError()) {
                        return Mono.error(ex);
                    }
                    return Mono.just(ResponseEntity.status(ex.getStatusCode()).headers(ex.getHeaders()).body(ex.getResponseBodyAsString()));
                });
    }

    @RequestMapping("/goto")
    Mono<String> restApis(@RequestParam String url) throws URISyntaxException, JsonProcessingException {

        var index = url.indexOf("redfish/v1/");
        if (index != -1) {
            url = url.substring("redfish/v1/".length() + 1);
        }
        return makeGetRequestMono(url).map(res -> {
            return getResource(res);
        });
    }

    @RequestMapping("/interfaces")
    public Mono<ResponseEntity<Map<String, Object>>> getInterfaces(@RequestParam String f) throws IOException {
        Map<String, Object> directoryContent = readDirectoryContent(Paths.get(Util.interfacesRoot), f);
        return Mono.just(ResponseEntity.ok(directoryContent));
    }

    @RequestMapping("/directory")
    public Mono<ResponseEntity<Map<String, Object>>> getDirectoryContent(@RequestParam String path) throws IOException {
        Map<String, Object> directoryContent = readDirectoryContent(Paths.get(path), ".*");
        return Mono.just(ResponseEntity.ok(directoryContent));
    }

    // Method to read directory content recursively
    private Map<String, Object> readDirectoryContent(Path dirPath, String regex) throws IOException {
        Map<String, Object> contentMap = new HashMap<>();

        try (Stream<Path> paths = Files.walk(dirPath, 1)) {
            paths.filter(path -> !path.equals(dirPath)).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        var subDirContent = readDirectoryContent(path, regex);
                        if (!subDirContent.isEmpty()) {
                            contentMap.put(path.getFileName().toString(), subDirContent);
                        }
                    } else {
                        if (path.getFileName().toString().matches(regex)) {
                            contentMap.put(path.getFileName().toString(), new String(Files.readAllBytes(path)));
                        }

                    }
                } catch (IOException e) {
                    // e.printStackTrace();
                }
            });
        }

        return contentMap;
    }

    @RequestMapping("/machine")
    Mono<String> setMachine(@RequestParam String m) throws URISyntaxException, IOException {
        machine(m);
        return restApis();
    }

    @RequestMapping("/machines")
    Mono<String> machines() throws URISyntaxException, IOException {

        var list = listOfMachines().stream().skip(1).map(a -> {
            var r = "<a href=\"machine?m=" + a + "\">" + a + "</a>";
            return r;
        }).reduce((a, b) -> a + "<P>" + b).orElse("Null");
        return Mono.just(list);
    }

    @RequestMapping("/")
    Mono<String> root() throws URISyntaxException, IOException {
        return getFile("loginview.html");
    }

    @RequestMapping("/**")
    @ResponseBody
    public Mono<String> handleDefaultRequest(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return getFile(path);
    }

    private Mono<String> getFile(String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        return Optional.ofNullable(classLoader.getResourceAsStream(path))
                .map(resourceStream -> {
                    try {
                        return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .or(() -> {
                    if (Util.webroot != null) {
                        try {
                            Path filePath = Paths.get(Util.webroot + File.separator + path);
                            if (Files.exists(filePath)) {
                                return Optional.of(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    return Optional.empty();
                })
                .map(Mono::just)
                .orElse(Mono.just("Resource not found: " + path));
    }

    String docoratedJson(String src) {
        return String.format(Templates.jsonTemplate, src, CommonCommands.machine);

    }

    private String getResource(ResponseEntity<String> res) {
        try {
            StringBuilder builder = new StringBuilder();

            ObjectMapper mapper = new ObjectMapper();
            var root = mapper.readTree(res.getBody());
            builder.append(docoratedJson(root.toString()));
            builder.append("<p>");
            var links = buildLinksAndTargets(root);
            AtomicInteger i = new AtomicInteger();
            var list = links.stream().map(a -> {
                var r = "<a href=\"goto?url=" + a + "\">" + i + ")" + a + "</a>";
                i.getAndIncrement();

                return r;
            }).reduce((a, b) -> a + "<P>" + b).orElse("Null");
            builder.append(list);
            return builder.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> buildLinksAndTargets(JsonNode root) {
        return Util.buildLinksAndTargets(root).stream().map(a -> a.url).collect(Collectors.toList());
    }
}
