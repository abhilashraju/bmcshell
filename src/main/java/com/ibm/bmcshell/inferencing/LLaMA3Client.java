package com.ibm.bmcshell.inferencing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LLaMA3Client {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String DEFAULT_MODEL = "codellama";

    public static String complete(String question, String model) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(OLLAMA_URL);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            String json = mapper.createObjectNode()
                    .put("model", model)
                    .put("prompt", question)
                    .toString();

            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                StringBuffer sb = new StringBuffer();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        var res = new ObjectMapper().readTree(line);
                        try {
                            System.out.print(res.get("response").asText());
                        } catch (Exception e) {
                            System.out.println(res);
                        }
                        // Optionally, process each line here as it arrives
                    }
                }
                return "DONE";
            }
        }
    }

    public static String ask(String question) throws IOException {
        return complete(question, DEFAULT_MODEL);
    }

    public static String suggest(String text) throws IOException {
        // Use the AI model to get completion suggestions for the user-entered text
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(OLLAMA_URL);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // You can adjust the prompt to instruct the model to return suggestions as a
            // list
            String prompt = "Suggest 1 possible completions for: \"" + text + "\" as text don't repeat the question";
            String json = mapper.createObjectNode()
                    .put("model", DEFAULT_MODEL)
                    .put("prompt", prompt)
                    .put("max_tokens", 100)
                    .put("stream", false) // Disable streaming
                    .toString();

            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JsonNode root = mapper.readTree(sb.toString());
                String responseText = root.get("response").asText();
                return responseText;
            } catch (IOException e) {

            }

        }
        return "";

    }

    public static void setModel(String m) {
        DEFAULT_MODEL = m;
    }

    java.util.ArrayList<String> getModels() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("http://localhost:11434/api/tags");
            get.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = client.execute(get)) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JsonNode root = mapper.readTree(sb.toString());
                JsonNode models = root.get("models");
                var modelList = new java.util.ArrayList<String>();
                for (JsonNode modelNode : models) {
                    String modelName = modelNode.get("model").asText();
                    modelList.add(modelName);
                }
                return modelList;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void listModels() {
        System.out.println("Available models:");
        for (String model : new LLaMA3Client().getModels()) {

            if (model.contains(DEFAULT_MODEL)) {
                System.out.print("* ");
            } else {
                System.out.print("  ");
            }
            System.out.println(model);
        }
    }

    public static void pullModel(String modelName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:11434/api/pull");
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            String json = mapper.createObjectNode()
                    .put("model", modelName)
                    .toString();

            post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("Model pulled successfully: " + modelName);
                } else {
                    System.out.println("Failed to pull model: " + modelName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
