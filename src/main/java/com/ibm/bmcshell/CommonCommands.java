package com.ibm.bmcshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.inferencing.WatsonAssistant;
import com.ibm.bmcshell.Os.Cmd;
import com.ibm.bmcshell.ssh.SSHShellClient;
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
import org.springframework.shell.standard.commands.Script;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runShell;

public class CommonCommands implements ApplicationContextAware {
    WebClient client;

    String token;
    public String base(){
        return Utils.base(machine);
    }
    public static String machine="rain127bmc";
    @Autowired
    private ApplicationContext applicationContext;
    static String userName;
    static String passwd;
    static String libPath="./";
    PrintStream savedStream=System.out;
    @Autowired
    Script script;
    String lastCurlRequest;
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
            FileOutputStream stream = new FileOutputStream(new File(libPath+"clear"));
            stream.write("clear\n".getBytes(StandardCharsets.UTF_8));
            stream.close();
        }
    }
    @ShellMethod(key = "token")
    public String getToken() {

        if(token==null){
            String req=String.format("curl -k -X POST %s/redfish/v1/SessionService/Sessions -d '{\"UserName\":\"%s\", \"Password\":\"%s\"}'",Utils.base(machine),userName,passwd);
            System.out.println(req);
            try {
                var response = client.post()

                        .uri(new URI(Utils.base(machine)+"/redfish/v1/SessionService/Sessions"))
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
                        .header("Content-Type", "application/json")
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
                        .header("Content-Type", "application/json")
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
    String makeDeleteRequest(String target) throws URISyntaxException {
        var auri=new URI(base()+target);
        return Utils.tryUntil(3, () -> {
            try {
                var response = client.delete()
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
        var mapper = new ObjectMapper();
        var resp=makeGetRequest(Utils.normalise(""),"");
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
         Utils.addToMachineList(machine);
         return machine;
    }

    void redirector(OutputStream outputStream,Runnable runnable)
    {
        PrintStream customOut = new PrintStream(outputStream);
        PrintStream originalOut = System.out;
        System.setOut(customOut);
        runnable.run();
        System.setOut(originalOut);
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
    @ShellMethod(key="redirect",value = "eg: redirect filename. Will redirect all out puts to the file")

    public void redirect(String filePath) throws  FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        // Create a PrintStream that writes to the file
        PrintStream printStream = new PrintStream(fileOutputStream);
        // Redirect System.out to the file PrintStream
        System.setOut(printStream);
    }
    @ShellMethod(key="closeredirected",value = "Closes the active redirection and file will be saved")
    public void closeredirected() throws URISyntaxException, JsonProcessingException {

        System.out.close();
        System.setOut(savedStream);
    }
    @ShellMethod(key="overrideport")
    public void setOverridePort(int p)
    {
        Utils.targetport=p;
    }
    @ShellMethod(key="scheme")
    public void setScheme(String s)
    {
        Utils.scheme=s;
    }

    @ShellMethod(key="apis")
    @ShellMethodAvailability("availabilityCheck")
    public void apis() throws URISyntaxException, JsonProcessingException {
        makeApiList();
        displayCurrent();
    }
    @ShellMethod(key="sleep")
    public void sleep(int sec) throws InterruptedException {
        Thread.sleep(sec * 1000);
    }



    @ShellMethod(key="patch",value = "eg patch Systems/hypervisor/EthernetInterfaces/eth0 '{\"DHCPv4\": {\"DHCPEnabled\": false}}'")
    @ShellMethodAvailability("availabilityCheck")
    public void patch(String endPoint,String data) throws URISyntaxException, IOException {
        var ep=new Utils.EndPoints(endPoint,"Get");
        System.out.println(goTo(ep,data,true,""));
    }

    @ShellMethod(key="post",value = "eg post Managers/bmc/LogServices/Dump/Actions/LogService.CollectDiagnosticData  '{\"DiagnosticDataType\":\"Manager\"}'")
    @ShellMethodAvailability("availabilityCheck")
    public void post(String endPoint,String data) throws URISyntaxException, IOException {
        var ep=new Utils.EndPoints(endPoint,"Post");
        System.out.println(goTo(ep,data,false,""));
    }
    public void delete(String endPoint) throws URISyntaxException, IOException {
        var ep=new Utils.EndPoints(endPoint,"Delete");
        System.out.println(goTo(ep,"",false,""));
    }
    @ShellMethod(key="get",value = "eg get Systems/hypervisor/EthernetInterfaces/eth0 or get Systems/hypervisor/EthernetInterfaces/eth0 output-filename")
    @ShellMethodAvailability("availabilityCheck")
    public void get(String endPoint,@ShellOption(value = {"--output", "-o"},defaultValue="") String o,@ShellOption(value = {"--menu", "-m"},defaultValue="false") boolean menu) throws URISyntaxException, IOException {
        var ep=new Utils.EndPoints(endPoint,"Get");
        execute(ep,"",false,o,menu);
    }
    @ShellMethod(key="lastcurl",value = "eg lastcurl")
    @ShellMethodAvailability("availabilityCheck")
    public void lastcurl() throws URISyntaxException, IOException {
        System.out.println(lastCurlRequest);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // Create a StringSelection object from the text
        StringSelection selection = new StringSelection(lastCurlRequest);
        // Set the clipboard contents to the StringSelection
        clipboard.setContents(selection, null);
    }

    public String goTo(Utils.EndPoints ep, String data, Boolean p,String o) throws URISyntaxException, IOException {
        if(ep.url.equals("back")){
            endPoints.pop();
            return "";
        }
        lastCurlRequest=String.format("curl -k -H \"X-Auth-Token: %s\" -X GET https://%s%s",token,Utils.fullMachineName(machine),Utils.normalise(ep.url));
        String url=Utils.normalise(ep.url);
        try {
            if(ep.action.equals("Post")){
                System.out.println(data);
                return makePostRequest(url,data);
            }
            if(ep.action.equals("Delete")){
                return makeDeleteRequest(url);
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


    @ShellMethod(key="s",value = "eg s 1. Selects the menu item specified")
    @ShellMethodAvailability("availabilityCheck")
    public void select(int index,@ShellOption(value = {"--data", "-d"},defaultValue="") String d ,@ShellOption(value = {"--file", "-f"},defaultValue="") String f,@ShellOption(value = {"--patch", "-p"},defaultValue="false") boolean p,@ShellOption(value = {"--output", "-o"},defaultValue="") String o) throws URISyntaxException, IOException {
        if (checkMachineSelection(index)) return;
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

    @ShellMethod(key="select_all" ,value = "select all links in the current menu level")
    @ShellMethodAvailability("availabilityCheck")
    public void select_all() throws URISyntaxException, IOException {
        for(var e:endPoints.peek()){
            if(!e.action.equals("Get") ||e.url.equals("back")) {
                continue;
            }
            execute(e,"",false,"",false);
        }
    }
    @ShellMethod(key="select_recur" ,value = "select all links from the current menu level downwards recursively")
    @ShellMethodAvailability("availabilityCheck")
    public void select_recur() throws URISyntaxException, IOException {
        for(var e:endPoints.peek()){
            if(!e.action.equals("Get") ||e.url.equals("back")) {
                continue;
            }
            execute(e,"",false,"",true);
            select_recur();
        }
        execute(new Utils.EndPoints("back","Get"),"",false,"",true);
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
    @ShellMethod(key = "persistent_data")
    void persistentData()
    {
        scmd("cat /home/root/bmcweb_persistent_data.json");
    }
    @ShellMethod(key = "subscribe",value = "eg: subscribe ipaddress . Subscribes for events")
    void subscribe(String ipaddress,@ShellOption(value = {"--port", "-p"},defaultValue="8443")int port,@ShellOption(value = {"--target", "-t"},defaultValue="events") String target) throws IOException, URISyntaxException {
        post("EventService/Subscriptions", String.format("{\"Destination\":\"https://%s:%d/%s\",\"Protocol\":\"Redfish\",\"DeliveryRetryPolicy\": \"RetryForever\"}",ipaddress,port,target));

    }
    @ShellMethod(key = "delete_subscribe",value = "eg: subscribe ipaddress . Subscribes for events")
    void deleteSubscription(String id) throws IOException, URISyntaxException {
        delete(String.format("EventService/Subscriptions/%s",id));

    }
    @ShellMethod(key = "event_filters",value = "eg: event_filters hypervisor/EthernetInterfaces/eth0,hypervisor/EthernetInterfaces/eth1")
    void event_filters(String filter){
        Utils.setEventFilter(filter);
    }



    @ShellMethod(key = "restart",value = "eg: restart service-name . To restart the service")
    void restart(String service)  {
        scmd(String.format("systemctl restart %s",service));
    }

    @ShellMethod(key = "journalctl",value = "eg: journalctl arg ")
    void journalctl(@ShellOption(value = {"-u"},defaultValue="")String u,@ShellOption(value = {"-o"},defaultValue="")String o,@ShellOption(value = {""},defaultValue="-f")String f ,@ShellOption(value = {"-s"},defaultValue="")String s) throws IOException {
        var lambdaContext = new Object() {
            String cmd = "journalctl ";
        };

        if(!u.isEmpty()){
            lambdaContext.cmd = lambdaContext.cmd +String.format("--unit=%s ",u);
        }
        if(!o.isEmpty()){
            lambdaContext.cmd = lambdaContext.cmd +String.format("-o=%s ",o);
        }
        if(!f.isEmpty()){
            lambdaContext.cmd = lambdaContext.cmd +String.format("-f ");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if(!s.isEmpty()){
            try {
                redirector(outputStream,()->scmd(lambdaContext.cmd));
            }catch (Exception e){

            }


            new FileOutputStream(new File("journalctl")).write(outputStream.toByteArray());
            system("cat journalctl");
            return;
        }
        scmd(lambdaContext.cmd);
    }
    @ShellMethod(key = "journalhelp",value = "eg: journalhelp")
    void journalhelp(){
        scmd(String.format("journalctl -h"));
    }

    @ShellMethod(key = "display_session",value = "eg: display_session")
    void display_session(){

        System.out.println("User: "+userName);
        System.out.println("Password: "+passwd);
        System.out.println("Token: "+token);
    }





    @ShellMethod(key = "machines",value = "List all machine names that are available or previously logged in ")
    void machines() throws IOException {
         endPoints.push(Utils.listOfMachines());
        displayCurrent();

    }
    @ShellMethod(key = "username",value = "To supply bmc username")
    void setUserName(String u) throws IOException {

        userName=u;
        serialise();
    }
    @ShellMethod(key = "password")
    void setPasswd(String p) throws IOException {
        passwd=p;
        serialise();
    }
    @ShellMethod(key = "ssh" ,value = "eg: ssh . You can ssh in to the machine")
    @ShellMethodAvailability("availabilityCheck")
    void ssh() {
        runShell(Utils.fullMachineName(machine),userName,passwd);
        System.out.println("Exited Shell");
        displayCurrent();
    }
    @ShellMethod(key = "scmd",value = "eg: scmd 'ls /tmp/' .The specified command will be executed in machine with super user privilege")
    @ShellMethodAvailability("availabilityCheck")
    void scmd(String command) {
        var newCmd=userName.equals("root")?Optional.of(command):Arrays.stream(command.split(";")).map(a->"sudo -i "+a).reduce((a,b)->a+";"+b);
        newCmd.ifPresentOrElse(a->{
            runCommand(Utils.fullMachineName(machine),userName,passwd,a);
        },()->{
            System.out.println(command + " is invalid");
        });
    }

    @ShellMethod(key = "cmd" ,value = "eg cmd 'ls /tmp/' . Will execute the specified command in machine")
    @ShellMethodAvailability("availabilityCheck")
    void cmd(String command) {
        runCommand(Utils.fullMachineName(machine),userName,passwd,command);
    }
    @ShellMethod(key = "os" ,value = "Displays fw version details")
    @ShellMethodAvailability("availabilityCheck")
    void os() {
        runCommand(Utils.fullMachineName(machine),userName,passwd,"cat /etc/os-release");
        runCommand(Utils.fullMachineName(machine),userName,passwd,"cat /etc/timestamp");
    }
    @ShellMethod(key = "sshport" ,value = "eg sshport portnumber .Set default port for ssh")
    @ShellMethodAvailability("availabilityCheck")
    void sshport(int port) {
        SSHShellClient.port=port;
    }

    @ShellMethod(key = "system")
    void system(String command) {
        Cmd.execute(command,passwd);
    }

    @ShellMethod(key = "apikey")
    void key(String key) throws IOException {
        WatsonAssistant.apiKey=key;
        serialise();
    }
    void invokeScript(String filename)
    {


    }
    @ShellMethod(key="do-while")
    @ShellMethodAvailability("availabilityCheck")
    void do_while(String scrFile,String condition)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        redirector(outputStream,()-> {
            try {
                script.script(new File(scrFile));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(outputStream.toString());

    }
    @ShellMethod(key="verify")
    @ShellMethodAvailability("availabilityCheck")
    void verify(String expression) throws JsonProcessingException {
        var tokens=expression.split("==");
        String key=tokens[0].trim();
        String expected=tokens[1].trim();
        ObjectMapper mapper = new ObjectMapper();
        var root=mapper.readTree(applicationContext.getBean(SerializeCommands.class).lastResult());
        var node=root.at(key);
        String SUCCESS="Passed: "+ expression;
        String FAILED="Failed: "+ expression;
        if(root!=null){
            switch (node.getNodeType()){
                case STRING:
                    System.out.println(expected.equals(node.asText())?SUCCESS:FAILED);
                    break;
                case BOOLEAN:
                    System.out.println(Boolean.parseBoolean(expected) == node.asBoolean()?SUCCESS:FAILED);
                    break;
                case NUMBER:
                    System.out.println(Double.parseDouble(expected) == node.asDouble()?SUCCESS:FAILED);
                    break;
                default:
                    System.out.println(FAILED);

            }
            return;
        }
        System.out.println(FAILED);

    }
    @ShellMethod(key="repeat",value = "eg: repeat filename count. This will rung the script specifed(count) number of times")
    @ShellMethodAvailability("availabilityCheck")
    void repeat(String scrFile,int count) throws Exception {
        while(count>0){
            script.script(new File(scrFile));
            count--;
        }
    }

    @ShellMethod(key="r",value = "eg: r filename. This command will run the file content as script")
    @ShellMethodAvailability("availabilityCheck")
    void runScript(String scrFile) throws Exception {
        script.script(new File(libPath+scrFile));

    }
    void tell(String message) throws IOException, InterruptedException {
        message=message.replace(".","\n");
        var words=message.split(" ");
        int currentcount=0;
        for(var w:words){
            System.out.print(w + " ");
            Thread.sleep(100);
        }
        System.out.println("\n");
    }
    @ShellMethod(key = "q")
    protected void query(String m,@ShellOption(value = {"-c"},defaultValue="openbmcwiki") String c) throws IOException, InterruptedException {
        var res= WatsonAssistant.ask(m,c);
        tell(res);
    }
    @ShellMethod(key = "whoami")
    protected void whoami() throws IOException, InterruptedException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/whoami.txt");

        tell(new String(resourceAsStream.readAllBytes(),StandardCharsets.UTF_8));
    }
    @ShellMethod(key = "efficiency?")
    protected void efficiency() throws IOException, InterruptedException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/efficiency.txt");

        tell(new String(resourceAsStream.readAllBytes(),StandardCharsets.UTF_8));
    }


    @ShellMethod(key = "save",value = "eg save filename 3 .This will save last 3 command executed in to the file as runnable script")
    protected String save(String scriptname, int count) throws IOException, InterruptedException {
        FileInputStream reader = new FileInputStream(new File("spring-shell.log"));
        var history=Arrays.stream(new String(reader.readAllBytes()).split("\n")).collect(Collectors.toList());
        history.remove(history.size()-1);
        var toSkip = Math.max(0,history.size()-count);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(libPath+scriptname));
        history.stream().skip(toSkip).forEach(element->{
            System.out.println(element);
            var index =element.indexOf(':');
            var cmd = element.substring(index+1);
            try {
                fileOutputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return "Saved to file "+libPath+scriptname;

    }
    @ShellMethod(key = "list")
    protected void list() throws IOException, InterruptedException {
        system("ls "+libPath);
    }
    @ShellMethod(key = "libpath")
    protected void libpath(String path) throws IOException, InterruptedException {
        libPath=path.endsWith("/")?path:path+"/";
    }


    public Availability availabilityCheck() {
        int maxBufferSize = 1024 * 1024 * 1024; // 10 MB
        System.setProperty("spring.codec.max-in-memory-size", String.valueOf(maxBufferSize));

        return (machine != null && userName !=null && passwd !=null)
                ? Availability.available()
                : Availability.unavailable("machine/username/password is not set Eg: machine rain104bmc username \"rain username\" password \"rain passwd\"");
    }
}
