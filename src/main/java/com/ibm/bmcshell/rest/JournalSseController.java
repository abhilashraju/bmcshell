package com.ibm.bmcshell.rest;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class JournalSseController {

    // Many-unicast: backpressure-buffered, multiple subscribers each see all events
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(1024, false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /**
     * SSE endpoint for journal log streaming.
     * Clients connect here to receive real-time journal updates.
     */
    @GetMapping(path = "/sse/journal", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamJournal() {
        activeConnections.incrementAndGet();
        System.out.println("New SSE client connected. Total clients: " + activeConnections.get());

        return sink.asFlux()
                .map(line -> ServerSentEvent.<String>builder()
                        .data(line)
                        .build())
                .startWith(ServerSentEvent.<String>builder()
                        .data("Connected to journal stream")
                        .build())
                .doOnCancel(() -> {
                    int remaining = activeConnections.decrementAndGet();
                    System.out.println("SSE client disconnected. Total clients: " + remaining);
                })
                .doOnError(ex -> {
                    int remaining = activeConnections.decrementAndGet();
                    System.out.println("SSE client error: " + ex.getMessage() + ". Total clients: " + remaining);
                })
                .doOnComplete(() -> {
                    int remaining = activeConnections.decrementAndGet();
                    System.out.println("SSE client stream completed. Total clients: " + remaining);
                });
    }

    /**
     * Broadcast a journal line to all connected SSE clients.
     *
     * @param line The journal line to broadcast
     */
    public void broadcastJournalLine(String line) {
        sink.tryEmitNext(line);
    }

    /**
     * Get the number of active SSE connections.
     *
     * @return The number of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    /**
     * Check if there are any active SSE connections.
     *
     * @return true if there are active connections, false otherwise
     */
    public boolean hasActiveConnections() {
        return activeConnections.get() > 0;
    }
}
