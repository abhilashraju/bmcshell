package com.ibm.bmcshell.Utils;

import com.ibm.bmcshell.ColorPrinter;
import com.ibm.bmcshell.CustomPromptProvider;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ConnectivityMonitor {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private volatile Boolean isConnected = null; // null = unknown initial state
    private Thread connectivityCheckerThread;
    private volatile boolean stopConnectivityChecker = false;
    private String currentMachine;
    private String currentUserName;
    private int currentPort;
    private CustomPromptProvider promptProvider;
    private InetAddress cachedAddress = null; // Cache resolved address
    private long lastDnsResolveAttempt = 0;
    private static final long DNS_RESOLVE_RETRY_INTERVAL = 30000; // Retry DNS every 30 seconds

    public void startMonitoring(String machine, String userName, int port, CustomPromptProvider promptProvider) {
        // Stop existing checker if running
        stopMonitoring();

        // Use the same fully qualified name logic as SSH and Redfish connections
        this.currentMachine = Util.fullMachineName(machine);
        this.currentUserName = userName;
        this.currentPort = port;
        this.promptProvider = promptProvider;
        this.stopConnectivityChecker = false;
        this.cachedAddress = null; // Reset cached address
        this.isConnected = null; // Reset connection state

        System.out.println("[ConnectivityMonitor] Starting monitoring for: " + this.currentMachine);

        connectivityCheckerThread = new Thread(() -> {
            while (!stopConnectivityChecker) {
                try {
                    boolean connected = checkMachineReachability();

                    // Update if state changed OR if this is the first check (isConnected == null)
                    if (isConnected == null || connected != isConnected) {
                        isConnected = connected;
                        updatePromptBasedOnConnectivity();
                    }

                    Thread.sleep(1000); // Check every 1 second
                } catch (InterruptedException e) {
                    System.err.println("[ConnectivityMonitor] Thread interrupted for " + currentMachine);
                    break;
                } catch (Exception e) {
                    System.err.println("[ConnectivityMonitor] Error checking connectivity for " + currentMachine + ": "
                            + e.getMessage());
                }
            }
        });
        connectivityCheckerThread.setDaemon(true);
        connectivityCheckerThread.setName("ConnectivityChecker-" + machine);
        connectivityCheckerThread.start();
    }

    public void stopMonitoring() {
        if (connectivityCheckerThread != null && connectivityCheckerThread.isAlive()) {
            stopConnectivityChecker = true;
            connectivityCheckerThread.interrupt();
            try {
                connectivityCheckerThread.join(10000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private boolean checkMachineReachability() {
        try {
            // Try to resolve DNS if we don't have a cached address or it's time to retry
            long currentTime = System.currentTimeMillis();
            if (cachedAddress == null && (currentTime - lastDnsResolveAttempt) > DNS_RESOLVE_RETRY_INTERVAL) {
                lastDnsResolveAttempt = currentTime;
                try {
                    cachedAddress = InetAddress.getByName(currentMachine);
                    System.out.println("[ConnectivityMonitor] Successfully resolved " + currentMachine + " to "
                            + cachedAddress.getHostAddress());
                } catch (UnknownHostException e) {
                    // DNS resolution failed - check if currentMachine is already an IP address
                    if (isIpAddress(currentMachine)) {
                        System.out.println("[ConnectivityMonitor] Using IP address directly: " + currentMachine);
                        cachedAddress = InetAddress.getByName(currentMachine);
                    } else {
                        System.err.println("[ConnectivityMonitor] DNS resolution failed for " + currentMachine + ": "
                                + e.getMessage());
                        System.err.println("[ConnectivityMonitor] Will retry DNS resolution in 30 seconds");
                        return false;
                    }
                }
            }

            // If we still don't have an address, we can't check connectivity
            if (cachedAddress == null) {
                return false;
            }

            // Use ICMP ping for reachability check
            boolean reachable = cachedAddress.isReachable(2000);

            // If still not reachable, invalidate cached address for next DNS retry
            if (!reachable) {
                System.err.println("[ConnectivityMonitor] Machine " + currentMachine + " ("
                        + cachedAddress.getHostAddress() + ") is not reachable");
                cachedAddress = null; // Force DNS re-resolution on next retry interval
            }

            return reachable;
        } catch (Exception e) {
            System.err.println("[ConnectivityMonitor] Exception checking reachability for " + currentMachine + ": "
                    + e.getMessage());
            cachedAddress = null; // Force DNS re-resolution
            return false;
        }
    }

    private boolean isIpAddress(String host) {
        // Simple check for IPv4 address pattern
        return host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    private void updatePromptBasedOnConnectivity() {
        try {
            if (promptProvider == null) {
                return;
            }

            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

            if (isConnected) {
                // Connected state - green
                System.out.println(ColorPrinter.green("\n[" + timestamp + "] ✓ Connected to " + currentMachine));
                promptProvider.setShellData(
                        new CustomPromptProvider.ShellData(
                                currentUserName + "@" + currentMachine,
                                AttributedStyle.GREEN,
                                AttributedStyle.BLACK));
            } else {
                // Disconnected state - gray
                System.out.println(ColorPrinter.red("\n[" + timestamp + "] ✗ Disconnected from " + currentMachine));
                promptProvider.setShellData(
                        new CustomPromptProvider.ShellData(
                                currentUserName + "@" + currentMachine + " [DISCONNECTED]",
                                AttributedStyle.WHITE,
                                AttributedStyle.BLACK));
            }
        } catch (Exception e) {
            // Ignore errors updating prompt
        }
    }

    public boolean isConnected() {
        return isConnected != null && isConnected;
    }
}
