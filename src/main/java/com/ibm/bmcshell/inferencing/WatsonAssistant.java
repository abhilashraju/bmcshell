package com.ibm.bmcshell.inferencing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.bmcshell.Utils;
import com.ibm.cloud.sdk.core.security.IamToken;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;
import java.io.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.stream.StreamSupport;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;

public class WatsonAssistant {
    static WebClient client;
    static  String token;
    public static String apiKey;
    public static String help;

    static {
        try {
            Resource resource = new ClassPathResource("help.txt");
            help = new String(resource.getInputStream().readAllBytes());
//            help = new String(new FileInputStream(new File("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/help.txt")).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static class LastPrompt
    {
        String question;
        String answer;

    }
    static LastPrompt lastPrompt=new LastPrompt();
    static String getToken(){
        IamAuthenticator authenticator = new IamAuthenticator.Builder()
                .apikey(apiKey)
                .build();
        token= authenticator.requestToken().getAccessToken();
        return token;
    }
    static {
        try {
            client = Utils.createWebClient();

        } catch (SSLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void saveLastPrompt() throws IOException {
        FileWriter fileWriter = new FileWriter("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/help.txt", true); // The 'true' parameter indicates append mode
        fileWriter.write("\n"+lastPrompt.question+"\n"+lastPrompt.answer);
        fileWriter.close();
    }
    public static LastPrompt getLastPrompt() throws IOException {
        return lastPrompt;
    }
    public static String getLastQuery() throws IOException {
        return lastPrompt.question;
    }
    public static void refresh()
    {
        try {
            help = new String(new FileInputStream(new File("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/help.txt")).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String makeQuery(String input) throws IOException {
     ObjectMapper mapper= new ObjectMapper();

     var data =mapper.readTree(new FileInputStream(new File("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/model.json")));
     StringBuilder builder= new StringBuilder();
     builder.append(help)
                .append("Input:")
                .append(input).append("?");
     ((ObjectNode) data).put("input",builder.toString());
     lastPrompt.question=input+"?";
     lastPrompt.answer="";

     return Utils.tryUntil(2,()->{
            try {
                String bearer="Bearer "+token;
                String auri="https://us-south.ml.cloud.ibm.com/ml/v1-beta/generation/text?version=2023-05-28";
                var response = client.post()
                        .uri(auri)
                        .header("Authorization", bearer)
                        .header("Content-Type", "application/json")
                        .bodyValue(data)
                        .retrieve()
                        .toEntity(String.class)
                        .block();
                var resp= response.getBody();
//            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resp));

                var res=mapper.readTree(resp).get("results");
                if(res instanceof ArrayNode){
                    var arry=(ArrayNode)res ;
                    lastPrompt.answer= StreamSupport.stream(arry.spliterator(),false).map(a->a.get("generated_text").asText()).reduce((a,b)->
                    {
                        return a+ "\n"+b;

                    }).orElse("Dont Know");
                    StringBuilder builder2= new StringBuilder();
                    builder2.append(help+"\n");
                    builder2.append(lastPrompt.question);
                    builder2.append(lastPrompt.answer);
                    help=builder2.toString();
                    return lastPrompt.answer;

                }
                return "Don't Know";
            } catch (JsonProcessingException jx){
                return "Don't Know";
            }catch (Exception ex) {
                getToken();
                throw ex;
            }

     });

 }
 public static void main(String[] args) throws IOException {

     System.out.println(makeQuery("how do i give username and password to shell"));


 }
}
