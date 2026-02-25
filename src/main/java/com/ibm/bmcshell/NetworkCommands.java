package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class NetworkCommands extends CommonCommands {
    protected NetworkCommands() throws IOException {
    }

    /**
     * Send gratuitous ARP to update other devices' ARP cache
     * Updates network about your MAC address for an IP
     * 
     * Example: net.arp-announce --ip 192.168.1.100 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "net.arp-announce", value = "Send gratuitous ARP to update other devices' ARP cache")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpAnnounce(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "3") int count) {
        String command = String.format("arping -U -c %d -I %s %s", count, interfaceName, ipAddress);
        scmd(command);
    }

    /**
     * Refresh local ARP cache by probing a device
     * Sends ARP requests and updates your local cache
     * 
     * Example: net.arp-probe --ip 192.168.1.1 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to probe
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "net.arp-probe", value = "Refresh local ARP cache by probing a device")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpProbe(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "3") int count) {
        String command = String.format("arping -c %d -I %s %s", count, interfaceName, ipAddress);
        scmd(command);
    }

    /**
     * Update ARP cache after IP/MAC change
     * Announces to network using ARP REPLY instead of REQUEST
     * 
     * Example: net.arp-update --ip 192.168.1.100 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "net.arp-update", value = "Update ARP cache after IP/MAC change using ARP REPLY")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpUpdate(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "3") int count) {
        String command = String.format("arping -A -c %d -I %s %s", count, interfaceName, ipAddress);
        scmd(command);
    }

    /**
     * Detect duplicate IP before using
     * Returns 0 if no duplicate found
     * 
     * Example: net.arp-detect-duplicate --ip 192.168.1.100 --interface eth2 --count 2
     * 
     * @param ipAddress The IP address to check
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 2)
     */
    @ShellMethod(key = "net.arp-detect-duplicate", value = "Detect duplicate IP and refresh")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpDetectDuplicate(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "2") int count) {
        String command = String.format("arping -D -c %d -I %s %s", count, interfaceName, ipAddress);
        scmd(command);
    }

    /**
     * BMC network reconfiguration - announce current IP
     * Automatically detects the IP address of the specified interface
     * 
     * Example: net.bmc-reconfig --interface eth2 --count 5
     * 
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 5)
     */
    @ShellMethod(key = "net.bmc-reconfig", value = "Announce BMC network reconfiguration")
    @ShellMethodAvailability("availabilityCheck")
    protected void bmcReconfig(
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "5") int count) {
        String command = String.format(
                "arping -U -c %d -I %s $(ip -4 addr show %s | grep -oP '(?<=inet\\s)\\d+(\\.\\d+){3}')",
                count, interfaceName, interfaceName);
        scmd(command);
    }

    /**
     * Refresh MAC periodically - inline script execution
     * Executes commands to refresh MAC address periodically
     * 
     * Example: net.refresh-mac --interface eth2 --count 3
     * 
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "net.refresh-mac", value = "Refresh MAC address on the network")
    @ShellMethodAvailability("availabilityCheck")
    protected void refreshMac(
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "3") int count) {
        String command = String.format(
                "INTERFACE=\"%s\"; IP_ADDR=$(ip -4 addr show $INTERFACE | grep -oP '(?<=inet\\s)\\d+(\\.\\d+){3}'); arping -U -c %d -I $INTERFACE $IP_ADDR",
                interfaceName, count);
        scmd(command);
    }

    /**
     * Display current ARP cache (BusyBox compatible)
     * Uses ip neigh show or cat /proc/net/arp
     *
     * Example: net.arp-show
     */
    @ShellMethod(key = "net.arp-show", value = "Display current ARP cache")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShow() {
        scmd("ip neigh show || cat /proc/net/arp");
    }

    /**
     * Display ARP cache from /proc (BusyBox compatible)
     *
     * Example: net.arp-show-proc
     */
    @ShellMethod(key = "net.arp-show-proc", value = "Display ARP cache from /proc/net/arp")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShowProc() {
        scmd("cat /proc/net/arp");
    }

    /**
     * Clear ARP cache entry for a specific IP (BusyBox compatible)
     * Uses ip neigh del
     *
     * Example: net.arp-clear --ip 192.168.1.100 --device eth0
     *
     * @param ipAddress The IP address to remove from ARP cache
     * @param device Network interface name
     */
    @ShellMethod(key = "net.arp-clear", value = "Clear ARP cache entry for a specific IP")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpClear(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip neigh del %s dev %s", ipAddress, device);
        scmd(command);
    }

    /**
     * Show network interface information
     * 
     * Example: net.ifconfig --interface eth2
     * 
     * @param interfaceName Network interface (optional, shows all if not specified)
     */
    @ShellMethod(key = "net.ifconfig", value = "Show network interface information")
    @ShellMethodAvailability("availabilityCheck")
    protected void ifconfig(
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "") String interfaceName) {
        String command = interfaceName.isEmpty() ? "ip addr show" : String.format("ip addr show %s", interfaceName);
        scmd(command);
    }

    /**
     * Execute custom arping command with full control
     * 
     * Example: net.arping-custom "arping -U -c 5 -I eth2 192.168.1.100"
     * 
     * @param arpingCommand The complete arping command to execute
     */
    @ShellMethod(key = "net.arping-custom", value = "Execute custom arping command")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpingCustom(String arpingCommand) {
        scmd(arpingCommand);
    }

    /**
     * Show routing table
     * 
     * Example: net.route
     */
    @ShellMethod(key = "net.route", value = "Show routing table")
    @ShellMethodAvailability("availabilityCheck")
    protected void route() {
        scmd("ip route show");
    }

    /**
     * Ping a host
     *
     * Example: net.ping --host 192.168.1.1 --count 4
     * Example: net.ping 192.168.1.1
     *
     * @param host The host to ping
     * @param count Number of ping packets (default: 4)
     */
    @ShellMethod(key = "net.ping", value = "Ping a host")
    @ShellMethodAvailability("availabilityCheck")
    protected void ping(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "4") int count) {
        String command = String.format("ping -c %d %s", count, host);
        scmd(command);
    }

    // ==================== IP TOOL COMMANDS ====================

    /**
     * Show all network interfaces with detailed information
     *
     * Example: net.ip-addr-show
     */
    @ShellMethod(key = "net.ip-addr-show", value = "Show all network interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrShow() {
        scmd("ip addr show");
    }

    /**
     * Show specific network interface details
     *
     * Example: net.ip-addr-show-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-addr-show-dev", value = "Show specific network interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrShowDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr show dev %s", device);
        scmd(command);
    }

    /**
     * Add IP address to an interface
     *
     * Example: net.ip-addr-add --ip 192.168.1.100/24 --device eth0
     *
     * @param ipWithMask IP address with CIDR notation (e.g., 192.168.1.100/24)
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-addr-add", value = "Add IP address to interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrAdd(
            @ShellOption(value = { "--ip", "-i" }) String ipWithMask,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr add %s dev %s", ipWithMask, device);
        scmd(command);
    }

    /**
     * Delete IP address from an interface
     *
     * Example: net.ip-addr-del --ip 192.168.1.100/24 --device eth0
     *
     * @param ipWithMask IP address with CIDR notation
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-addr-del", value = "Delete IP address from interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrDel(
            @ShellOption(value = { "--ip", "-i" }) String ipWithMask,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr del %s dev %s", ipWithMask, device);
        scmd(command);
    }

    /**
     * Flush all IP addresses from an interface
     *
     * Example: net.ip-addr-flush --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-addr-flush", value = "Flush all IP addresses from interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrFlush(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr flush dev %s", device);
        scmd(command);
    }

    /**
     * Show all network links (interfaces)
     *
     * Example: net.ip-link-show
     */
    @ShellMethod(key = "net.ip-link-show", value = "Show all network links")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkShow() {
        scmd("ip link show");
    }

    /**
     * Set network interface up
     *
     * Example: net.ip-link-up --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-link-up", value = "Bring network interface up")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkUp(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip link set %s up", device);
        scmd(command);
    }

    /**
     * Set network interface down
     *
     * Example: net.ip-link-down --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-link-down", value = "Bring network interface down")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkDown(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip link set %s down", device);
        scmd(command);
    }

    /**
     * Set MAC address for an interface
     *
     * Example: net.ip-link-set-mac --device eth0 --mac 00:11:22:33:44:55
     *
     * @param device Network interface name
     * @param macAddress New MAC address
     */
    @ShellMethod(key = "net.ip-link-set-mac", value = "Set MAC address for interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkSetMac(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--mac", "-m" }) String macAddress) {
        String command = String.format("ip link set %s address %s", device, macAddress);
        scmd(command);
    }

    /**
     * Set MTU for an interface
     *
     * Example: net.ip-link-set-mtu --device eth0 --mtu 1500
     *
     * @param device Network interface name
     * @param mtu MTU value
     */
    @ShellMethod(key = "net.ip-link-set-mtu", value = "Set MTU for interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkSetMtu(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--mtu", "-m" }) int mtu) {
        String command = String.format("ip link set %s mtu %d", device, mtu);
        scmd(command);
    }

    /**
     * Show routing table
     *
     * Example: net.ip-route-show
     */
    @ShellMethod(key = "net.ip-route-show", value = "Show routing table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteShow() {
        scmd("ip route show");
    }

    /**
     * Add a route
     *
     * Example: net.ip-route-add --destination 192.168.2.0/24 --via 192.168.1.1
     *
     * @param destination Destination network in CIDR notation
     * @param via Gateway IP address
     */
    @ShellMethod(key = "net.ip-route-add", value = "Add a route")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteAdd(
            @ShellOption(value = { "--destination", "-dest" }) String destination,
            @ShellOption(value = { "--via", "-v" }) String via) {
        String command = String.format("ip route add %s via %s", destination, via);
        scmd(command);
    }

    /**
     * Add a route with specific device
     *
     * Example: net.ip-route-add-dev --destination 192.168.2.0/24 --via 192.168.1.1 --device eth0
     *
     * @param destination Destination network in CIDR notation
     * @param via Gateway IP address
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-route-add-dev", value = "Add a route with specific device")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteAddDev(
            @ShellOption(value = { "--destination", "-dest" }) String destination,
            @ShellOption(value = { "--via", "-v" }) String via,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip route add %s via %s dev %s", destination, via, device);
        scmd(command);
    }

    /**
     * Delete a route
     *
     * Example: net.ip-route-del --destination 192.168.2.0/24
     *
     * @param destination Destination network in CIDR notation
     */
    @ShellMethod(key = "net.ip-route-del", value = "Delete a route")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteDel(
            @ShellOption(value = { "--destination", "-dest" }) String destination) {
        String command = String.format("ip route del %s", destination);
        scmd(command);
    }

    /**
     * Add default gateway
     *
     * Example: net.ip-route-default --gateway 192.168.1.1
     *
     * @param gateway Gateway IP address
     */
    @ShellMethod(key = "net.ip-route-default", value = "Add default gateway")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteDefault(
            @ShellOption(value = { "--gateway", "-g" }) String gateway) {
        String command = String.format("ip route add default via %s", gateway);
        scmd(command);
    }

    /**
     * Show neighbor (ARP) table
     *
     * Example: net.ip-neigh-show
     */
    @ShellMethod(key = "net.ip-neigh-show", value = "Show neighbor (ARP) table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighShow() {
        scmd("ip neigh show");
    }

    /**
     * Add static ARP entry
     *
     * Example: net.ip-neigh-add --ip 192.168.1.100 --mac 00:11:22:33:44:55 --device eth0
     *
     * @param ipAddress IP address
     * @param macAddress MAC address
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-neigh-add", value = "Add static ARP entry")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighAdd(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--mac", "-m" }) String macAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip neigh add %s lladdr %s dev %s", ipAddress, macAddress, device);
        scmd(command);
    }

    /**
     * Delete ARP entry
     *
     * Example: net.ip-neigh-del --ip 192.168.1.100 --device eth0
     *
     * @param ipAddress IP address
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-neigh-del", value = "Delete ARP entry")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighDel(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip neigh del %s dev %s", ipAddress, device);
        scmd(command);
    }

    /**
     * Flush neighbor (ARP) table
     *
     * Example: net.ip-neigh-flush --device eth0
     *
     * @param device Network interface name (optional, flushes all if not specified)
     */
    @ShellMethod(key = "net.ip-neigh-flush", value = "Flush neighbor (ARP) table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighFlush(
            @ShellOption(value = { "--device", "-d" }, defaultValue = "") String device) {
        String command = device.isEmpty() ? "ip neigh flush all" : String.format("ip neigh flush dev %s", device);
        scmd(command);
    }

    /**
     * Set ARP cache timeout for a specific interface
     * Reduces the time ARP entries are cached before being refreshed
     *
     * Example: net.arp-set-timeout --device eth2 --timeout 1000
     *
     * @param device Network interface name
     * @param timeoutMs Base reachable time in milliseconds (default: 1000ms = 1 second)
     */
    @ShellMethod(key = "net.arp-set-timeout", value = "Set ARP cache timeout for interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetTimeout(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "1000") int timeoutMs) {
        String command = String.format("sysctl -w net.ipv4.neigh.%s.base_reachable_time_ms=%d", device, timeoutMs);
        scmd(command);
    }

    /**
     * Set ARP garbage collection stale time for a specific interface
     * Controls how long stale entries remain before being removed
     *
     * Example: net.arp-set-gc-stale --device eth2 --seconds 1
     *
     * @param device Network interface name
     * @param seconds Garbage collection stale time in seconds (default: 1)
     */
    @ShellMethod(key = "net.arp-set-gc-stale", value = "Set ARP garbage collection stale time")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetGcStale(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "1") int seconds) {
        String command = String.format("sysctl -w net.ipv4.neigh.%s.gc_stale_time=%d", device, seconds);
        scmd(command);
    }

    /**
     * Set minimal ARP caching for a specific interface
     * Combines timeout and gc_stale settings for aggressive cache refresh
     *
     * Example: net.arp-minimal-cache --device eth2
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.arp-minimal-cache", value = "Set minimal ARP caching for interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpMinimalCache(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format(
                "sysctl -w net.ipv4.neigh.%s.base_reachable_time_ms=1000 && " +
                "sysctl -w net.ipv4.neigh.%s.gc_stale_time=1",
                device, device);
        scmd(command);
    }

    /**
     * Set global default ARP cache timeout
     * Applies to all interfaces by default
     *
     * Example: net.arp-set-default-timeout --timeout 1000
     *
     * @param timeoutMs Base reachable time in milliseconds (default: 1000ms)
     */
    @ShellMethod(key = "net.arp-set-default-timeout", value = "Set global default ARP cache timeout")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetDefaultTimeout(
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "1000") int timeoutMs) {
        String command = String.format("sysctl -w net.ipv4.neigh.default.base_reachable_time_ms=%d", timeoutMs);
        scmd(command);
    }

    /**
     * Set global default ARP garbage collection stale time
     *
     * Example: net.arp-set-default-gc-stale --seconds 1
     *
     * @param seconds Garbage collection stale time in seconds (default: 1)
     */
    @ShellMethod(key = "net.arp-set-default-gc-stale", value = "Set global default ARP GC stale time")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetDefaultGcStale(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "1") int seconds) {
        String command = String.format("sysctl -w net.ipv4.neigh.default.gc_stale_time=%d", seconds);
        scmd(command);
    }

    /**
     * Set minimal ARP caching globally for all interfaces
     *
     * Example: net.arp-minimal-cache-global
     */
    @ShellMethod(key = "net.arp-minimal-cache-global", value = "Set minimal ARP caching globally")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpMinimalCacheGlobal() {
        String command = "sysctl -w net.ipv4.neigh.default.base_reachable_time_ms=1000 && " +
                        "sysctl -w net.ipv4.neigh.default.gc_stale_time=1";
        scmd(command);
    }

    /**
     * Show current ARP cache settings for a specific interface
     *
     * Example: net.arp-show-settings --device eth2
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.arp-show-settings", value = "Show ARP cache settings for interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShowSettings(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format(
                "echo 'ARP Settings for %s:' && " +
                "sysctl net.ipv4.neigh.%s.base_reachable_time_ms && " +
                "sysctl net.ipv4.neigh.%s.gc_stale_time",
                device, device, device);
        scmd(command);
    }

    /**
     * Show global default ARP cache settings
     *
     * Example: net.arp-show-default-settings
     */
    @ShellMethod(key = "net.arp-show-default-settings", value = "Show global default ARP cache settings")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShowDefaultSettings() {
        String command = "echo 'Global Default ARP Settings:' && " +
                        "sysctl net.ipv4.neigh.default.base_reachable_time_ms && " +
                        "sysctl net.ipv4.neigh.default.gc_stale_time";
        scmd(command);
    }

    /**
     * Clear and refresh ARP entry for a specific IP
     * Deletes the entry and immediately probes to refresh
     *
     * Example: net.arp-refresh --ip 9.6.28.101 --device eth2
     *
     * @param ipAddress IP address to refresh
     * @param device Network interface name
     */
    @ShellMethod(key = "net.arp-refresh", value = "Clear and refresh ARP entry")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpRefresh(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format(
                "ip neigh del %s dev %s 2>/dev/null; ping -c 1 -W 1 %s",
                ipAddress, device, ipAddress);
        scmd(command);
    }

    /**
     * Show network statistics (BusyBox compatible)
     * Uses /proc/net/dev for statistics
     *
     * Example: net.ip-stats
     */
    @ShellMethod(key = "net.ip-stats", value = "Show network statistics")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipStats() {
        scmd("cat /proc/net/dev");
    }

    /**
     * Show detailed network statistics for specific interface (BusyBox compatible)
     *
     * Example: net.ip-stats-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "net.ip-stats-dev", value = "Show network statistics for specific interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipStatsDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("cat /proc/net/dev | grep -E '(%s|Inter|face)'", device);
        scmd(command);
    }

    /**
     * Show interface statistics using ifconfig (BusyBox compatible)
     *
     * Example: net.ifconfig-stats --device eth0
     *
     * @param device Network interface name (optional)
     */
    @ShellMethod(key = "net.ifconfig-stats", value = "Show interface statistics using ifconfig")
    @ShellMethodAvailability("availabilityCheck")
    protected void ifconfigStats(
            @ShellOption(value = { "--device", "-d" }, defaultValue = "") String device) {
        String command = device.isEmpty() ? "ifconfig" : String.format("ifconfig %s", device);
        scmd(command);
    }

    /**
     * Show network connections and sockets
     *
     * Example: net.netstat
     */
    @ShellMethod(key = "net.netstat", value = "Show network connections and sockets")
    @ShellMethodAvailability("availabilityCheck")
    protected void netstat() {
        scmd("netstat -tuln");
    }

    /**
     * Show all network connections
     *
     * Example: net.netstat-all
     */
    @ShellMethod(key = "net.netstat-all", value = "Show all network connections")
    @ShellMethodAvailability("availabilityCheck")
    protected void netstatAll() {
        scmd("netstat -a");
    }

    /**
     * Execute custom ip command
     *
     * Example: net.ip-custom "ip addr show dev eth0"
     *
     * @param ipCommand The complete ip command to execute
     */
    @ShellMethod(key = "net.ip-custom", value = "Execute custom ip command")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipCustom(String ipCommand) {
        scmd(ipCommand);
    }
}

// Made with Bob
