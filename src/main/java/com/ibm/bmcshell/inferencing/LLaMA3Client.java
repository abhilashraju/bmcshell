package com.ibm.bmcshell.inferencing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LLaMA3Client {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String ask(String question) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(OLLAMA_URL);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            String json = mapper.createObjectNode()
                .put("model", "llama3")
                .put("prompt", question)
                .toString();

            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                StringBuffer sb = new StringBuffer();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        var res=new ObjectMapper().readTree(line);
                        
                        System.out.print(res.get("response").asText());   
                        // Optionally, process each line here as it arrives
                    }
                }
                return "DONE";
            }
        }
    }

    public static void main(String[] args) throws IOException {
        LLaMA3Client client = new LLaMA3Client();
        String response = client.ask("What is vpd?");
        System.out.println(response);
    }
}