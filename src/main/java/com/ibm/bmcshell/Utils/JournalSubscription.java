package com.ibm.bmcshell.Utils;

import java.net.URI;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;

/**
 * Holds one SSE subscription to the journalserver running on the current BMC
 * (GET https://&lt;machine&gt;:&lt;port&gt;/subscribe). Journal lines received on
 * that connection are handed to a sink supplied by the caller.
 *
 * The subscription follows the selected machine: once started, a machine
 * change drops the old connection and connects to the new machine.
 */
@Component
public class JournalSubscription {

    @Value("${journal.server.port:8080}")
    private int port;

    private Disposable subscription;
    private Consumer<String> sink;
    private String machine;
    private boolean active;

    /**
     * Register where received lines go. From now on every machine change
     * connects to the new machine, until {@link #stop()} is called.
     */
    public synchronized void setSink(Consumer<String> sink) {
        this.sink = sink;
        this.active = true;
    }

    /** Subscribe to the journalserver on {@code machine}, replacing any existing subscription. */
    public synchronized void start(String machine, Consumer<String> sink) {
        stop();
        this.active = true;
        this.sink = sink;
        connect(machine);
    }

    /** Drop the subscription and stop following machine changes. */
    public synchronized void stop() {
        active = false;
        disconnect();
    }

    /** Called when the selected machine changes. Drops the old connection and connects to the new machine. */
    public synchronized void onMachineChanged(String newMachine) {
        if (!active || sink == null) {
            return;
        }
        disconnect();
        connect(newMachine);
    }

    /** True when the user asked for a subscription, even if the connection has since dropped. */
    public synchronized boolean isActive() {
        return active;
    }

    /** True while the SSE connection is open. */
    public synchronized boolean isConnected() {
        return subscription != null && !subscription.isDisposed();
    }

    public synchronized String getMachine() {
        return machine;
    }

    public int getPort() {
        return port;
    }

    private void disconnect() {
        if (subscription != null) {
            subscription.dispose();
            subscription = null;
        }
    }

    private void connect(String newMachine) {
        this.machine = newMachine;
        URI uri = URI.create(String.format("https://%s:%d/subscribe", Util.fullMachineName(newMachine), port));
        final Consumer<String> target = sink;

        WebClient sseClient;
        try {
            SslContext sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            // No read timeout: the journal can be silent for long periods.
            HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslCtx));
            sseClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (Exception e) {
            System.err.println("Journal subscribe failed to build client: " + e.getMessage());
            return;
        }

        System.out.println("Journal subscribing to " + uri);
        subscription = sseClient.get()
                .uri(uri)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .doOnNext(event -> {
                    String data = event.data();
                    if (data != null && !data.isBlank()) {
                        target.accept(data);
                    }
                })
                .doOnError(ex -> System.err
                        .println("Journal subscription to " + uri + " failed: " + ex.getMessage()))
                .doOnComplete(() -> System.out.println("Journal subscription to " + uri + " closed by server."))
                .subscribe(e -> {
                }, ex -> {
                });
    }
}
