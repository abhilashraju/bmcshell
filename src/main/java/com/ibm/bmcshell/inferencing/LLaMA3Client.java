package com.ibm.bmcshell.inferencing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketConfigurationInfo;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.client.TextMessageHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.ibm.bmcshell.CommonCommands;



public class LLaMA3Client {
    @Configuration
@EnableWebSocket
public static class LLaMA3WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(llama3WebSocketHandler(), "/llama");
    }

    @Bean
    public LLaMA3WebSocketHandler llama3WebSocketHandler() {
        return new LLaMA3WebSocketHandler();
    }
}



public static class LLaMA3WebSocketHandler implements TextMessageHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Send a response back to the client
        String response = processLLaMA3Request(message.getPayload());
        TextMessage responseMessage = new TextMessage(response);
        session.sendMessage(responseMessage);
    }
}
static WebSocketSession session=new WebSocketSession("ws://localhost:11434/llama");
    public static String getAnswer(String query){
        TextMessage message = new TextMessage(query);
        session.sendMessage(message);

        // Receive and process the response from the model
        String response = session.recv(10000); // 10 seconds timeout

        return response;
    }
    public static void main(String[] args) throws Exception {
        // Establish a WebSocket connection to the Spring Boot app
      

        // Send a message to the model (e.g., "Hello, how are you?")
        TextMessage message = new TextMessage("Hello, how are you?");
        session.sendMessage(message);

        // Receive and process the response from the model
        String response = session.recv(10000); // 10 seconds timeout

        System.out.println(response);
    }
}

