package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class SysCtlCommands extends CommonCommands {
    protected SysCtlCommands() throws IOException {
    }

    /**
     * Set reverse path filtering for all interfaces
     * Controls source validation of packets by reverse path
     * 0=off, 1=strict, 2=loose
     * 
     * Example: sysctl.rp-filter --value 0
     * 
     * @param value The rp_filter value (0, 1, or 2)
     */
    @ShellMethod(key = "sysctl.rp-filter", value = "Set reverse path filtering for all interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void rpFilter(
            @ShellOption(value = { "--value", "-v" }) int value) {
        String command = String.format("sysctl -w net.ipv4.conf.all.rp_filter=%d", value);
        scmd(command);
    }

    /**
     * Set reverse path filtering for a specific interface
     * Controls source validation of packets by reverse path
     * 0=off, 1=strict, 2=loose
     * 
     * Example: sysctl.rp-filter-interface --interface eth2 --value 0
     * 
     * @param interfaceName Network interface name
     * @param value         The rp_filter value (0, 1, or 2)
     */
    @ShellMethod(key = "sysctl.rp-filter-interface", value = "Set reverse path filtering for specific interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void rpFilterInterface(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--value", "-v" }) int value) {
        String command = String.format("sysctl -w net.ipv4.conf.%s.rp_filter=%d", interfaceName, value);
        scmd(command);
    }

    /**
     * Enable or disable IP forwarding
     * Allows the system to forward packets between interfaces
     * 
     * Example: sysctl.ip-forward --enable
     * 
     * @param enable Enable (true) or disable (false) IP forwarding
     */
    @ShellMethod(key = "sysctl.ip-forward", value = "Enable or disable IP forwarding")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipForward(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.ip_forward=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable IPv6 forwarding
     * Allows the system to forward IPv6 packets between interfaces
     * 
     * Example: sysctl.ipv6-forward --enable
     * 
     * @param enable Enable (true) or disable (false) IPv6 forwarding
     */
    @ShellMethod(key = "sysctl.ipv6-forward", value = "Enable or disable IPv6 forwarding")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipv6Forward(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv6.conf.all.forwarding=%d", value);
        scmd(command);
    }

    /**
     * Set TCP keepalive time
     * Time before sending keepalive probes (in seconds)
     * 
     * Example: sysctl.tcp-keepalive-time --seconds 7200
     * 
     * @param seconds Keepalive time in seconds (default: 7200)
     */
    @ShellMethod(key = "sysctl.tcp-keepalive-time", value = "Set TCP keepalive time")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpKeepaliveTime(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "7200") int seconds) {
        String command = String.format("sysctl -w net.ipv4.tcp_keepalive_time=%d", seconds);
        scmd(command);
    }

    /**
     * Set TCP keepalive interval
     * Interval between keepalive probes (in seconds)
     * 
     * Example: sysctl.tcp-keepalive-interval --seconds 75
     * 
     * @param seconds Keepalive interval in seconds (default: 75)
     */
    @ShellMethod(key = "sysctl.tcp-keepalive-interval", value = "Set TCP keepalive interval")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpKeepaliveInterval(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "75") int seconds) {
        String command = String.format("sysctl -w net.ipv4.tcp_keepalive_intvl=%d", seconds);
        scmd(command);
    }

    /**
     * Set TCP keepalive probes
     * Number of keepalive probes before giving up
     * 
     * Example: sysctl.tcp-keepalive-probes --count 9
     * 
     * @param count Number of probes (default: 9)
     */
    @ShellMethod(key = "sysctl.tcp-keepalive-probes", value = "Set TCP keepalive probes count")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpKeepaliveProbes(
            @ShellOption(value = { "--count", "-c" }, defaultValue = "9") int count) {
        String command = String.format("sysctl -w net.ipv4.tcp_keepalive_probes=%d", count);
        scmd(command);
    }

    /**
     * Set maximum number of SYN retries
     * Controls connection establishment retry attempts
     * 
     * Example: sysctl.tcp-syn-retries --count 5
     * 
     * @param count Number of SYN retries (default: 5)
     */
    @ShellMethod(key = "sysctl.tcp-syn-retries", value = "Set maximum number of SYN retries")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpSynRetries(
            @ShellOption(value = { "--count", "-c" }, defaultValue = "5") int count) {
        String command = String.format("sysctl -w net.ipv4.tcp_syn_retries=%d", count);
        scmd(command);
    }

    /**
     * Set maximum number of SYN-ACK retries
     * Controls server-side connection retry attempts
     * 
     * Example: sysctl.tcp-synack-retries --count 5
     * 
     * @param count Number of SYN-ACK retries (default: 5)
     */
    @ShellMethod(key = "sysctl.tcp-synack-retries", value = "Set maximum number of SYN-ACK retries")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpSynackRetries(
            @ShellOption(value = { "--count", "-c" }, defaultValue = "5") int count) {
        String command = String.format("sysctl -w net.ipv4.tcp_synack_retries=%d", count);
        scmd(command);
    }

    /**
     * Enable or disable TCP timestamps
     * Improves RTT estimation and PAWS protection
     * 
     * Example: sysctl.tcp-timestamps --enable
     * 
     * @param enable Enable (true) or disable (false) TCP timestamps
     */
    @ShellMethod(key = "sysctl.tcp-timestamps", value = "Enable or disable TCP timestamps")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpTimestamps(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.tcp_timestamps=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable TCP window scaling
     * Allows larger TCP window sizes for high-bandwidth networks
     * 
     * Example: sysctl.tcp-window-scaling --enable
     * 
     * @param enable Enable (true) or disable (false) TCP window scaling
     */
    @ShellMethod(key = "sysctl.tcp-window-scaling", value = "Enable or disable TCP window scaling")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpWindowScaling(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.tcp_window_scaling=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable TCP SACK (Selective Acknowledgment)
     * Improves performance on lossy networks
     * 
     * Example: sysctl.tcp-sack --enable
     * 
     * @param enable Enable (true) or disable (false) TCP SACK
     */
    @ShellMethod(key = "sysctl.tcp-sack", value = "Enable or disable TCP SACK")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpSack(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.tcp_sack=%d", value);
        scmd(command);
    }

    /**
     * Set TCP FIN timeout
     * Time to wait for final FIN packet (in seconds)
     * 
     * Example: sysctl.tcp-fin-timeout --seconds 60
     * 
     * @param seconds FIN timeout in seconds (default: 60)
     */
    @ShellMethod(key = "sysctl.tcp-fin-timeout", value = "Set TCP FIN timeout")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpFinTimeout(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "60") int seconds) {
        String command = String.format("sysctl -w net.ipv4.tcp_fin_timeout=%d", seconds);
        scmd(command);
    }

    /**
     * Set maximum SYN backlog
     * Maximum number of queued connection requests
     * 
     * Example: sysctl.tcp-max-syn-backlog --size 2048
     * 
     * @param size Maximum SYN backlog size (default: 2048)
     */
    @ShellMethod(key = "sysctl.tcp-max-syn-backlog", value = "Set maximum SYN backlog")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpMaxSynBacklog(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "2048") int size) {
        String command = String.format("sysctl -w net.ipv4.tcp_max_syn_backlog=%d", size);
        scmd(command);
    }

    /**
     * Enable or disable SYN cookies
     * Protection against SYN flood attacks
     * 
     * Example: sysctl.tcp-syncookies --enable
     * 
     * @param enable Enable (true) or disable (false) SYN cookies
     */
    @ShellMethod(key = "sysctl.tcp-syncookies", value = "Enable or disable SYN cookies")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpSyncookies(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.tcp_syncookies=%d", value);
        scmd(command);
    }

    /**
     * Set TCP receive buffer sizes (min, default, max)
     * Controls memory allocation for TCP receive buffers
     * 
     * Example: sysctl.tcp-rmem --min 4096 --default 87380 --max 6291456
     * 
     * @param min Minimum size in bytes (default: 4096)
     * @param def Default size in bytes (default: 87380)
     * @param max Maximum size in bytes (default: 6291456)
     */
    @ShellMethod(key = "sysctl.tcp-rmem", value = "Set TCP receive buffer sizes")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpRmem(
            @ShellOption(value = { "--min" }, defaultValue = "4096") int min,
            @ShellOption(value = { "--default" }, defaultValue = "87380") int def,
            @ShellOption(value = { "--max" }, defaultValue = "6291456") int max) {
        String command = String.format("sysctl -w net.ipv4.tcp_rmem='%d %d %d'", min, def, max);
        scmd(command);
    }

    /**
     * Set TCP send buffer sizes (min, default, max)
     * Controls memory allocation for TCP send buffers
     * 
     * Example: sysctl.tcp-wmem --min 4096 --default 16384 --max 4194304
     * 
     * @param min Minimum size in bytes (default: 4096)
     * @param def Default size in bytes (default: 16384)
     * @param max Maximum size in bytes (default: 4194304)
     */
    @ShellMethod(key = "sysctl.tcp-wmem", value = "Set TCP send buffer sizes")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpWmem(
            @ShellOption(value = { "--min" }, defaultValue = "4096") int min,
            @ShellOption(value = { "--default" }, defaultValue = "16384") int def,
            @ShellOption(value = { "--max" }, defaultValue = "4194304") int max) {
        String command = String.format("sysctl -w net.ipv4.tcp_wmem='%d %d %d'", min, def, max);
        scmd(command);
    }

    /**
     * Set core receive buffer default size
     * Default receive buffer size for all protocols
     * 
     * Example: sysctl.core-rmem-default --size 212992
     * 
     * @param size Buffer size in bytes (default: 212992)
     */
    @ShellMethod(key = "sysctl.core-rmem-default", value = "Set core receive buffer default size")
    @ShellMethodAvailability("availabilityCheck")
    protected void coreRmemDefault(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "212992") int size) {
        String command = String.format("sysctl -w net.core.rmem_default=%d", size);
        scmd(command);
    }

    /**
     * Set core receive buffer maximum size
     * Maximum receive buffer size for all protocols
     * 
     * Example: sysctl.core-rmem-max --size 212992
     * 
     * @param size Buffer size in bytes (default: 212992)
     */
    @ShellMethod(key = "sysctl.core-rmem-max", value = "Set core receive buffer maximum size")
    @ShellMethodAvailability("availabilityCheck")
    protected void coreRmemMax(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "212992") int size) {
        String command = String.format("sysctl -w net.core.rmem_max=%d", size);
        scmd(command);
    }

    /**
     * Set core send buffer default size
     * Default send buffer size for all protocols
     * 
     * Example: sysctl.core-wmem-default --size 212992
     * 
     * @param size Buffer size in bytes (default: 212992)
     */
    @ShellMethod(key = "sysctl.core-wmem-default", value = "Set core send buffer default size")
    @ShellMethodAvailability("availabilityCheck")
    protected void coreWmemDefault(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "212992") int size) {
        String command = String.format("sysctl -w net.core.wmem_default=%d", size);
        scmd(command);
    }

    /**
     * Set core send buffer maximum size
     * Maximum send buffer size for all protocols
     * 
     * Example: sysctl.core-wmem-max --size 212992
     * 
     * @param size Buffer size in bytes (default: 212992)
     */
    @ShellMethod(key = "sysctl.core-wmem-max", value = "Set core send buffer maximum size")
    @ShellMethodAvailability("availabilityCheck")
    protected void coreWmemMax(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "212992") int size) {
        String command = String.format("sysctl -w net.core.wmem_max=%d", size);
        scmd(command);
    }

    /**
     * Set maximum socket backlog
     * Maximum number of packets queued on the input side
     * 
     * Example: sysctl.core-netdev-max-backlog --size 1000
     * 
     * @param size Maximum backlog size (default: 1000)
     */
    @ShellMethod(key = "sysctl.core-netdev-max-backlog", value = "Set maximum socket backlog")
    @ShellMethodAvailability("availabilityCheck")
    protected void coreNetdevMaxBacklog(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "1000") int size) {
        String command = String.format("sysctl -w net.core.netdev_max_backlog=%d", size);
        scmd(command);
    }

    /**
     * Enable or disable ICMP echo ignore
     * Controls whether system responds to ping requests
     * 
     * Example: sysctl.icmp-echo-ignore --enable
     * 
     * @param enable Enable (true) or disable (false) ICMP echo ignore
     */
    @ShellMethod(key = "sysctl.icmp-echo-ignore", value = "Enable or disable ICMP echo ignore")
    @ShellMethodAvailability("availabilityCheck")
    protected void icmpEchoIgnore(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "false") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.icmp_echo_ignore_all=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable ICMP broadcast echo ignore
     * Controls whether system responds to broadcast pings
     * 
     * Example: sysctl.icmp-echo-ignore-broadcasts --enable
     * 
     * @param enable Enable (true) or disable (false) broadcast echo ignore
     */
    @ShellMethod(key = "sysctl.icmp-echo-ignore-broadcasts", value = "Enable or disable ICMP broadcast echo ignore")
    @ShellMethodAvailability("availabilityCheck")
    protected void icmpEchoIgnoreBroadcasts(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.icmp_echo_ignore_broadcasts=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable ICMP redirect acceptance
     * Controls whether system accepts ICMP redirect messages
     * 
     * Example: sysctl.accept-redirects --enable
     * 
     * @param enable Enable (true) or disable (false) ICMP redirects
     */
    @ShellMethod(key = "sysctl.accept-redirects", value = "Enable or disable ICMP redirect acceptance")
    @ShellMethodAvailability("availabilityCheck")
    protected void acceptRedirects(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "false") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.conf.all.accept_redirects=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable sending ICMP redirects
     * Controls whether system sends ICMP redirect messages
     * 
     * Example: sysctl.send-redirects --enable
     * 
     * @param enable Enable (true) or disable (false) sending ICMP redirects
     */
    @ShellMethod(key = "sysctl.send-redirects", value = "Enable or disable sending ICMP redirects")
    @ShellMethodAvailability("availabilityCheck")
    protected void sendRedirects(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "false") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.conf.all.send_redirects=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable source route acceptance
     * Controls whether system accepts source-routed packets
     * 
     * Example: sysctl.accept-source-route --enable
     * 
     * @param enable Enable (true) or disable (false) source route acceptance
     */
    @ShellMethod(key = "sysctl.accept-source-route", value = "Enable or disable source route acceptance")
    @ShellMethodAvailability("availabilityCheck")
    protected void acceptSourceRoute(
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "false") boolean enable) {
        int value = enable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv4.conf.all.accept_source_route=%d", value);
        scmd(command);
    }

    /**
     * Enable or disable IPv6
     * Disables or enables IPv6 on all interfaces
     * 
     * Example: sysctl.ipv6-disable --disable
     * 
     * @param disable Disable (true) or enable (false) IPv6
     */
    @ShellMethod(key = "sysctl.ipv6-disable", value = "Enable or disable IPv6")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipv6Disable(
            @ShellOption(value = { "--disable", "-d" }, defaultValue = "false") boolean disable) {
        int value = disable ? 1 : 0;
        String command = String.format("sysctl -w net.ipv6.conf.all.disable_ipv6=%d", value);
        scmd(command);
    }

    /**
     * Set ARP cache garbage collection thresholds
     * Configure all three GC thresholds at once
     * 
     * Example: sysctl.arp-gc-thresh --thresh1 128 --thresh2 512 --thresh3 1024
     * 
     * @param thresh1 Minimum threshold (default: 128)
     * @param thresh2 Soft maximum threshold (default: 512)
     * @param thresh3 Hard maximum threshold (default: 1024)
     */
    @ShellMethod(key = "sysctl.arp-gc-thresh", value = "Set ARP cache GC thresholds")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpGcThresh(
            @ShellOption(value = { "--thresh1" }, defaultValue = "128") int thresh1,
            @ShellOption(value = { "--thresh2" }, defaultValue = "512") int thresh2,
            @ShellOption(value = { "--thresh3" }, defaultValue = "1024") int thresh3) {
        scmd(String.format("sysctl -w net.ipv4.neigh.default.gc_thresh1=%d", thresh1));
        scmd(String.format("sysctl -w net.ipv4.neigh.default.gc_thresh2=%d", thresh2));
        scmd(String.format("sysctl -w net.ipv4.neigh.default.gc_thresh3=%d", thresh3));
    }

    /**
     * Set swappiness value
     * Controls how aggressively kernel swaps memory pages (0-100)
     * 
     * Example: sysctl.swappiness --value 60
     * 
     * @param value Swappiness value 0-100 (default: 60)
     */
    @ShellMethod(key = "sysctl.swappiness", value = "Set swappiness value")
    @ShellMethodAvailability("availabilityCheck")
    protected void swappiness(
            @ShellOption(value = { "--value", "-v" }, defaultValue = "60") int value) {
        String command = String.format("sysctl -w vm.swappiness=%d", value);
        scmd(command);
    }

    /**
     * Set maximum number of file handles
     * System-wide limit on number of open files
     * 
     * Example: sysctl.file-max --size 65536
     * 
     * @param size Maximum number of file handles (default: 65536)
     */
    @ShellMethod(key = "sysctl.file-max", value = "Set maximum number of file handles")
    @ShellMethodAvailability("availabilityCheck")
    protected void fileMax(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "65536") int size) {
        String command = String.format("sysctl -w fs.file-max=%d", size);
        scmd(command);
    }

    /**
     * Set kernel panic timeout
     * Seconds to wait before rebooting after kernel panic (0=no reboot)
     * 
     * Example: sysctl.panic --seconds 10
     * 
     * @param seconds Panic timeout in seconds (default: 0)
     */
    @ShellMethod(key = "sysctl.panic", value = "Set kernel panic timeout")
    @ShellMethodAvailability("availabilityCheck")
    protected void panic(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "0") int seconds) {
        String command = String.format("sysctl -w kernel.panic=%d", seconds);
        scmd(command);
    }

    /**
     * Set maximum number of PIDs
     * System-wide limit on number of process IDs
     * 
     * Example: sysctl.pid-max --size 32768
     * 
     * @param size Maximum number of PIDs (default: 32768)
     */
    @ShellMethod(key = "sysctl.pid-max", value = "Set maximum number of PIDs")
    @ShellMethodAvailability("availabilityCheck")
    protected void pidMax(
            @ShellOption(value = { "--size", "-s" }, defaultValue = "32768") int size) {
        String command = String.format("sysctl -w kernel.pid_max=%d", size);
        scmd(command);
    }

    /**
     * Set IP local port range
     * Sets the range of local ports available for outgoing connections
     * 
     * Example: sysctl.ip-local-port-range --min 32768 --max 60999
     * 
     * @param min Minimum port number (default: 32768)
     * @param max Maximum port number (default: 60999)
     */
    @ShellMethod(key = "sysctl.ip-local-port-range", value = "Set IP local port range")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLocalPortRange(
            @ShellOption(value = { "--min" }, defaultValue = "32768") int min,
            @ShellOption(value = { "--max" }, defaultValue = "60999") int max) {
        String command = String.format("sysctl -w net.ipv4.ip_local_port_range='%d %d'", min, max);
        scmd(command);
    }

    /**
     * Set TCP congestion control algorithm
     * Controls the congestion control algorithm used
     * 
     * Example: sysctl.tcp-congestion-control --algorithm cubic
     * 
     * @param algorithm Congestion control algorithm (default: cubic)
     */
    @ShellMethod(key = "sysctl.tcp-congestion-control", value = "Set TCP congestion control algorithm")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpCongestionControl(
            @ShellOption(value = { "--algorithm", "-a" }, defaultValue = "cubic") String algorithm) {
        String command = String.format("sysctl -w net.ipv4.tcp_congestion_control=%s", algorithm);
        scmd(command);
    }

    /**
     * Show all sysctl parameters
     * Display all kernel parameters and their current values
     * 
     * Example: sysctl.show-all
     */
    @ShellMethod(key = "sysctl.show-all", value = "Show all sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showAll() {
        scmd("sysctl -a");
    }

    /**
     * Show specific sysctl parameter
     * Display a specific kernel parameter value
     * 
     * Example: sysctl.show --parameter net.ipv4.ip_forward
     * 
     * @param parameter The sysctl parameter name
     */
    @ShellMethod(key = "sysctl.show", value = "Show specific sysctl parameter")
    @ShellMethodAvailability("availabilityCheck")
    protected void show(
            @ShellOption(value = { "--parameter", "-p" }) String parameter) {
        String command = String.format("sysctl %s", parameter);
        scmd(command);
    }

    /**
     * Set custom sysctl parameter
     * Set any sysctl parameter with custom value
     * 
     * Example: sysctl.set --parameter net.ipv4.ip_forward --value 1
     * 
     * @param parameter The sysctl parameter name
     * @param value     The value to set
     */
    @ShellMethod(key = "sysctl.set", value = "Set custom sysctl parameter")
    @ShellMethodAvailability("availabilityCheck")
    protected void set(
            @ShellOption(value = { "--parameter", "-p" }) String parameter,
            @ShellOption(value = { "--value", "-v" }) String value) {
        String command = String.format("sysctl -w %s=%s", parameter, value);
        scmd(command);
    }

    /**
     * Load sysctl settings from file
     * Load kernel parameters from configuration file
     * 
     * Example: sysctl.load --file /etc/sysctl.conf
     * 
     * @param file Path to sysctl configuration file (default: /etc/sysctl.conf)
     */
    @ShellMethod(key = "sysctl.load", value = "Load sysctl settings from file")
    @ShellMethodAvailability("availabilityCheck")
    protected void load(
            @ShellOption(value = { "--file", "-f" }, defaultValue = "/etc/sysctl.conf") String file) {
        String command = String.format("sysctl -p %s", file);
        scmd(command);
    }

    /**
     * Show network-related sysctl parameters
     * Display all network kernel parameters
     * 
     * Example: sysctl.show-network
     */
    @ShellMethod(key = "sysctl.show-network", value = "Show network-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showNetwork() {
        scmd("sysctl -a | grep net");
    }

    /**
     * Show TCP-related sysctl parameters
     * Display all TCP kernel parameters
     * 
     * Example: sysctl.show-tcp
     */
    @ShellMethod(key = "sysctl.show-tcp", value = "Show TCP-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showTcp() {
        scmd("sysctl -a | grep tcp");
    }

    /**
     * Show IPv4-related sysctl parameters
     * Display all IPv4 kernel parameters
     * 
     * Example: sysctl.show-ipv4
     */
    @ShellMethod(key = "sysctl.show-ipv4", value = "Show IPv4-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showIpv4() {
        scmd("sysctl -a | grep ipv4");
    }

    /**
     * Show IPv6-related sysctl parameters
     * Display all IPv6 kernel parameters
     * 
     * Example: sysctl.show-ipv6
     */
    @ShellMethod(key = "sysctl.show-ipv6", value = "Show IPv6-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showIpv6() {
        scmd("sysctl -a | grep ipv6");
    }

    /**
     * Show memory-related sysctl parameters
     * Display all VM/memory kernel parameters
     * 
     * Example: sysctl.show-vm
     */
    @ShellMethod(key = "sysctl.show-vm", value = "Show memory-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showVm() {
        scmd("sysctl -a | grep vm");
    }

    /**
     * Show kernel-related sysctl parameters
     * Display all kernel parameters
     * 
     * Example: sysctl.show-kernel
     */
    @ShellMethod(key = "sysctl.show-kernel", value = "Show kernel-related sysctl parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void showKernel() {
        scmd("sysctl -a | grep kernel");
    }

    /**
     * Execute custom sysctl command
     * Run any sysctl command directly
     * 
     * Example: sysctl.custom "sysctl -w net.ipv4.tcp_tw_reuse=1"
     * 
     * @param command The complete sysctl command to execute
     */
    @ShellMethod(key = "sysctl.custom", value = "Execute custom sysctl command")
    @ShellMethodAvailability("availabilityCheck")
    protected void sysctlCustom(String command) {
        scmd(command);
    }

    /**
     * Display help for sysctl commands
     * Shows all available sysctl commands with examples
     * 
     * Example: sysctl.help
     */
    @ShellMethod(key = "sysctl.help", value = "Display help for sysctl commands")
    protected void sysctlHelp() {
        String help = """

                ╔══════════════════════════════════════════════════════════════════════════════╗
                ║                          SYSCTL COMMANDS HELP                                ║
                ╚══════════════════════════════════════════════════════════════════════════════╝

                NETWORK FILTERING & ROUTING:
                  sysctl.rp-filter --value <0|1|2>
                    Set reverse path filtering for all interfaces (0=off, 1=strict, 2=loose)

                  sysctl.rp-filter-interface --interface <name> --value <0|1|2>
                    Set reverse path filtering for specific interface

                  sysctl.ip-forward --enable <true|false>
                    Enable or disable IP forwarding

                  sysctl.ipv6-forward --enable <true|false>
                    Enable or disable IPv6 forwarding

                TCP KEEPALIVE:
                  sysctl.tcp-keepalive-time --seconds <value>
                    Set TCP keepalive time (default: 7200)

                  sysctl.tcp-keepalive-interval --seconds <value>
                    Set TCP keepalive interval (default: 75)

                  sysctl.tcp-keepalive-probes --count <value>
                    Set TCP keepalive probes count (default: 9)

                TCP CONNECTION MANAGEMENT:
                  sysctl.tcp-syn-retries --count <value>
                    Set maximum number of SYN retries (default: 5)

                  sysctl.tcp-synack-retries --count <value>
                    Set maximum number of SYN-ACK retries (default: 5)

                  sysctl.tcp-fin-timeout --seconds <value>
                    Set TCP FIN timeout (default: 60)

                  sysctl.tcp-max-syn-backlog --size <value>
                    Set maximum SYN backlog (default: 2048)

                  sysctl.tcp-syncookies --enable <true|false>
                    Enable or disable SYN cookies

                TCP PERFORMANCE:
                  sysctl.tcp-timestamps --enable <true|false>
                    Enable or disable TCP timestamps

                  sysctl.tcp-window-scaling --enable <true|false>
                    Enable or disable TCP window scaling

                  sysctl.tcp-sack --enable <true|false>
                    Enable or disable TCP SACK

                  sysctl.tcp-congestion-control --algorithm <name>
                    Set TCP congestion control algorithm (default: cubic)

                BUFFER SIZES:
                  sysctl.tcp-rmem --min <bytes> --default <bytes> --max <bytes>
                    Set TCP receive buffer sizes

                  sysctl.tcp-wmem --min <bytes> --default <bytes> --max <bytes>
                    Set TCP send buffer sizes

                  sysctl.core-rmem-default --size <bytes>
                    Set core receive buffer default size

                  sysctl.core-rmem-max --size <bytes>
                    Set core receive buffer maximum size

                  sysctl.core-wmem-default --size <bytes>
                    Set core send buffer default size

                  sysctl.core-wmem-max --size <bytes>
                    Set core send buffer maximum size

                  sysctl.core-netdev-max-backlog --size <value>
                    Set maximum socket backlog

                ICMP SETTINGS:
                  sysctl.icmp-echo-ignore --enable <true|false>
                    Enable or disable ICMP echo ignore

                  sysctl.icmp-echo-ignore-broadcasts --enable <true|false>
                    Enable or disable ICMP broadcast echo ignore

                  sysctl.accept-redirects --enable <true|false>
                    Enable or disable ICMP redirect acceptance

                  sysctl.send-redirects --enable <true|false>
                    Enable or disable sending ICMP redirects

                  sysctl.accept-source-route --enable <true|false>
                    Enable or disable source route acceptance

                IPV6 SETTINGS:
                  sysctl.ipv6-disable --disable <true|false>
                    Enable or disable IPv6

                ARP CACHE:
                  sysctl.arp-gc-thresh --thresh1 <value> --thresh2 <value> --thresh3 <value>
                    Set ARP cache GC thresholds

                MEMORY MANAGEMENT:
                  sysctl.swappiness --value <0-100>
                    Set swappiness value (default: 60)

                SYSTEM LIMITS:
                  sysctl.file-max --size <value>
                    Set maximum number of file handles

                  sysctl.pid-max --size <value>
                    Set maximum number of PIDs

                KERNEL PANIC:
                  sysctl.panic --seconds <value>
                    Set kernel panic timeout (0=no reboot)

                PORT RANGE:
                  sysctl.ip-local-port-range --min <port> --max <port>
                    Set IP local port range

                QUERY COMMANDS:
                  sysctl.show-all
                    Show all sysctl parameters

                  sysctl.show --parameter <name>
                    Show specific sysctl parameter

                  sysctl.show-network
                    Show network-related parameters

                  sysctl.show-tcp
                    Show TCP-related parameters

                  sysctl.show-ipv4
                    Show IPv4-related parameters

                  sysctl.show-ipv6
                    Show IPv6-related parameters

                  sysctl.show-vm
                    Show memory-related parameters

                  sysctl.show-kernel
                    Show kernel-related parameters

                CUSTOM COMMANDS:
                  sysctl.set --parameter <name> --value <value>
                    Set any custom sysctl parameter

                  sysctl.load --file <path>
                    Load sysctl settings from file

                  sysctl.custom "<command>"
                    Execute custom sysctl command

                EXAMPLES:
                  # Disable reverse path filtering for all interfaces
                  sysctl.rp-filter --value 0

                  # Disable reverse path filtering for eth2
                  sysctl.rp-filter-interface --interface eth2 --value 0

                  # Enable IP forwarding
                  sysctl.ip-forward --enable true

                  # Set TCP keepalive time to 2 hours
                  sysctl.tcp-keepalive-time --seconds 7200

                  # Show current IP forwarding status
                  sysctl.show --parameter net.ipv4.ip_forward

                  # Load settings from custom file
                  sysctl.load --file /etc/sysctl.d/99-custom.conf

                ╚══════════════════════════════════════════════════════════════════════════════╝
                """;
        System.out.println(help);
    }
}

// Made with Bob
