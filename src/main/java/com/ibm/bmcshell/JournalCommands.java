package com.ibm.bmcshell;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import com.ibm.bmcshell.rest.JournalSseController;

@ShellComponent
public class JournalCommands extends CommonCommands {
    protected Thread journalThread;
    private final Object journalLock = new Object();
    private final Deque<String> journalLines = new ArrayDeque<>();
    private static final int JOURNAL_BUFFER_MAX_LINES = 1000;
    private boolean webSocketStreamingEnabled = true; // Enabled by default

    @Autowired(required = false)
    private JournalSseController sseController;

    @Value("${server.port:8443}")
    private int serverPort;

    protected JournalCommands() throws IOException {
    }

    private String sanitizeLine(String line) {
        if (line == null) {
            return "";
        }
        return line.replace("\r", "").replace("\n", "");
    }

    private void appendJournalLine(String line) {
        synchronized (journalLock) {
            if (journalLines.size() >= JOURNAL_BUFFER_MAX_LINES) {
                journalLines.removeFirst();
            }
            String sanitized = sanitizeLine(line);
            journalLines.addLast(sanitized);

            // Broadcast to SSE clients if streaming is enabled
            if (webSocketStreamingEnabled && sseController != null) {
                sseController.broadcastJournalLine(sanitized);
            }
        }
    }

    private void activateJournalCapture() {
        synchronized (journalLock) {
            journalLines.clear();
            appendJournalLine("journal capture active");
            outputConsumerOverride = line -> System.out.println(line);
        }
    }

    private void deactivateJournalCapture(String statusLine) {
        synchronized (journalLock) {
            if (statusLine != null && !statusLine.isBlank()) {
                appendJournalLine(statusLine);
            }
            outputConsumerOverride = null;
        }
    }

    private Consumer<String> journalConsumer() {
        return line -> appendJournalLine(line);
    }

    /**
     * Enable or disable SSE streaming for journal logs
     *
     * @param enabled true to enable SSE streaming, false to disable
     */
    public void setWebSocketStreaming(boolean enabled) {
        this.webSocketStreamingEnabled = enabled;
        if (enabled && sseController != null) {
            System.out.println("SSE streaming enabled. Active connections: " +
                    sseController.getActiveConnectionCount());
        } else {
            System.out.println("SSE streaming disabled.");
        }
    }

    /**
     * Check if SSE streaming is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isWebSocketStreamingEnabled() {
        return webSocketStreamingEnabled;
    }

    private void runJournalCommand(String command) {
        scmd(command, journalConsumer());
    }

    @ShellMethod(key = "journal.start", value = "eg: journal.start arg ")
    void journalctl(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] filter) throws IOException {
        journalctlStop(); // Stop any existing journal thread
        StringBuffer command = new StringBuffer();
        command.append("journalctl -f");
        Stream.of(filter).map(f -> {
            return ServiceCommands.ServiceProvider.serviceNames.stream().filter(s -> s.contains(f));
        }).flatMap(s -> s).forEach(s -> {
            command.append(" -u ").append(s);
        });
        activateJournalCapture();
        Thread journalThread = new Thread(() -> runJournalCommand(command.toString()));
        journalThread.setName("JournalCtlThread");
        journalThread.start();

        // Store the thread reference for control commands
        this.journalThread = journalThread;
        return;

    }

    @ShellMethod(key = "journal.search", value = "eg: journal.search arg1 arg2 arg3")
    void journalSearch(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms) throws IOException {
        journalSearchN(searchTerms, 100);
    }

    @ShellMethod(key = "journal.searchN", value = "eg: journal.searchN arg1 arg2 arg3 -n 50")
    void journalSearchN(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms,
            @ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        journalctlStop(); // Stop any existing journal thread

        if (searchTerms[0].equals("*")) {
            runJournalCommand(String.format("journalctl | tail -n %d", n));
            return;
        }

        // Build grep command with OR pattern (matches any of the search terms)
        String pattern = String.join("|", searchTerms);
        runJournalCommand(String.format("journalctl | grep -E '%s' | tail -n %d", pattern, n));
    }

    @ShellMethod(key = "journal.stop", value = "eg: journal.stop")
    void journalctlStop() {
        if (journalThread != null && journalThread.isAlive()) {
            journalThread.interrupt();
            deactivateJournalCapture("JournalCtl thread stopped.");
        }
    }

    @ShellMethod(key = "journal.clear", value = "eg: journal.clear")
    void journalctlClear() {
        runJournalCommand(" journalctl --rotate; journalctl --vacuum-time=1s");
    }

    @ShellMethod(key = "journal.show", value = "Show captured journal logs. eg: journal.show")
    void journalShow() {
        synchronized (journalLock) {
            System.out.println("\n=== Captured Journal Logs ===\n");
            if (journalLines.isEmpty()) {
                System.out.println("(no captured journal logs)");
            } else {
                journalLines.forEach(System.out::println);
            }
            System.out.println();
        }
    }

    @ShellMethod(key = "journal.ws.enable", value = "Enable SSE streaming. eg: journal.ws.enable")
    void journalWebSocketEnable() {
        setWebSocketStreaming(true);
    }

    @ShellMethod(key = "journal.ws.disable", value = "Disable SSE streaming. eg: journal.ws.disable")
    void journalWebSocketDisable() {
        setWebSocketStreaming(false);
    }

    @ShellMethod(key = "journal.ws.status", value = "Show SSE streaming status. eg: journal.ws.status")
    void journalWebSocketStatus() {
        System.out.println("\n=== SSE Streaming Status ===");
        System.out.println("Streaming Enabled: " + webSocketStreamingEnabled);
        if (sseController != null) {
            System.out.println("Active SSE Connections: " + sseController.getActiveConnectionCount());

            // SSL enabled by default for all ports
            String endpoint = String.format("https://localhost:%d/sse/journal", serverPort);

            System.out.println("\nSSE Endpoint: " + endpoint);
            System.out.println("\nConnect with curl:");
            System.out.println("  curl -k -N " + endpoint);
            System.out.println("\nConnect with Python:");
            System.out.println("  python3 -c \"import requests; r=requests.get('" + endpoint
                    + "', stream=True, verify=False); [print(line.decode()) for line in r.iter_lines()]\"");
        } else {
            System.out.println("SSE Controller: Not available");
        }
        System.out.println();
    }

    @ShellMethod(key = "journal.help", value = "Show journal commands help. eg: journal.help")
    void journalHelp() {
        System.out.println("\n=== Journal Log Commands ===\n");
        System.out.println("journal.start <filter>    - Start following journal logs for services matching filter");
        System.out.println("                            Filters services by name (e.g., 'bmcweb', 'network')");
        System.out
                .println("                            Journal logs are captured without interleaving into main output");
        System.out.println("journal.stop              - Stop journal log monitoring");
        System.out.println("\njournal.search <terms>    - Search journal logs (default: last 100 lines)");
        System.out.println("                            Use '*' to show all logs");
        System.out.println("journal.searchN <terms> -n <num> - Search with custom limit");
        System.out.println("\njournal.show              - Show captured journal logs");
        System.out.println("journal.clear             - Clear journal logs (rotate and vacuum)");
        System.out.println("\n=== SSE Streaming Commands ===");
        System.out.println("journal.ws.enable         - Enable SSE streaming for live journal updates");
        System.out.println("journal.ws.disable        - Disable SSE streaming");
        System.out.println("journal.ws.status         - Show SSE streaming status and active connections");
        System.out.println("\njournal.help              - Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  journal.start bmcweb");
        System.out.println("  journal.start network cert");
        System.out.println("  journal.search error warning");
        System.out.println("  journal.searchN certificate -n 50");
        System.out.println("  journal.search '*'");
        System.out.println("  journal.show");
        System.out.println("  journal.clear");
        System.out.println("  journal.ws.enable");
        System.out.println("  journal.ws.status");
        System.out.println("  journal.stop\n");
    }
}
