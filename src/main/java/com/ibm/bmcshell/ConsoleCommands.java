package com.ibm.bmcshell;

import com.ibm.bmcshell.console.ObmcConsoleClientReactor;
import com.ibm.bmcshell.console.ObmcConsoleClientReactor.ConsoleType;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Spring Shell commands for OpenBMC console access via WebSocket.
 * Provides interactive serial console access to BMC host systems.
 * Extends CommonCommands to inherit token authentication and base
 * configuration.
 */
@ShellComponent
public class ConsoleCommands extends CommonCommands {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleCommands.class);

    @Autowired
    private Terminal terminal;

    private ObmcConsoleClientReactor activeConsoleClient;

    public ConsoleCommands() throws IOException {
        super();
    }

    /**
     * Connect to the default BMC console (console0)
     * Uses the current machine, username, and token from CommonCommands
     */
    @ShellMethod(value = "Connect to BMC console (default console0)", key = "console-connect")
    public String consoleConnect() {
        return consoleConnectWithId("default");
    }

    /**
     * Connect to a specific BMC console
     * 
     * @param consoleId Console identifier (default, or specific console name)
     */
    @ShellMethod(value = "Connect to specific BMC console", key = "console-connect-id")
    public String consoleConnectWithId(
            @ShellOption(help = "Console ID (default for console0)", defaultValue = "default") String consoleId) {

        try {
            // Ensure we have a valid token
            if (token == null || token.isEmpty()) {
                getToken();
                if (token == null || token.isEmpty()) {
                    return ColorPrinter.red("✗ Failed to obtain authentication token");
                }
            }

            // Close existing connection if any
            if (activeConsoleClient != null && activeConsoleClient.isConnected()) {
                activeConsoleClient.disconnect();
            }

            // Get BMC URL from base()
            String bmcUrl = base();

            // Create new console client using token authentication
            activeConsoleClient = new ObmcConsoleClientReactor.Builder()
                    .bmcUrl(bmcUrl)
                    .username(getUserName())
                    .password(getPasswd())
                    .consoleId(consoleId)
                    .token(token)
                    .onMessage(this::handleConsoleMessage)
                    .onError(this::handleConsoleError)
                    .onConnected(() -> logger.info("Console connected"))
                    .onDisconnected(() -> logger.info("Console disconnected"))
                    .build();

            // Connect
            CompletableFuture<Void> connectFuture = activeConsoleClient.connect();
            connectFuture.get(); // Wait for connection

            String consolePath = consoleId.equals("default") ? "/console0" : "/console/" + consoleId;
            return ColorPrinter.green("✓ Connected to BMC console: " + machine + consolePath);

        } catch (Exception e) {
            logger.error("Failed to connect to console", e);
            return ColorPrinter.red("✗ Failed to connect: " + e.getMessage());
        }
    }

    /**
     * Start interactive console session with special key support
     */
    @ShellMethod(value = "Start interactive console session", key = "console-interactive")
    public String consoleInteractive() {
        if (activeConsoleClient == null || !activeConsoleClient.isConnected()) {
            return ColorPrinter.red("✗ Not connected to console. Use 'console-connect' first.");
        }

        try {
            terminal.writer().println(ColorPrinter.cyan("Starting interactive console session..."));
            terminal.writer().println(ColorPrinter.yellow("Type 'exit' to return to BMC SHELL"));
            terminal.writer().println(
                    ColorPrinter.yellow(
                            "Special keys: Alt+C (Mac: Option+C) for Ctrl-C, Alt+D for Ctrl-D, Alt+Z for Ctrl-Z, Alt+E for ESC"));
            terminal.writer().println(ColorPrinter.yellow("Commands are sent when you press Enter"));
            terminal.writer().flush();

            // Use JLine terminal reader with custom key bindings
            org.jline.reader.LineReader reader = org.jline.reader.LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Bind Alt+C to send Ctrl-C to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-c"),
                    org.jline.keymap.KeyMap.alt('c'));
            // Bind Alt+D to send Ctrl-D to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-d"),
                    org.jline.keymap.KeyMap.alt('d'));
            // Bind Alt+Z to send Ctrl-Z to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-z"),
                    org.jline.keymap.KeyMap.alt('z'));
            // Bind Alt+E to send ESC to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-esc"),
                    org.jline.keymap.KeyMap.alt('e'));

            // Add custom widgets for special keys
            reader.getWidgets().put("send-ctrl-c", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x03 });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-C]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-C", e);
                }
                return true;
            });
            reader.getWidgets().put("send-ctrl-d", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x04 });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-D]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-D", e);
                }
                return true;
            });
            reader.getWidgets().put("send-ctrl-z", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x1A });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-Z]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-Z", e);
                }
                return true;
            });
            reader.getWidgets().put("send-esc", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x1B });
                    terminal.writer().println(ColorPrinter.gray("[Sent ESC]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send ESC", e);
                }
                return true;
            });

            // Read and send commands line by line
            while (activeConsoleClient.isConnected()) {
                try {
                    String line = reader.readLine("");
                    if (line == null || line.equals("exit")) {
                        break;
                    }
                    // Send the command with newline
                    activeConsoleClient.sendText(line + "\n");
                    // Give time for response
                    Thread.sleep(100);
                } catch (org.jline.reader.UserInterruptException e) {
                    // Ctrl+C pressed locally - exit interactive mode
                    terminal.writer().println(ColorPrinter.yellow("\n[Local Ctrl+C - exiting interactive mode]"));
                    terminal.writer().flush();
                    break;
                } catch (org.jline.reader.EndOfFileException e) {
                    // Ctrl+D pressed locally - exit interactive mode
                    terminal.writer().println(ColorPrinter.yellow("\n[Local Ctrl+D - exiting interactive mode]"));
                    terminal.writer().flush();
                    break;
                }
            }

            return ColorPrinter.green("✓ Console session ended");

        } catch (Exception e) {
            logger.error("Error in interactive console", e);
            return ColorPrinter.red("✗ Console error: " + e.getMessage());
        }
    }

    /**
     * Send text to the console
     * 
     * @param text Text to send
     */
    @ShellMethod(value = "Send text to console", key = "console-send")
    public String consoleSend(
            @ShellOption(help = "Text to send to console") String text) {

        if (activeConsoleClient == null || !activeConsoleClient.isConnected()) {
            return ColorPrinter.red("✗ Not connected to console. Use 'console-connect' first.");
        }

        try {
            activeConsoleClient.sendText(text + "\n");
            return ColorPrinter.green("✓ Sent: " + text);
        } catch (IOException e) {
            logger.error("Failed to send to console", e);
            return ColorPrinter.red("✗ Failed to send: " + e.getMessage());
        }
    }

    /**
     * Send a command to the console
     * 
     * @param command Command to execute
     */
    @ShellMethod(value = "Execute command on console", key = "console-exec")
    public String consoleExec(
            @ShellOption(help = "Command to execute") String command) {

        if (activeConsoleClient == null || !activeConsoleClient.isConnected()) {
            return ColorPrinter.red("✗ Not connected to console. Use 'console-connect' first.");
        }

        try {
            // Send command with newline
            activeConsoleClient.sendText(command + "\n");

            // Wait a bit for response
            Thread.sleep(500);

            return ColorPrinter.green("✓ Executed: " + command);
        } catch (Exception e) {
            logger.error("Failed to execute command", e);
            return ColorPrinter.red("✗ Failed to execute: " + e.getMessage());
        }
    }

    /**
     * Disconnect from console
     */
    @ShellMethod(value = "Disconnect from console", key = "console-disconnect")
    public String consoleDisconnect() {
        if (activeConsoleClient == null) {
            return ColorPrinter.yellow("⚠ No active console connection");
        }

        try {
            activeConsoleClient.disconnect();
            activeConsoleClient = null;
            return ColorPrinter.green("✓ Disconnected from console");
        } catch (Exception e) {
            logger.error("Error disconnecting", e);
            return ColorPrinter.red("✗ Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Check console connection status
     */
    @ShellMethod(value = "Check console connection status", key = "console-status")
    public String consoleStatus() {
        if (activeConsoleClient == null) {
            return ColorPrinter.yellow("⚠ No console client initialized");
        }

        if (activeConsoleClient.isConnected()) {
            return ColorPrinter.green("✓ Console connected to " + machine);
        } else {
            return ColorPrinter.red("✗ Console disconnected");
        }
    }

    /**
     * Value provider for console-key command
     */
    @Component
    public static class ConsoleKeyValueProvider implements ValueProvider {
        private static final List<String> AVAILABLE_KEYS = Arrays.asList(
                "enter", "tab", "ctrl-c", "ctrl-d", "esc", "ctrl-z");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String currentInput = context.currentWordUpToCursor();
            return AVAILABLE_KEYS.stream()
                    .filter(key -> key.startsWith(currentInput.toLowerCase()))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Send special key sequences to console
     *
     * @param key Special key (enter, tab, ctrl-c, ctrl-d, etc.)
     */
    @ShellMethod(value = "Send special key to console", key = "console-key")
    public String consoleKey(
            @ShellOption(help = "Special key: enter, tab, ctrl-c, ctrl-d, esc, ctrl-z", valueProvider = ConsoleKeyValueProvider.class) String key) {

        if (activeConsoleClient == null || !activeConsoleClient.isConnected()) {
            return ColorPrinter.red("✗ Not connected to console. Use 'console-connect' first.");
        }

        try {
            byte[] keyBytes;
            switch (key.toLowerCase()) {
                case "enter":
                    keyBytes = "\n".getBytes(StandardCharsets.UTF_8);
                    break;
                case "tab":
                    keyBytes = "\t".getBytes(StandardCharsets.UTF_8);
                    break;
                case "ctrl-c":
                    keyBytes = new byte[] { 0x03 }; // ETX
                    break;
                case "ctrl-d":
                    keyBytes = new byte[] { 0x04 }; // EOT
                    break;
                case "esc":
                    keyBytes = new byte[] { 0x1B }; // ESC
                    break;
                case "ctrl-z":
                    keyBytes = new byte[] { 0x1A }; // SUB
                    break;
                default:
                    return ColorPrinter.red("✗ Unknown key: " + key);
            }

            activeConsoleClient.sendData(keyBytes);
            return ColorPrinter.green("✓ Sent key: " + key);

        } catch (IOException e) {
            logger.error("Failed to send key", e);
            return ColorPrinter.red("✗ Failed to send key: " + e.getMessage());
        }
    }

    /**
     * Handle incoming console messages
     */
    private void handleConsoleMessage(byte[] data) {
        try {
            String message = new String(data, StandardCharsets.UTF_8);
            terminal.writer().print(message);
            terminal.writer().flush();
        } catch (Exception e) {
            logger.error("Error handling console message", e);
        }
    }

    /**
     * Handle console errors
     */
    private void handleConsoleError(String error) {
        try {
            terminal.writer().println(ColorPrinter.red("\n✗ Console error: " + error));
            terminal.writer().flush();
        } catch (Exception e) {
            logger.error("Error handling console error", e);
        }
    }

    /**
     * Connect to BMC shell (PTY-based login shell)
     * Uses /bmc-console route which provides a login shell on the BMC
     */
    @ShellMethod(value = "Connect to BMC shell (PTY-based login)", key = "bmc-shell-connect")
    public String bmcShellConnect() {
        try {
            // Ensure we have a valid token
            if (token == null || token.isEmpty()) {
                getToken();
                if (token == null || token.isEmpty()) {
                    return ColorPrinter.red("✗ Failed to obtain authentication token");
                }
            }

            // Close existing connection if any
            if (activeConsoleClient != null && activeConsoleClient.isConnected()) {
                activeConsoleClient.disconnect();
            }

            // Get BMC URL from base()
            String bmcUrl = base();

            // Create new console client for BMC shell using token authentication
            activeConsoleClient = new ObmcConsoleClientReactor.Builder()
                    .bmcUrl(bmcUrl)
                    .username(getUserName())
                    .password(getPasswd())
                    .consoleId("bmc-shell") // Not used for BMC_SHELL type
                    .consoleType(ConsoleType.BMC_SHELL)
                    .token(token)
                    .onMessage(this::handleConsoleMessage)
                    .onError(this::handleConsoleError)
                    .onConnected(() -> logger.info("BMC shell connected"))
                    .onDisconnected(() -> logger.info("BMC shell disconnected"))
                    .build();

            // Connect
            CompletableFuture<Void> connectFuture = activeConsoleClient.connect();
            connectFuture.get(); // Wait for connection

            return ColorPrinter.green("✓ Connected to BMC shell: " + machine + "/bmc-console");

        } catch (Exception e) {
            logger.error("Failed to connect to BMC shell", e);
            return ColorPrinter.red("✗ Failed to connect: " + e.getMessage());
        }
    }

    /**
     * Start interactive BMC shell session with special key support
     */
    @ShellMethod(value = "Start interactive BMC shell session", key = "bmc-shell-interactive")
    public String bmcShellInteractive() {
        if (activeConsoleClient == null || !activeConsoleClient.isConnected()) {
            return ColorPrinter.red("✗ Not connected to BMC shell. Use 'bmc-shell-connect' first.");
        }

        try {
            terminal.writer().println(ColorPrinter.cyan("Starting interactive BMC shell session..."));
            terminal.writer().println(ColorPrinter.yellow("Type 'exit' to return to BMC SHELL"));
            terminal.writer().println(
                    ColorPrinter.yellow(
                            "Special keys: Alt+C (Mac: Option+C) for Ctrl-C, Alt+D for Ctrl-D, Alt+Z for Ctrl-Z, Alt+E for ESC"));
            terminal.writer().println(ColorPrinter.yellow("Commands are sent when you press Enter"));
            terminal.writer().flush();

            // Use JLine terminal reader with custom key bindings
            org.jline.reader.LineReader reader = org.jline.reader.LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Bind Alt+C to send Ctrl-C to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-c"),
                    org.jline.keymap.KeyMap.alt('c'));
            // Bind Alt+D to send Ctrl-D to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-d"),
                    org.jline.keymap.KeyMap.alt('d'));
            // Bind Alt+Z to send Ctrl-Z to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-ctrl-z"),
                    org.jline.keymap.KeyMap.alt('z'));
            // Bind Alt+E to send ESC to remote console
            reader.getKeyMaps().get("main").bind(new org.jline.reader.Reference("send-esc"),
                    org.jline.keymap.KeyMap.alt('e'));

            // Add custom widgets for special keys
            reader.getWidgets().put("send-ctrl-c", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x03 });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-C]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-C", e);
                }
                return true;
            });
            reader.getWidgets().put("send-ctrl-d", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x04 });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-D]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-D", e);
                }
                return true;
            });
            reader.getWidgets().put("send-ctrl-z", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x1A });
                    terminal.writer().println(ColorPrinter.gray("[Sent Ctrl-Z]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send Ctrl-Z", e);
                }
                return true;
            });
            reader.getWidgets().put("send-esc", () -> {
                try {
                    activeConsoleClient.sendData(new byte[] { 0x1B });
                    terminal.writer().println(ColorPrinter.gray("[Sent ESC]"));
                    terminal.writer().flush();
                } catch (Exception e) {
                    logger.error("Failed to send ESC", e);
                }
                return true;
            });

            // Read and send commands line by line
            while (activeConsoleClient.isConnected()) {
                try {
                    String line = reader.readLine("");
                    if (line == null || line.equals("exit")) {
                        break;
                    }
                    // Send the command with newline
                    activeConsoleClient.sendText(line + "\n");
                    // Give time for response
                    Thread.sleep(100);
                } catch (org.jline.reader.UserInterruptException e) {
                    // Ctrl+C pressed locally - exit interactive mode
                    terminal.writer().println(ColorPrinter.yellow("\n[Local Ctrl+C - exiting interactive mode]"));
                    terminal.writer().flush();
                    break;
                } catch (org.jline.reader.EndOfFileException e) {
                    // Ctrl+D pressed locally - exit interactive mode
                    terminal.writer().println(ColorPrinter.yellow("\n[Local Ctrl+D - exiting interactive mode]"));
                    terminal.writer().flush();
                    break;
                }
            }

            return ColorPrinter.green("✓ BMC shell session ended");

        } catch (Exception e) {
            logger.error("Error in interactive BMC shell", e);
            return ColorPrinter.red("✗ Shell error: " + e.getMessage());
        }
    }
}
