package com.ibm.bmcshell.console;

import com.ibm.bmcshell.exceptions.HttpClientException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Reactor Netty-based WebSocket client for OpenBMC console access.
 * Uses Reactor Netty instead of Tyrus to avoid authentication issues.
 *
 * Supports two types of console connections:
 * 1. Serial Console: /console0 or /console/<id> - Direct serial console access
 * 2. BMC Shell: /bmc-console - PTY-based login shell on the BMC
 */
public class ObmcConsoleClientReactor {

    private static final Logger logger = LoggerFactory.getLogger(ObmcConsoleClientReactor.class);

    private final String bmcUrl;
    private final String username;
    private final String password;
    private final String token;
    private final String consoleId;
    private final ConsoleType consoleType;

    /**
     * Console type enumeration
     */
    public enum ConsoleType {
        /** Serial console access via /console0 or /console/<id> */
        SERIAL_CONSOLE,
        /** BMC shell access via /bmc-console */
        BMC_SHELL
    }

    private Consumer<byte[]> messageHandler;
    private Consumer<String> errorHandler;
    private Runnable connectionEstablishedHandler;
    private Runnable connectionClosedHandler;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    private WebsocketOutbound outbound;
    private Thread inputThread;

    public ObmcConsoleClientReactor(String bmcUrl, String username, String password, String consoleId) {
        this(bmcUrl, username, password, null, consoleId, ConsoleType.SERIAL_CONSOLE);
    }

    public ObmcConsoleClientReactor(String bmcUrl, String username, String password, String token, String consoleId) {
        this(bmcUrl, username, password, token, consoleId, ConsoleType.SERIAL_CONSOLE);
    }

    public ObmcConsoleClientReactor(String bmcUrl, String username, String password, String token,
            String consoleId, ConsoleType consoleType) {
        this.bmcUrl = bmcUrl.replaceFirst("^https?://", "");
        this.username = username;
        this.password = password;
        this.token = token;
        this.consoleId = consoleId;
        this.consoleType = consoleType;
    }

    public void setMessageHandler(Consumer<byte[]> handler) {
        this.messageHandler = handler;
    }

    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }

    public void setConnectionEstablishedHandler(Runnable handler) {
        this.connectionEstablishedHandler = handler;
    }

    public void setConnectionClosedHandler(Runnable handler) {
        this.connectionClosedHandler = handler;
    }

    public CompletableFuture<Void> connect() {
        return connectWithRetry(0);
    }

    /**
     * Connect with retry logic based on error type
     */
    private CompletableFuture<Void> connectWithRetry(int attemptNumber) {
        return CompletableFuture.runAsync(() -> {
            AtomicReference<HttpClientException> lastException = new AtomicReference<>();

            try {
                // Determine path based on console type
                String path;
                if (consoleType == ConsoleType.BMC_SHELL) {
                    path = "/bmc-console";
                } else {
                    path = consoleId.equals("default") ? "/console0" : "/console/" + consoleId;
                }
                URI uri = new URI("wss://" + bmcUrl + path);

                logger.info("Connecting to BMC {} console: {} (attempt {})",
                        consoleType == ConsoleType.BMC_SHELL ? "shell" : "serial", uri, attemptNumber + 1);

                // Create HTTP client with SSL that trusts all certificates
                HttpClient httpClient = HttpClient.create()
                        .secure(sslSpec -> {
                            try {
                                sslSpec.sslContext(SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                        .build());
                            } catch (SSLException e) {
                                logger.error("Failed to configure SSL", e);
                                lastException.set(HttpClientException.sslError("SSL configuration failed", e));
                            }
                        })
                        .headers(headers -> {
                            if (token != null && !token.isEmpty()) {
                                headers.add("X-Auth-Token", token);
                                logger.debug("Added X-Auth-Token header");
                            } else {
                                String auth = username + ":" + password;
                                String encodedAuth = java.util.Base64.getEncoder()
                                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                                headers.add("Authorization", "Basic " + encodedAuth);
                                logger.debug("Added Basic Authorization header for user: {}", username);
                            }
                        })
                        .responseTimeout(Duration.ofSeconds(30));

                // Connect to WebSocket with error handling
                httpClient.websocket()
                        .uri(uri.toString())
                        .handle((inbound, outbound) -> {
                            this.outbound = outbound;
                            connected.set(true);
                            connectLatch.countDown();

                            logger.info("WebSocket connection established");
                            if (connectionEstablishedHandler != null) {
                                connectionEstablishedHandler.run();
                            }

                            // Handle incoming messages
                            return inbound.receive()
                                    .asByteArray()
                                    .doOnNext(data -> {
                                        logger.debug("Received {} bytes from console", data.length);
                                        if (messageHandler != null) {
                                            messageHandler.accept(data);
                                        }
                                    })
                                    .doOnError(error -> {
                                        HttpClientException httpEx = categorizeError(error);
                                        logger.error("WebSocket error: {}", httpEx.getUserMessage());
                                        if (errorHandler != null) {
                                            errorHandler.accept(httpEx.getUserMessage());
                                        }
                                    })
                                    .doOnComplete(() -> {
                                        logger.info("WebSocket connection closed");
                                        connected.set(false);
                                        closeLatch.countDown();
                                        if (connectionClosedHandler != null) {
                                            connectionClosedHandler.run();
                                        }
                                    })
                                    .then();
                        })
                        .doOnError(error -> {
                            HttpClientException httpEx = categorizeError(error);
                            lastException.set(httpEx);
                            logger.error("Connection error: {}", httpEx.getUserMessage());
                        })
                        .subscribe();

                // Wait for connection
                if (!connectLatch.await(30, TimeUnit.SECONDS)) {
                    throw HttpClientException.timeoutError("Connection timeout after 30 seconds");
                }

                logger.info("Successfully connected to BMC console");

            } catch (HttpClientException e) {
                lastException.set(e);
                handleConnectionError(e, attemptNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                HttpClientException httpEx = new HttpClientException(
                        HttpClientException.ErrorType.UNKNOWN_ERROR, "Connection interrupted", e);
                lastException.set(httpEx);
                handleConnectionError(httpEx, attemptNumber);
            } catch (Exception e) {
                HttpClientException httpEx = categorizeError(e);
                lastException.set(httpEx);
                handleConnectionError(httpEx, attemptNumber);
            }

            // If we have an exception and should retry, do so
            HttpClientException finalException = lastException.get();
            if (finalException != null) {
                if (shouldRetry(finalException, attemptNumber)) {
                    logger.info("Retrying connection (attempt {}/{})",
                            attemptNumber + 2, finalException.getMaxRetryAttempts() + 1);
                    try {
                        Thread.sleep(1000 * (attemptNumber + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    connectWithRetry(attemptNumber + 1).join();
                } else {
                    throw new RuntimeException(finalException);
                }
            }
        });
    }

    /**
     * Categorize exception into HttpClientException
     */
    private HttpClientException categorizeError(Throwable error) {
        // Unwrap if needed
        Throwable cause = error.getCause() != null ? error.getCause() : error;

        // Network connectivity errors
        if (cause instanceof UnknownHostException) {
            return HttpClientException.connectivityError(
                    "Cannot resolve hostname: " + bmcUrl + ". Please check the BMC address.", cause);
        }
        if (cause instanceof ConnectException || cause instanceof ConnectTimeoutException) {
            return HttpClientException.connectivityError(
                    "Cannot connect to BMC at " + bmcUrl
                            + ". Please verify the BMC is reachable and the network is working.",
                    cause);
        }
        if (cause instanceof ClosedChannelException) {
            return HttpClientException.connectivityError(
                    "Connection closed unexpectedly. The BMC may have rejected the connection.", cause);
        }

        // SSL/TLS errors
        if (cause instanceof SSLException || cause instanceof SSLHandshakeException) {
            return HttpClientException.sslError(
                    "SSL/TLS handshake failed. This may indicate certificate issues or incompatible SSL protocols.",
                    cause);
        }

        // Timeout errors
        if (cause instanceof TimeoutException) {
            return HttpClientException.timeoutError(
                    "Request timed out. The BMC may be slow to respond or unreachable.");
        }

        // Check for HTTP status codes in error message
        String errorMsg = error.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                return HttpClientException.authError(
                        "Authentication failed. Please check your username, password, or token.");
            }
            if (errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
                return HttpClientException.fromStatusCode(403,
                        "Access forbidden. You may not have permission to access this console.");
            }
            if (errorMsg.contains("404") || errorMsg.contains("Not Found")) {
                return HttpClientException.fromStatusCode(404,
                        "Console endpoint not found. The BMC may not support this console type.");
            }
            if (errorMsg.matches(".*\\b[45]\\d{2}\\b.*")) {
                // Extract status code
                try {
                    int statusCode = Integer.parseInt(errorMsg.replaceAll(".*\\b([45]\\d{2})\\b.*", "$1"));
                    return HttpClientException.fromStatusCode(statusCode, errorMsg);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Default unknown error
        return new HttpClientException(HttpClientException.ErrorType.UNKNOWN_ERROR,
                "Unexpected error: " + error.getMessage(), error);
    }

    /**
     * Handle connection error with appropriate logging and user notification
     */
    private void handleConnectionError(HttpClientException exception, int attemptNumber) {
        String userMessage = exception.getUserMessage();

        // Log based on error type
        switch (exception.getErrorType()) {
            case CONNECTIVITY_ERROR:
                logger.error("Network connectivity issue: {}", exception.getMessage());
                break;
            case CLIENT_ERROR:
                logger.error("Client error (HTTP {}): {}", exception.getStatusCode(), exception.getMessage());
                break;
            case SERVER_ERROR:
                logger.warn("Server error (HTTP {}): {}", exception.getStatusCode(), exception.getMessage());
                break;
            case SSL_ERROR:
                logger.error("SSL/TLS error: {}", exception.getMessage());
                break;
            case AUTH_ERROR:
                logger.error("Authentication error: {}", exception.getMessage());
                break;
            case TIMEOUT_ERROR:
                logger.error("Timeout error: {}", exception.getMessage());
                break;
            default:
                logger.error("Unknown error: {}", exception.getMessage());
        }

        // Notify error handler
        if (errorHandler != null) {
            errorHandler.accept(userMessage);
        }
    }

    /**
     * Determine if we should retry based on error type and attempt number
     */
    private boolean shouldRetry(HttpClientException exception, int attemptNumber) {
        // Don't retry if not retryable
        if (!exception.isRetryable()) {
            return false;
        }

        // Check if we've exceeded max retry attempts
        return attemptNumber < exception.getMaxRetryAttempts();
    }

    public void sendData(byte[] data) throws IOException {
        if (outbound == null || !connected.get()) {
            throw new IOException("WebSocket is not connected");
        }

        logger.debug("Sending {} bytes to console: [{}]", data.length,
                new String(data, StandardCharsets.UTF_8).replace("\n", "\\n").replace("\r", "\\r"));

        try {
            // Send as WebSocket binary frame using Mono
            outbound.sendByteArray(Mono.just(data))
                    .then()
                    .subscribe(
                            null,
                            error -> logger.error("Error sending data", error),
                            () -> logger.debug("Send operation completed successfully"));
        } catch (Exception e) {
            logger.error("Failed to send data", e);
            throw new IOException("Failed to send data: " + e.getMessage(), e);
        }
    }

    public void sendText(String text) throws IOException {
        sendData(text.getBytes(StandardCharsets.UTF_8));
    }

    public void disconnect() {
        if (inputThread != null && inputThread.isAlive()) {
            inputThread.interrupt();
        }

        if (outbound != null && connected.get()) {
            try {
                outbound.sendClose().then().subscribe();
                logger.info("Disconnected from BMC console");
            } catch (Exception e) {
                logger.error("Error closing WebSocket", e);
            }
        }

        try {
            closeLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected() {
        return connected.get() && outbound != null;
    }

    public void startInteractiveSession() {
        if (!isConnected()) {
            logger.error("Not connected to console");
            return;
        }

        logger.info("Starting interactive console session. Press Ctrl+D to exit.");

        // Set message handler to print to stdout
        setMessageHandler(data -> {
            System.out.write(data, 0, data.length);
            System.out.flush();
        });

        // Start input thread using raw System.in
        inputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isConnected() && !Thread.currentThread().isInterrupted()) {
                    if (System.in.available() > 0) {
                        int bytesRead = System.in.read(buffer);
                        if (bytesRead > 0) {
                            byte[] data = new byte[bytesRead];
                            System.arraycopy(buffer, 0, data, 0, bytesRead);
                            sendData(data);
                        } else if (bytesRead == -1) {
                            // EOF detected (Ctrl+D)
                            logger.info("EOF detected, closing session");
                            break;
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (IOException e) {
                logger.error("Error reading from stdin", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();

        // Wait for disconnect or Ctrl+C
        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class Builder {
        private String bmcUrl;
        private String username;
        private String password;
        private String token;
        private String consoleId = "default";
        private ConsoleType consoleType = ConsoleType.SERIAL_CONSOLE;
        private Consumer<byte[]> messageHandler;
        private Consumer<String> errorHandler;
        private Runnable connectionEstablishedHandler;
        private Runnable connectionClosedHandler;

        public Builder bmcUrl(String bmcUrl) {
            this.bmcUrl = bmcUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder consoleId(String consoleId) {
            this.consoleId = consoleId;
            return this;
        }

        public Builder consoleType(ConsoleType consoleType) {
            this.consoleType = consoleType;
            return this;
        }

        public Builder onMessage(Consumer<byte[]> handler) {
            this.messageHandler = handler;
            return this;
        }

        public Builder onError(Consumer<String> handler) {
            this.errorHandler = handler;
            return this;
        }

        public Builder onConnected(Runnable handler) {
            this.connectionEstablishedHandler = handler;
            return this;
        }

        public Builder onDisconnected(Runnable handler) {
            this.connectionClosedHandler = handler;
            return this;
        }

        public ObmcConsoleClientReactor build() {
            if (bmcUrl == null || username == null) {
                throw new IllegalStateException("bmcUrl and username are required");
            }

            if (token == null && password == null) {
                throw new IllegalStateException("Either token or password must be provided");
            }

            ObmcConsoleClientReactor client = new ObmcConsoleClientReactor(bmcUrl, username, password, token,
                    consoleId, consoleType);
            client.setMessageHandler(messageHandler);
            client.setErrorHandler(errorHandler);
            client.setConnectionEstablishedHandler(connectionEstablishedHandler);
            client.setConnectionClosedHandler(connectionClosedHandler);

            return client;
        }
    }
}
