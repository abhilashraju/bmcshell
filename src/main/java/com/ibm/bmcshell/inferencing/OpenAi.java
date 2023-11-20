package com.ibm.bmcshell.inferencing;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class OpenAi {
    public static void main(String[] args) throws Exception {

        String endpoint = "https://api.openai.com/v1/engines/gpt-3.5-turbo/completions";
        String prompt = "Translate the following English text to French: 'Hello, world.'";

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(endpoint);

        // Set request headers
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + "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");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader("OpenAI-Organization", "org-iESAp1gTluWzkMkiaiOG8DYy");

        // Set request body
        String requestBody = "{\"prompt\":\"" + prompt + "\",\"max_tokens\":50}";
        httpPost.setEntity(new StringEntity(requestBody));

        // Send the request and receive the response
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            System.out.println("API Response: " + responseString);
        }
    }
}

