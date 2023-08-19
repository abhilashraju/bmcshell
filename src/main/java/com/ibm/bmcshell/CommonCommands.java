package com.ibm.bmcshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.inferencing.WatsonAssistant;
import com.ibm.bmcshell.Os.Cmd;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.ibm.bmcshell.CustomPromptProvider;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runShell;
import com.ibm.bmcshell.Os.Cmd;

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
    PrintStream savedStream=System.out;
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

    protected static Stack<List<Utils.EndPoints>> endPoints=new Stack<>();

    protected CommonCommands() throws IOException {
        if(client ==null) {
            client = Utils.createWebClient();
            File file= new File("history");
            if(file.exists()){
                BufferedInputStream bufferedInputStream= new BufferedInputStream(new FileInputStream(file));
                var data=new String(bufferedInputStream.readAllBytes()).split(",");
                machine=data[0];
                if(data.length>1)userName=data[1];
                if(data.length>2)passwd=data[2];
                if(data.length>3)WatsonAssistant.apiKey=data[3];


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
    private static Mono<Path> saveToFile(Flux<byte[]> content) {
        Path tempFilePath;
        try {
            tempFilePath = Files.createTempFile("temp", null);
        } catch (IOException e) {
            return Mono.error(e);
        }

        return content
                .flatMap(bytes -> Mono.fromCallable(() -> saveBytesToFile(bytes, tempFilePath)))
                .then(Mono.just(tempFilePath));
    }
    private static Void saveBytesToFile(byte[] bytes, Path filePath) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            channel.write(ByteBuffer.wrap(bytes));
        }
        return null;
    }

    private static Path concatFiles(Path file1, Path file2) {
        try (FileChannel channel1 = FileChannel.open(file1, StandardOpenOption.READ);
             FileChannel channel2 = FileChannel.open(file2, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

            channel2.position(channel2.size());
            channel2.transferFrom(channel1, channel2.size(), channel1.size());
            return file2;
        } catch (IOException e) {
            throw new RuntimeException("Error concatenating files", e);
        }
    }
    String makeGetRequest(String target,String o) throws URISyntaxException {
        var auri=new URI(base()+target);
        if(!o.isEmpty()){
            try {
                client.get()
                        .uri(auri)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .flatMap(bytes -> Mono.fromRunnable(() -> {
                            try {
                                java.nio.file.Files.write(Paths.get(o), bytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }))
                        .block();
                return String.format("{\"file_location\":\"%s\"}",o);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return String.format("{\"file_location\":\"not able to download\"}",target);

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
        var resp=makeGetRequest("","");
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
        out.write(",".getBytes(StandardCharsets.UTF_8));
        out.write(WatsonAssistant.apiKey != null ?WatsonAssistant.apiKey.getBytes(StandardCharsets.UTF_8):"".getBytes(StandardCharsets.UTF_8));
        out.close();

    }


    @ShellMethod(key = "display")
    void displayCurrent(){
        if(endPoints.empty()){
            System.out.println("Empty");
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
    @ShellMethod(key="redirect")

    public void redirect(String filePath) throws  FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        // Create a PrintStream that writes to the file
        PrintStream printStream = new PrintStream(fileOutputStream);
        // Redirect System.out to the file PrintStream
        System.setOut(printStream);
    }
    @ShellMethod(key="closeredirected")
    public void closeredirected() throws URISyntaxException, JsonProcessingException {

        System.out.close();
        System.setOut(savedStream);
    }

    @ShellMethod(key="apis")
    @ShellMethodAvailability("availabilityCheck")
    public void apis() throws URISyntaxException, JsonProcessingException {
        makeApiList();
        displayCurrent();
    }
    @ShellMethod(key="sleep")
    public void apis(int sec) throws InterruptedException {
        Thread.sleep(sec * 1000);
    }




    @ShellMethod(key="goto")
    @ShellMethodAvailability("availabilityCheck")
    public void run(String endPoint) throws URISyntaxException, IOException {
        var ep=new Utils.EndPoints(endPoint,"Get");
        System.out.println(goTo(ep,"",false,""));
    }
        public String goTo(Utils.EndPoints ep, String data, Boolean p,String o) throws URISyntaxException, IOException {
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
            return applicationContext.getBean(SerializeCommands.class).save(makeGetRequest(url,o));

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
    public void select(int index,@ShellOption(value = {"--data", "-d"},defaultValue="") String d ,@ShellOption(value = {"--file", "-f"},defaultValue="") String f,@ShellOption(value = {"--patch", "-p"},defaultValue="false") boolean p,@ShellOption(value = {"--output", "-o"},defaultValue="") String o) throws URISyntaxException, IOException {
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
        execute(endPoints.peek().get(index),d,p,o);
    }
    void execute(Utils.EndPoints endp,String d, boolean p, String o,boolean showMenu) throws URISyntaxException, IOException {
        var resp=goTo(endp,d,p,o);
        if(resp!=null && !resp.isEmpty()){
            System.out.println(resp);
            if(showMenu) {
                if(endp.action.equals("Get")){
                    endPoints.push(Utils.sorted(Utils.buildLinksAndTargets( new ObjectMapper().readTree(resp))));
                    endPoints.peek().add(0,new Utils.EndPoints("back","Get"));
                }
            }
        }
        if(showMenu){
            displayCurrent();
        }

    }
    void execute(Utils.EndPoints endp,String d, boolean p, String o) throws URISyntaxException, IOException {
        execute(endp,d,p,o,true);
    }
    public void select(int index) throws URISyntaxException, IOException {
        select(index,"","",false,"");
    }
        @ShellMethod(key="seq")
    @ShellMethodAvailability("availabilityCheck")
    public void sequence(String seq){
        Arrays.stream(seq.split(",")).forEach(a->{
            try {
                select(Integer.parseInt(a));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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
    @ShellMethod(key = "ssh")
    @ShellMethodAvailability("availabilityCheck")
    void ssh() {
        runShell(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd);
        System.out.println("Exited Shell");
        displayCurrent();
    }

    @ShellMethod(key = "cmd")
    @ShellMethodAvailability("availabilityCheck")
    void cmd(String command) {
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,command);
    }
    @ShellMethod(key = "os")
    @ShellMethodAvailability("availabilityCheck")
    void os() {
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,"cat /etc/os-release");
    }
    @ShellMethod(key = "system")
    void system(String command) {
        Cmd.execute(command,passwd);
    }




    @ShellMethod(key = "scp")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String path) {
        StringBuilder cmdBuilder=new StringBuilder();

        cmdBuilder.append("scp ");
        cmdBuilder.append("-o StrictHostKeyChecking=no ");
        cmdBuilder.append("-o UserKnownHostsFile=/dev/null ");
        cmdBuilder.append("-P 22 ");
        cmdBuilder.append(path);
        cmdBuilder.append(" ");
        cmdBuilder.append(userName);
        cmdBuilder.append("@");
        cmdBuilder.append(String.format("%s.aus.stglabs.ibm.com",machine));
        cmdBuilder.append(":/tmp/");

        system(cmdBuilder.toString());
    }
    @ShellMethod(key = "apikey")
    void key(String key) throws IOException {
        WatsonAssistant.apiKey=key;
        serialise();
    }
    @ShellMethod(key = "save")
    void savePrompt() throws IOException {
        WatsonAssistant.saveLastPrompt();

    }
    @ShellMethod(key = "refresh")
    void refresh() {
        WatsonAssistant.refresh();
    }
    @ShellMethod(key = "q")
    protected void query(String m) throws IOException, InterruptedException {
        var res= WatsonAssistant.makeQuery(m);
        res=res.replace(".","\n");
        var words=res.split(" ");
        int currentcount=0;
        for(var w:words){
            System.out.print(w + " ");
            Thread.sleep(100);
        }
        System.out.println("\n");
    }
    @ShellMethod(key = "aq")
    protected void aquery(String m) throws IOException, InterruptedException {
       query(WatsonAssistant.getLastQuery()+m);
    }
    @ShellMethod(key = "save_as")
    protected String save(String scriptname, int count) throws IOException, InterruptedException {
        FileInputStream reader = new FileInputStream(new File("spring-shell.log"));
        var history=Arrays.stream(new String(reader.readAllBytes()).split("\n")).collect(Collectors.toList());
        history.remove(history.size()-1);
        var toSkip = Math.max(0,history.size()-count);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(scriptname));
        history.stream().skip(toSkip).forEach(element->{
            System.out.println(element);
            var cmd =element.split(":")[1];
            try {
                fileOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return "Saved to file "+scriptname;

    }


    public Availability availabilityCheck() {
        int maxBufferSize = 1024 * 1024 * 1024; // 10 MB
        System.setProperty("spring.codec.max-in-memory-size", String.valueOf(maxBufferSize));

        return (machine != null && userName !=null && passwd !=null && WatsonAssistant.apiKey!=null)
                ? Availability.available()
                : Availability.unavailable("machine/username/passwd/apikey is not set Eg: machine rain104bmc username \"rain username\" password \"rain passwd\"");
    }
}
