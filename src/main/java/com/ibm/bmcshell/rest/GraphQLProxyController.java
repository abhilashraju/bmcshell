package com.ibm.bmcshell.rest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.SSLException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.Utils.Util;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * GraphQL proxy for the mobile (iOS) application.
 *
 * The phone talks to bmcshell exactly as it would talk to bmcweb:
 * <ul>
 * <li>{@code GET|POST /graphql} - one-shot queries</li>
 * <li>{@code GET /graphql/subscribe?query=...} - Server-Sent-Events
 * subscriptions</li>
 * </ul>
 * Requests are forwarded to the BMC bmcweb that bmcshell is currently connected
 * to ({@link CommonCommands#machine}). The phone's authentication headers are
 * passed through unchanged; the query string (including {@code remoteIp}) is
 * forwarded verbatim.
 */
@RestController
public class GraphQLProxyController {

    private static final String GRAPHQL_PATH = "/graphql";
    private static final String SUBSCRIBE_PATH = "/graphql/subscribe";

    /** Only these request headers are forwarded to the BMC. */
    private static final List<String> FORWARDED_HEADERS = List.of("X-Auth-Token", "Authorization");

    private static final String UPSTREAM_ERROR_BODY = "{\"errors\":[{\"message\":\"Upstream BMC unavailable\"}]}";
    private static final String PERMISSION_ERROR_BODY = "{\"errors\":[{\"message\":\"Permission denied\"}]}";
    private static final String GENERIC_ERROR_BODY = "{\"errors\":[{\"message\":\"Upstream error\"}]}";

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final WebClient queryClient;
    private final WebClient streamClient;

    GraphQLProxyController() throws SSLException {
        // Short-lived requests: reuse the shared client (self-signed BMC certs,
        // timeouts, large in-memory limit).
        this.queryClient = Util.createWebClient();
        this.streamClient = createStreamingWebClient();
    }

    /** Client for long-lived SSE streams: no read/response timeouts. */
    private static WebClient createStreamingWebClient() throws SSLException {
        ConnectionProvider provider = ConnectionProvider.builder("graphql-sse")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .secure(t -> t.sslContext(sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static URI upstreamUri(String path, HttpServletRequest request) {
        String query = request.getQueryString();
        return URI.create(Util.base(CommonCommands.machine) + path + (query != null ? "?" + query : ""));
    }

    private static HttpHeaders forwardedHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : FORWARDED_HEADERS) {
            String value = request.getHeader(name);
            if (value != null) {
                headers.add(name, value);
            }
        }
        return headers;
    }

    /** One-shot GraphQL query/mutation. */
    @RequestMapping(value = GRAPHQL_PATH, method = { RequestMethod.GET, RequestMethod.POST })
    public Mono<ResponseEntity<String>> query(@RequestBody(required = false) String body,
            HttpServletRequest request) {
        URI target = upstreamUri(GRAPHQL_PATH, request);
        HttpHeaders headers = forwardedHeaders(request);

        WebClient.RequestBodySpec spec = queryClient.method(HttpMethod.valueOf(request.getMethod()))
                .uri(target)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.addAll(headers));
        WebClient.RequestHeadersSpec<?> ready = body != null ? spec.bodyValue(body) : spec;

        return ready.exchangeToMono(resp -> resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(b -> ResponseEntity.status(resp.statusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(b)))
                .onErrorResume(ex -> {
                    System.err.println("GraphQL proxy request to " + target.getAuthority() + " failed: " + describe(ex));
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(UPSTREAM_ERROR_BODY));
                });
    }

    /**
     * GraphQL subscription over SSE. The SSE stream from the BMC is relayed event
     * by event. Because the response is already committed once streaming starts,
     * upstream failures are reported in-band as a GraphQL {@code errors} event and
     * the stream is then completed.
     */
    @GetMapping(path = SUBSCRIBE_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribe(HttpServletRequest request) {
        URI target = upstreamUri(SUBSCRIBE_PATH, request);
        HttpHeaders headers = forwardedHeaders(request);

        return streamClient.get()
                .uri(target)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(h -> h.addAll(headers))
                .exchangeToFlux(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToFlux(SSE_TYPE);
                    }
                    String body = resp.statusCode().value() == HttpStatus.FORBIDDEN.value()
                            ? PERMISSION_ERROR_BODY
                            : GENERIC_ERROR_BODY;
                    return resp.releaseBody().thenMany(Flux.just(errorEvent(body)));
                })
                .onErrorResume(ex -> {
                    System.err.println("GraphQL proxy stream to " + target.getAuthority() + " failed: " + describe(ex));
                    return Flux.just(errorEvent(UPSTREAM_ERROR_BODY));
                });
    }

    /** Server-side log text only: exception type plus root cause, never sent to the phone. */
    private static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return ex.getClass().getSimpleName() + " / " + root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static ServerSentEvent<String> errorEvent(String json) {
        return ServerSentEvent.<String>builder().data(json).build();
    }
}
