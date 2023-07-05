package com.ibm.bmcshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommonCommands implements ApplicationContextAware {
    WebClient client;

    String token;
    public String base(){
        return Utils.base(machine);
    }
    String machine;
    @Autowired
    private ApplicationContext applicationContext;
    static String userName;
    static String passwd;

    public  static  String getUserName(){
        return userName;
    }
    public  static  String getPasswd(){
        return passwd;
    }
    CustomPromptProvider getPromptProvider(){
        return applicationContext.getBean(CustomPromptProvider.class);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected Stack<List<Utils.EndPoints>> endPoints=new Stack<>();

    protected CommonCommands() throws IOException {
        if(client ==null) {
            client = Utils.createWebClient();
            File file= new File("history");
            if(file.exists()){
                BufferedInputStream bufferedInputStream= new BufferedInputStream(new FileInputStream(file));
                var data=new String(bufferedInputStream.readAllBytes()).split(",");
                machine=data[0];
                if(data.length>1){
                    userName=data[1];
                    passwd=data[2];
                }

            }

        }
    }
    public String getToken() {

        if(token==null){
            try {
                var response = client.post()

                        .uri(new URI(String.format("https://%s.aus.stglabs.ibm.com/redfish/v1/SessionService/Sessions",machine)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(String.format("{\"UserName\":\"%s\", \"Password\":\"%s\"}",userName,passwd)))
                        .retrieve()
                        .toEntity(String.class)
                        .block();


                HttpHeaders responseHeaders = response.getHeaders();

                List<String> headerValue = responseHeaders.get("X-Auth-Token");
                token= headerValue.stream().limit(1).reduce((a,b)->a).orElse("");
                getPromptProvider().setShellData(new CustomPromptProvider.ShellData(machine,AttributedStyle.GREEN));
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
    String makeGetRequest(String target) throws URISyntaxException {
        var auri=new URI(base()+target);
        var fileName=Arrays.stream(target.split("/")).filter(a->a.contains(".")).reduce((a,b)->a).orElse(null);
        if(fileName !=null){
            try {
                client.get()
                        .uri(auri)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .flatMap(bytes -> Mono.fromRunnable(() -> {
                            try {
                                java.nio.file.Files.write(Paths.get(fileName), bytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }))
                        .block();
                return String.format("{\"file_location\":\"%s\"}",fileName);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return String.format("{\"file_location\":\"not able to download\"}",fileName);

        }
        return Utils.tryUntil(3, () -> {
            try {
                var response = client.get()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .toEntity(String.class)
                        .block();
                return response.getBody();
            } catch (Exception ex) {
                resetToken();
                throw ex;
            }

        });
    }
    String makePostRequest(String target, String data) throws URISyntaxException {
        var auri=new URI(base()+target);
        return Utils.tryUntil(3, () -> {
            try {
                var response = client.post()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .bodyValue(data)
                        .retrieve()
                        .toEntity(String.class)
                        .block();
                return response.getBody();
            } catch (Exception ex) {
                resetToken();
                throw ex;
            }

        });
    }
    String makePatchRequest(String target, String data) throws URISyntaxException {
        var auri=new URI(base()+target);
        return Utils.tryUntil(3, () -> {
            try {
                var response = client.patch()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .bodyValue(data)
                        .retrieve()
                        .toEntity(String.class)
                        .block();
                return response.getBody();
            } catch (Exception ex) {
                resetToken();
                throw ex;
            }

        });
    }
    protected List<String> getCurrent(){
        AtomicInteger i = new AtomicInteger();
        return endPoints.peek().stream()
                .map(a->{
                    String r=i+") "+a;
                    i.getAndIncrement();
                    return r;
                }).collect(Collectors.toList());
    }
    protected void makeApiList() throws URISyntaxException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var resp=makeGetRequest("");
        endPoints.push(Utils.sorted(Utils.buildLinksAndTargets(mapper.readTree(resp))));
        endPoints.peek().add(0,new Utils.EndPoints("back","Get"));
    }
    private boolean checkMachineSelection(int index) throws IOException, URISyntaxException {
        if(endPoints.peek().size()>1 && endPoints.peek().get(1).url.startsWith("rain")){
            machine(endPoints.peek().get(index).url);
            endPoints.clear();
            apis();
            return true;
        }
        return false;
    }
    @ShellMethod(key = "machine")
    protected String machine(String m) throws IOException {

        token=null;
         machine=m;
         getToken();
        serialise();
         return machine;
    }

    private  void serialise() throws IOException {
        FileOutputStream out = new FileOutputStream(new File("history"));
        out.write(machine.getBytes(StandardCharsets.UTF_8));
        out.write(",".getBytes(StandardCharsets.UTF_8));
        out.write(userName != null ?userName.getBytes(StandardCharsets.UTF_8):"".getBytes(StandardCharsets.UTF_8));
        out.write(",".getBytes(StandardCharsets.UTF_8));
        out.write(passwd != null ?passwd.getBytes(StandardCharsets.UTF_8):"".getBytes(StandardCharsets.UTF_8));
        out.close();

    }


    @ShellMethod(key = "display")
    void displayCurrent(){
        if(endPoints.empty()){
            return;
        }
        System.out.println("\n*****************************************************\n");
        AtomicInteger i = new AtomicInteger();
        endPoints.peek().stream()
                .forEach(a->{
                    System.out.println(i+") "+a);
                    i.getAndIncrement();
                });
        System.out.println("\n*****************************************************\n");
    }


    @ShellMethod(key="apis")
    @ShellMethodAvailability("availabilityCheck")
    public void apis() throws URISyntaxException, JsonProcessingException {
        makeApiList();
        displayCurrent();
    }




    @ShellMethod(key="goto")
    @ShellMethodAvailability("availabilityCheck")
    public String goTo(Utils.EndPoints ep, String data, Boolean p) throws URISyntaxException, IOException {
        if(ep.url.equals("back")){
            endPoints.pop();
            return "";
        }
        var index =ep.url.indexOf("redfish/v1/");
        String url=ep.url;
        if(index != -1){
            url=url.substring("redfish/v1/".length()+1);
        }
        try {
            if(ep.action.equals("Post")){
                System.out.println(data);
                return makePostRequest(url,data);
            }
            if(p){
                System.out.println(data);
                return makePatchRequest(url,data);
            }
            return applicationContext.getBean(SerializeCommands.class).save(makeGetRequest(url));

        }catch (WebClientResponseException.BadRequest
                | WebClientResponseException.Forbidden
                | WebClientResponseException.MethodNotAllowed
                | WebClientResponseException.InternalServerError ex){
            return ex.getResponseBodyAsString();
        }catch (Exception exception){
            return exception.getMessage();
        }


    }


    @ShellMethod(key="s")
    @ShellMethodAvailability("availabilityCheck")
    public void select(int index,@ShellOption(defaultValue="") String d ,@ShellOption(defaultValue="") String f,@ShellOption(defaultValue="false") boolean p) throws URISyntaxException, IOException {
        if (checkMachineSelection(index)) return;
        ObjectMapper mapper=new ObjectMapper();
        if(!d.isEmpty()){
            d=d.substring(0,d.length()-1);///some strange , comes at end . need to remove it
        }
        if(!f.isEmpty()){
            var stream=new FileInputStream(new File(f));
            d = new String(stream.readAllBytes());
            stream.close();
        }
        var resp=goTo(endPoints.peek().get(index),d,p);
        if(!resp.isEmpty()){
            System.out.println(resp);
            endPoints.push(Utils.sorted(Utils.buildLinksAndTargets( mapper.readTree(resp))));
            endPoints.peek().add(0,new Utils.EndPoints("back","Get"));

        }
        displayCurrent();
    }




    @ShellMethod(key = "machines")
    void machines(){
         endPoints.push(Utils.listOfMachines());
        displayCurrent();

    }
    @ShellMethod(key = "username")
    void setUserName(String u) throws IOException {

        userName=u;
        serialise();
    }
    @ShellMethod(key = "password")
    void setPasswd(String p) throws IOException {
        passwd=p;
        serialise();
    }

    public Availability availabilityCheck() {
        return (machine != null && userName !=null && passwd !=null)
                ? Availability.available()
                : Availability.unavailable("machine/username/passwd is not set Eg: machine rain104bmc username \"rain username\" password \"rain passwd\"");
    }
}
