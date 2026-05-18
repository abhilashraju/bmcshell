package com.ibm.bmcshell.console;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
        return CompletableFuture.runAsync(() -> {
            try {
                // Determine path based on console type
                String path;
                if (consoleType == ConsoleType.BMC_SHELL) {
                    path = "/bmc-console";
                } else {
                    path = consoleId.equals("default") ? "/console0" : "/console/" + consoleId;
                }
                URI uri = new URI("wss://" + bmcUrl + path);

                logger.info("Connecting to BMC {} console: {}",
                        consoleType == ConsoleType.BMC_SHELL ? "shell" : "serial", uri);

                // Create HTTP client with SSL that trusts all certificates
                HttpClient httpClient = HttpClient.create()
                        .secure(sslSpec -> {
                            try {
                                sslSpec.sslContext(SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                        .build());
                            } catch (SSLException e) {
                                logger.error("Failed to configure SSL", e);
                            }
                        })
                        .headers(headers -> {
                            if (token != null && !token.isEmpty()) {
                                headers.add("X-Auth-Token", token);
                                logger.info("Added X-Auth-Token header (length: {})", token.length());
                            } else {
                                String auth = username + ":" + password;
                                String encodedAuth = java.util.Base64.getEncoder()
                                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                                headers.add("Authorization", "Basic " + encodedAuth);
                                logger.info("Added Basic Authorization header for user: {}", username);
                            }
                        })
                        .responseTimeout(Duration.ofSeconds(30));

                // Connect to WebSocket
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
                                        logger.error("WebSocket error", error);
                                        if (errorHandler != null) {
                                            errorHandler.accept("WebSocket error: " + error.getMessage());
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
                        .subscribe();

                // Wait for connection
                if (!connectLatch.await(30, TimeUnit.SECONDS)) {
                    throw new IOException("Connection timeout");
                }

                logger.info("Successfully connected to BMC console");

            } catch (Exception e) {
                logger.error("Failed to connect to BMC console", e);
                if (errorHandler != null) {
                    errorHandler.accept("Connection failed: " + e.getMessage());
                }
                throw new RuntimeException("Failed to connect", e);
            }
        });
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
