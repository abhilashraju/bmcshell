package com.ibm.bmcshell.inferencing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.bmcshell.Utils;
import com.ibm.cloud.sdk.core.security.IamToken;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;

public class WatsonAssistant {
    static WebClient client;
    static  String token;
    public static String apiKey;
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
        }
    }

    public static String makeQuery(String input) throws IOException {
     ObjectMapper mapper= new ObjectMapper();

     var data =mapper.readTree(new FileInputStream(new File("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/model.json")));
     var help =new String(new FileInputStream(new File("/Users/abhilashraju/work/JAVA/bmcshellnew/src/main/resources/help.txt")).readAllBytes());
     StringBuilder builder= new StringBuilder();
     builder.append(help)
                .append("Input:")
                .append(input).append("?");
     ((ObjectNode) data).put("input",builder.toString());

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
                    return arry.get(0).get("generated_text").asText();
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
