package com.ibm.bmcshell.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.CustomPromptProvider;
import com.ibm.bmcshell.EthCommands;
import com.ibm.bmcshell.Utils;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class CommonRest  {
    @Autowired
    private ApplicationContext applicationContext;

    WebClient client;
    String token;
    String machine;
    CommonRest() throws SSLException {

        if(client ==null) {
            client=Utils.createWebClient();
        }
    }
    String base(){
        return Utils.base(machine);
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
                        .uri(new URI(String.format("https://%s.aus.stglabs.ibm.com/redfish/v1/SessionService/Sessions",machine)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(String.format("{\"UserName\":\"%s\", \"Password\":\"%s\"}",CommonCommands.getUserName(),CommonCommands.getPasswd())))
                        .retrieve()
                        .toEntity(String.class)
                        .subscribe(resp->{
                            HttpHeaders responseHeaders = resp.getHeaders();

                            List<String> headerValue = responseHeaders.get("X-Auth-Token");
                            token= headerValue.stream().limit(1).reduce((a,b)->a).orElse("");
                            getPromptProvider().setShellData(new CustomPromptProvider.ShellData(machine, AttributedStyle.GREEN));
                        });



            }catch (Exception ex){
                System.out.println("Could not get token");
                getPromptProvider().setShellData(new CustomPromptProvider.ShellData(machine,AttributedStyle.RED));
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
            machine=m;
    }
    @RequestMapping("/apis")
    Mono<String> restApis() throws URISyntaxException, JsonProcessingException {
        return makeGetRequestMono("").map(res->{
            return getResource(res);
        });


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
    String docoratedJson(String src)
    {
        return String.format(Templates.jsonTemplate,src,machine);

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
