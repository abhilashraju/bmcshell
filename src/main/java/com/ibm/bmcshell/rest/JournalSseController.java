package com.ibm.bmcshell.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
public class JournalSseController {

    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    /**
     * SSE endpoint for journal log streaming
     * Clients connect to this endpoint to receive real-time journal updates
     */
    @GetMapping(path = "/sse/journal", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJournal() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        emitters.add(emitter);
        System.out.println("New SSE client connected. Total clients: " + emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            System.out.println("SSE client disconnected. Total clients: " + emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            System.out.println("SSE client timeout. Total clients: " + emitters.size());
        });

        emitter.onError((ex) -> {
            emitters.remove(emitter);
            System.out.println("SSE client error: " + ex.getMessage());
        });

        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                    .data("Connected to journal stream"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Broadcast a journal line to all connected SSE clients
     *
     * @param line The journal line to broadcast
     */
    public void broadcastJournalLine(String line) {
        Set<SseEmitter> deadEmitters = new CopyOnWriteArraySet<>();

        emitters.forEach(emitter -> {
            try {
                // Send data-only event (no event name)
                emitter.send(SseEmitter.event()
                        .data(line));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Remove dead emitters
        emitters.removeAll(deadEmitters);
    }

    /**
     * Get the number of active SSE connections
     * 
     * @return The number of active emitters
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * Check if there are any active SSE connections
     * 
     * @return true if there are active connections, false otherwise
     */
    public boolean hasActiveConnections() {
        return !emitters.isEmpty();
    }
}
