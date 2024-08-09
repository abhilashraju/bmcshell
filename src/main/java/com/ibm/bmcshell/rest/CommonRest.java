package com.ibm.bmcshell.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.CustomPromptProvider;
import com.ibm.bmcshell.EthCommands;
import com.ibm.bmcshell.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class CommonRest  {
     static public class CustomHttpHeaders extends HttpHeaders {
        private final MultiValueMap<String, String> originalHeaders = new LinkedMultiValueMap<>();

        @Override
        public void add(String headerName, String headerValue) {
            super.add(headerName, headerValue);
            originalHeaders.computeIfAbsent(headerName, k -> new ArrayList<>()).add(headerValue);
        }


        public List<String> get(String headerName) {
            return originalHeaders.getOrDefault(headerName, super.get(headerName));
        }

        public MultiValueMap<String, String> getOriginalHeaders() {
            return originalHeaders;
        }
    }
    @Autowired
    private ApplicationContext applicationContext;

    WebClient client;
    String token;

    @Autowired
    private HttpServletRequest request;
    CommonRest() throws IOException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        if(client ==null) {
            client=Utils.createWebClient();
        }
    }
    String base(){
        return Utils.base(CommonCommands.machine);
    }
    CustomPromptProvider getPromptProvider(){
        return applicationContext.getBean(CustomPromptProvider.class);
    }
    List<String> listOfMachines() throws IOException {
        return Utils.listOfMachines().stream().map(a->a.url).collect(Collectors.toList());
    }
    protected Mono<ResponseEntity<String>> makeGetRequestMono(String target) throws URISyntaxException {
        var auri=new URI(base()+target);
        return Utils.tryUntil(3, () -> {
            try {
                return client.get()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .toEntity(String.class).doOnSubscribe(s->{
                            if(token==null){
                                resetToken();
                            }
                        }).doOnError(ex->resetToken());
            }catch (Exception ex) {
                resetToken();
                throw ex;
            }

        });

    }
    public String getToken() {

        if(token==null){
            try {
                var response = client.post()
                        .uri(new URI(String.format("https://%s.aus.stglabs.ibm.com/redfish/v1/SessionService/Sessions",CommonCommands.machine)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(String.format("{\"UserName\":\"%s\", \"Password\":\"%s\"}",CommonCommands.getUserName(),CommonCommands.getPasswd())))
                        .retrieve()
                        .toEntity(String.class)
                        .subscribe(resp->{
                            HttpHeaders responseHeaders = resp.getHeaders();

                            List<String> headerValue = responseHeaders.get("X-Auth-Token");
                            token= headerValue.stream().limit(1).reduce((a,b)->a).orElse("");
                            getPromptProvider().setShellData(new CustomPromptProvider.ShellData(CommonCommands.machine, AttributedStyle.GREEN));
                        });



            }catch (Exception ex){
                System.out.println("Could not get token");
                getPromptProvider().setShellData(new CustomPromptProvider.ShellData(CommonCommands.machine,AttributedStyle.RED));
            }

        }
        return token;

    }
    void resetToken() {
        token=null;
        try{
            getToken();
        }catch (Exception ex){
            System.out.println("Token generation failed");
        }

    }
    protected void machine(String m) throws IOException {
            token=null;

    }
    @RequestMapping("/apis")
    Mono<String> restApis() throws URISyntaxException, JsonProcessingException {
        return makeGetRequestMono("").map(res->{
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

//        System.out.println(requestData);
        ObjectMapper mapper=new ObjectMapper();
        var tree=mapper.readTree(requestData);
        var filtered=StreamSupport.stream(tree.get("Events").spliterator(),false)
                .filter(a->{
                    if(Utils.eventFilters().equals("*")){
                        return true;
                    }
                    return Arrays.stream(Utils.eventFilters().split(",")).filter(f->{
                        return a.get("OriginOfCondition").asText().contains(f);
                    }).count()>0;
                }).collect(Collectors.toList());

        filtered.forEach(a->System.out.println(a.toPrettyString()));

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

    @RequestMapping(value = {"/redfish","/redfish/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.TRACE})
    @ResponseBody
    public Mono<ResponseEntity<String>> forwardRedfishRequest(@RequestBody(required = false) String body) throws URISyntaxException {

        String path = request.getRequestURI(); // Extract the path
        String queryString = request.getQueryString(); // Extract the query string
        String url = path + (queryString != null ? "?" + queryString : ""); // Recreate the full URL

        URI targetUri = new URI(base() + url);


        CustomHttpHeaders headers = new CustomHttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.add(headerName, headerValue);
        }
        headers.add("Content-Type", request.getContentType());
        if(headers.get("X-Auth-Token")!=null && headers.get("X-Auth-Token").size()>0){
            token=headers.get("X-Auth-Token").get(0);

        }
        WebClient.RequestBodySpec requestSpec = client.method(HttpMethod.valueOf(request.getMethod()))
                .uri(targetUri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Auth-Token",token )
                .headers(httpHeaders -> headers.addAll(headers.getOriginalHeaders()));

        if (body != null) {
            requestSpec.bodyValue(body);
        }

        return requestSpec.retrieve()
                .toEntity(String.class)
                .map(response -> ResponseEntity.status(response.getStatusCode())
                        .headers(response.getHeaders())
                        .body(response.getBody()));
    }
    @RequestMapping("/goto")
    Mono<String> restApis(@RequestParam String url) throws URISyntaxException, JsonProcessingException {

        var index =url.indexOf("redfish/v1/");
        if(index != -1){
            url=url.substring("redfish/v1/".length()+1);
        }
        return makeGetRequestMono(url).map(res->{
             return getResource(res);
        });
    }
    @RequestMapping("/machine")
    Mono<String> setMachine(@RequestParam String m) throws URISyntaxException, IOException {
        machine(m);
        return restApis();
    }
    @RequestMapping("/machines")
    Mono<String> machines() throws URISyntaxException, IOException {

        var list= listOfMachines().stream().skip(1).map(a -> {
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
        InputStream resourceStream = classLoader.getResourceAsStream(path);
        if (resourceStream == null) {
            return Mono.just("Resource not found: " + path);
        }
        String content = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        return Mono.just(content);
    }
    String docoratedJson(String src)
    {
        return String.format(Templates.jsonTemplate,src,CommonCommands.machine);

    }
    private String getResource(ResponseEntity<String> res) {
        try {
            StringBuilder builder = new StringBuilder();


            ObjectMapper mapper= new ObjectMapper();
            var root=mapper.readTree(res.getBody());
            builder.append(docoratedJson(root.toString()));
            builder.append("<p>");
            var links=buildLinksAndTargets(root);
            AtomicInteger i= new AtomicInteger();
            var list= links.stream().map(a -> {
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
        return Utils.buildLinksAndTargets(root).stream().map(a->a.url).collect(Collectors.toList());
    }
}
