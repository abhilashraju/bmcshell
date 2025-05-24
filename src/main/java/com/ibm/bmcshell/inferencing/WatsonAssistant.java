package com.ibm.bmcshell.inferencing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class WatsonAssistant {

    public  static String apiKey =
            "";
    public static String ask(String question,String collection) {
        int n=5;
        double temperature=0.3;
        String url =
                "https://kvuchkhvar.us-east-a.ibm.stepzen.net/wxflows-genai/openbmcwiki/graphql";

        // Construct the GraphQL query
        String query =String.format("{\n" +
                        "        \"query\": \"query RAG {\\n  myRag (\\n    n: %d\\n    collection: \\\"%s\\\"\\n    question: \\\"%s\\\"\\n    aiEngine: WATSONX\\n    model: \\\"ibm/granite-13b-chat-v2\\\"\\n  parameters: {max_new_tokens: 8191, temperature: %f, stop_sequences: [\\\"\\\\n\\\\n\\\\n\\\\n\\\"]}\\n    embedModel: \\\"ibm/slate-30m-english-rtrvr\\\"\\n    searchEngine: MILVUS\\n  ) {\\n    out\\n  }\\n}\"\n" +
                        "    }",n,
                collection,
                question,temperature);
//        System.out.println(query);
        // Set up the HTTP client
        String responseBody=null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);

            // Set headers
            request.setHeader(HttpHeaders.AUTHORIZATION, "Apikey " + apiKey);
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // Set the request body
            request.setEntity(new StringEntity(query, StandardCharsets.UTF_8));

            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Check the response status code
                if (response.getStatusLine().getStatusCode() == 200) {
                    // Parse and print the response body
                    responseBody= EntityUtils.toString(response.getEntity(),
                            StandardCharsets.UTF_8);
//                    System.out.println(responseBody);
                    ObjectMapper mapper = new ObjectMapper();
                    var tree=mapper.readTree(responseBody);
                    return tree.get("data").get("myRag").get("out").get("modelResponse").get("results").get(0).get("generated_text").asText();

                } else {
                    // Handle non-200 responses
                    System.err.println("HTTP error: " +
                            response.getStatusLine().getStatusCode());
                    System.err.println(EntityUtils.toString(response.getEntity(),
                            StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(responseBody!=null){
                System.out.println(responseBody);
            }
        }

        return "Error";
    }

    // Replace YOUR_API_KEY with your actual API key

}