// LLaMA3Client.java
package com.ibm.bmcshell.inferencing;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class LLaMA3Client{
    public static WebClient webClient = WebClient.builder()
            .baseUrl("ws://localhost:11434/llama")
            .build();

    public static String getAnswer(String query) {
        Mono<String> responseMono = webClient.textMessage(query).retrieve().bodyToMono(String.class);
        return responseMono.block();
    }

    public static void main(String[] args) throws Exception {
        // Send a message to the model (e.g., "Hello, how are you?")
        String response = getAnswer("Hello, how are you?");
        System.out.println(response);
    }
}
