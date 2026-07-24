package com.ibm.bmcshell.rest;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class JournalSseController {

    // Many-unicast: backpressure-buffered, multiple subscribers each see all events
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(1024, false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /** Active filters — default "*" passes everything through. */
    private volatile String[] filters = { "*" };

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
     * Set the filters for SSE event broadcasting.
     * Only lines that contain at least one filter string (case-insensitive) are forwarded.
     * Use {@code ["*"]} to pass everything through (default).
     *
     * @param filters one or more substrings to match; use {@code ["*"]} to pass everything
     */
    public void setFilters(String[] filters) {
        this.filters = filters.clone();
        System.out.println("SSE filter set to: " + Arrays.toString(this.filters));
    }

    /** Return a copy of the currently active filter strings. */
    public String[] getFilters() {
        return filters.clone();
    }

    /**
     * Broadcast a journal line to all connected SSE clients.
     * The line is only emitted when it matches at least one of the active filters.
     *
     * @param line The journal line to broadcast
     */
    public void broadcastJournalLine(String line) {
        if (matchesAnyFilter(line)) {
            sink.tryEmitNext(line);
        }
    }

    /** Returns true when {@code line} contains at least one filter string (case-insensitive). */
    private boolean matchesAnyFilter(String line) {
        String lower = line.toLowerCase();
        for (String f : filters) {
            if ("*".equals(f) || lower.contains(f.toLowerCase())) {
                return true;
            }
        }
        return false;
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
