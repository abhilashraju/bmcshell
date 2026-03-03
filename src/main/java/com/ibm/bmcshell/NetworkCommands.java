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
     * Example: arp.announce --ip 192.168.1.100 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "arp.announce", value = "Send gratuitous ARP to update other devices' ARP cache")
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
     * Example: arp.probe --ip 192.168.1.1 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to probe
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "arp.probe", value = "Refresh local ARP cache by probing a device")
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
     * Example: arp.update --ip 192.168.1.100 --interface eth2 --count 3
     * 
     * @param ipAddress The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "arp.update", value = "Update ARP cache after IP/MAC change using ARP REPLY")
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
     * Example: arp.detect-duplicate --ip 192.168.1.100 --interface eth2 --count 2
     * 
     * @param ipAddress The IP address to check
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 2)
     */
    @ShellMethod(key = "arp.detect-duplicate", value = "Detect duplicate IP and refresh")
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
     * Example: bmc-reconfig --interface eth2 --count 5
     * 
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 5)
     */
    @ShellMethod(key = "bmc-reconfig", value = "Announce BMC network reconfiguration")
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
     * Example: refresh-mac --interface eth2 --count 3
     * 
     * @param interfaceName Network interface (default: eth2)
     * @param count Number of packets to send (default: 3)
     */
    @ShellMethod(key = "refresh-mac", value = "Refresh MAC address on the network")
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
     * Example: arp.show
     */
    @ShellMethod(key = "arp.show", value = "Display current ARP cache")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShow() {
        scmd("ip neigh show || cat /proc/net/arp");
    }

    /**
     * Display ARP cache from /proc (BusyBox compatible)
     *
     * Example: arp.show-proc
     */
    @ShellMethod(key = "arp.show-proc", value = "Display ARP cache from /proc/net/arp")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpShowProc() {
        scmd("cat /proc/net/arp");
    }

    /**
     * Clear ARP cache entry for a specific IP (BusyBox compatible)
     * Uses ip neigh del
     *
     * Example: arp.clear --ip 192.168.1.100 --device eth0
     *
     * @param ipAddress The IP address to remove from ARP cache
     * @param device Network interface name
     */
    @ShellMethod(key = "arp.clear", value = "Clear ARP cache entry for a specific IP")
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
     * Example: ifconfig --interface eth2
     * 
     * @param interfaceName Network interface (optional, shows all if not specified)
     */
    @ShellMethod(key = "ifconfig", value = "Show network interface information")
    @ShellMethodAvailability("availabilityCheck")
    protected void ifconfig(
            @ShellOption(value = { "--interface", "-I" }, defaultValue = "") String interfaceName) {
        String command = interfaceName.isEmpty() ? "ip addr show" : String.format("ip addr show %s", interfaceName);
        scmd(command);
    }

    /**
     * Execute custom arping command with full control
     * 
     * Example: arping-custom "arping -U -c 5 -I eth2 192.168.1.100"
     * 
     * @param arpingCommand The complete arping command to execute
     */
    @ShellMethod(key = "arping-custom", value = "Execute custom arping command")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpingCustom(String arpingCommand) {
        scmd(arpingCommand);
    }

    /**
     * Show routing table
     * 
     * Example: route
     */
    @ShellMethod(key = "route", value = "Show routing table")
    @ShellMethodAvailability("availabilityCheck")
    protected void route() {
        scmd("ip route show");
    }

    /**
     * Ping a host
     *
     * Example: ping --host 192.168.1.1 --count 4
     * Example: ping 192.168.1.1
     *
     * @param host The host to ping
     * @param count Number of ping packets (default: 4)
     */
    @ShellMethod(key = "ping", value = "Ping a host")
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
     * Example: ip.addr-show
     */
    @ShellMethod(key = "ip.addr-show", value = "Show all network interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrShow() {
        scmd("ip addr show");
    }

    /**
     * Show specific network interface details
     *
     * Example: ip.addr-show-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.addr-show-dev", value = "Show specific network interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrShowDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr show dev %s", device);
        scmd(command);
    }

    /**
     * Add IP address to an interface
     *
     * Example: ip.addr-add --ip 192.168.1.100/24 --device eth0
     *
     * @param ipWithMask IP address with CIDR notation (e.g., 192.168.1.100/24)
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.addr-add", value = "Add IP address to interface")
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
     * Example: ip.addr-del --ip 192.168.1.100/24 --device eth0
     *
     * @param ipWithMask IP address with CIDR notation
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.addr-del", value = "Delete IP address from interface")
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
     * Example: ip.addr-flush --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.addr-flush", value = "Flush all IP addresses from interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipAddrFlush(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip addr flush dev %s", device);
        scmd(command);
    }

    /**
     * Show all network links (interfaces)
     *
     * Example: ip.link-show
     */
    @ShellMethod(key = "ip.link-show", value = "Show all network links")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkShow() {
        scmd("ip link show");
    }

    /**
     * Set network interface up
     *
     * Example: ip.link-up --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.link-up", value = "Bring network interface up")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkUp(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip link set %s up", device);
        scmd(command);
    }

    /**
     * Set network interface down
     *
     * Example: ip.link-down --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.link-down", value = "Bring network interface down")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipLinkDown(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("ip link set %s down", device);
        scmd(command);
    }

    /**
     * Set MAC address for an interface
     *
     * Example: ip.link-set-mac --device eth0 --mac 00:11:22:33:44:55
     *
     * @param device Network interface name
     * @param macAddress New MAC address
     */
    @ShellMethod(key = "ip.link-set-mac", value = "Set MAC address for interface")
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
     * Example: ip.link-set-mtu --device eth0 --mtu 1500
     *
     * @param device Network interface name
     * @param mtu MTU value
     */
    @ShellMethod(key = "ip.link-set-mtu", value = "Set MTU for interface")
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
     * Example: ip.route-show
     */
    @ShellMethod(key = "ip.route-show", value = "Show routing table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteShow() {
        scmd("ip route show");
    }

    /**
     * Add a route
     *
     * Example: ip.route-add --destination 192.168.2.0/24 --via 192.168.1.1
     *
     * @param destination Destination network in CIDR notation
     * @param via Gateway IP address
     */
    @ShellMethod(key = "ip.route-add", value = "Add a route")
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
     * Example: ip.route-add-dev --destination 192.168.2.0/24 --via 192.168.1.1 --device eth0
     *
     * @param destination Destination network in CIDR notation
     * @param via Gateway IP address
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.route-add-dev", value = "Add a route with specific device")
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
     * Example: ip.route-del --destination 192.168.2.0/24
     *
     * @param destination Destination network in CIDR notation
     */
    @ShellMethod(key = "ip.route-del", value = "Delete a route")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteDel(
            @ShellOption(value = { "--destination", "-dest" }) String destination) {
        String command = String.format("ip route del %s", destination);
        scmd(command);
    }

    /**
     * Add default gateway
     *
     * Example: ip.route-default --gateway 192.168.1.1
     *
     * @param gateway Gateway IP address
     */
    @ShellMethod(key = "ip.route-default", value = "Add default gateway")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipRouteDefault(
            @ShellOption(value = { "--gateway", "-g" }) String gateway) {
        String command = String.format("ip route add default via %s", gateway);
        scmd(command);
    }

    /**
     * Show neighbor (ARP) table (BusyBox compatible)
     * Reads directly from /proc/net/arp
     *
     * Example: ip.neigh-show
     */
    @ShellMethod(key = "ip.neigh-show", value = "Show neighbor (ARP) table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighShow() {
        // BusyBox minimal: read directly from /proc/net/arp
        scmd("cat /proc/net/arp");
    }

    /**
     * Add static ARP entry (BusyBox compatible)
     * Note: Without arp or ip neigh commands, static ARP entries cannot be added
     * This will attempt arping to populate the cache naturally
     *
     * Example: ip.neigh-add --ip 192.168.1.100 --mac 00:11:22:33:44:55 --device eth0
     *
     * @param ipAddress IP address
     * @param macAddress MAC address
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.neigh-add", value = "Add static ARP entry")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighAdd(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--mac", "-m" }) String macAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        // BusyBox minimal: Cannot add static ARP without arp/ip commands
        // Use arping if available to populate cache, otherwise ping
        String command = String.format(
            "if command -v arping >/dev/null 2>&1; then " +
            "arping -c 1 -I %s %s; " +
            "else " +
            "ping -c 1 -W 1 -I %s %s; " +
            "fi; " +
            "echo 'Note: Static ARP entry requires arp or ip neigh command. Entry will be dynamic.'",
            device, ipAddress, device, ipAddress);
        scmd(command);
    }

    /**
     * Delete ARP entry (BusyBox compatible)
     * Uses interface down/up cycle to clear ARP cache for that interface
     * Or forces ARP refresh by bringing interface down briefly
     *
     * Example: ip.neigh-del --ip 192.168.1.100 --device eth0
     *
     * @param ipAddress IP address
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.neigh-del", value = "Delete ARP entry")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighDel(
            @ShellOption(value = { "--ip", "-i" }) String ipAddress,
            @ShellOption(value = { "--device", "-d" }) String device) {
        // BusyBox ip doesn't support 'neigh' subcommand
        // Workaround: Cycle interface to clear ARP cache, then ping to refresh
        String command = String.format(
            "ip link set %s down && sleep 0.1 && ip link set %s up && sleep 0.5 && " +
            "ping -c 1 -W 1 -I %s %s >/dev/null 2>&1; " +
            "echo 'Interface %s cycled and ARP entry for %s refreshed'",
            device, device, device, ipAddress, device, ipAddress);
        scmd(command);
    }

    /**
     * Flush neighbor (ARP) table (BusyBox compatible)
     * Forces ARP cache refresh by pinging all entries
     *
     * Example: ip.neigh-flush --device eth0
     *
     * @param device Network interface name (optional, flushes all if not specified)
     */
    @ShellMethod(key = "ip.neigh-flush", value = "Flush neighbor (ARP) table")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipNeighFlush(
            @ShellOption(value = { "--device", "-d" }, defaultValue = "") String device) {
        // BusyBox minimal: Cannot delete ARP entries without arp/ip commands
        // Force refresh by pinging all entries with short timeout
        String command;
        if (device.isEmpty()) {
            command = "awk 'NR>1 {print $1, $6}' /proc/net/arp | " +
                     "while read ip dev; do ping -c 1 -W 1 -I \"$dev\" \"$ip\" >/dev/null 2>&1 & done; " +
                     "wait; echo 'ARP cache refreshed for all devices'";
        } else {
            command = String.format(
                "awk 'NR>1 && $6==\"%s\" {print $1}' /proc/net/arp | " +
                "while read ip; do ping -c 1 -W 1 -I \"%s\" \"$ip\" >/dev/null 2>&1 & done; " +
                "wait; echo 'ARP cache refreshed for %s'",
                device, device, device);
        }
        scmd(command);
    }

    /**
     * Set ARP cache timeout for a specific interface
     * Reduces the time ARP entries are cached before being refreshed
     *
     * Example: arp.set-timeout --device eth2 --timeout 1000
     *
     * @param device Network interface name
     * @param timeoutMs Base reachable time in milliseconds (default: 1000ms = 1 second)
     */
    @ShellMethod(key = "arp.set-timeout", value = "Set ARP cache timeout for interface")
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
     * Example: arp.set-gc-stale --device eth2 --seconds 1
     *
     * @param device Network interface name
     * @param seconds Garbage collection stale time in seconds (default: 1)
     */
    @ShellMethod(key = "arp.set-gc-stale", value = "Set ARP garbage collection stale time")
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
     * Example: arp.minimal-cache --device eth2
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "arp.minimal-cache", value = "Set minimal ARP caching for interface")
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
     * Example: arp.set-default-timeout --timeout 1000
     *
     * @param timeoutMs Base reachable time in milliseconds (default: 1000ms)
     */
    @ShellMethod(key = "arp.set-default-timeout", value = "Set global default ARP cache timeout")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetDefaultTimeout(
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "1000") int timeoutMs) {
        String command = String.format("sysctl -w net.ipv4.neigh.default.base_reachable_time_ms=%d", timeoutMs);
        scmd(command);
    }

    /**
     * Set global default ARP garbage collection stale time
     *
     * Example: arp.set-default-gc-stale --seconds 1
     *
     * @param seconds Garbage collection stale time in seconds (default: 1)
     */
    @ShellMethod(key = "arp.set-default-gc-stale", value = "Set global default ARP GC stale time")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpSetDefaultGcStale(
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "1") int seconds) {
        String command = String.format("sysctl -w net.ipv4.neigh.default.gc_stale_time=%d", seconds);
        scmd(command);
    }

    /**
     * Set minimal ARP caching globally for all interfaces
     *
     * Example: arp.minimal-cache-global
     */
    @ShellMethod(key = "arp.minimal-cache-global", value = "Set minimal ARP caching globally")
    @ShellMethodAvailability("availabilityCheck")
    protected void arpMinimalCacheGlobal() {
        String command = "sysctl -w net.ipv4.neigh.default.base_reachable_time_ms=1000 && " +
                        "sysctl -w net.ipv4.neigh.default.gc_stale_time=1";
        scmd(command);
    }

    /**
     * Show current ARP cache settings for a specific interface
     *
     * Example: arp.show-settings --device eth2
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "arp.show-settings", value = "Show ARP cache settings for interface")
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
     * Example: arp.show-default-settings
     */
    @ShellMethod(key = "arp.show-default-settings", value = "Show global default ARP cache settings")
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
     * Example: arp.refresh --ip 9.6.28.101 --device eth2
     *
     * @param ipAddress IP address to refresh
     * @param device Network interface name
     */
    @ShellMethod(key = "arp.refresh", value = "Clear and refresh ARP entry")
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
     * Example: ip.stats
     */
    @ShellMethod(key = "ip.stats", value = "Show network statistics")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipStats() {
        scmd("cat /proc/net/dev");
    }

    /**
     * Show detailed network statistics for specific interface (BusyBox compatible)
     *
     * Example: ip.stats-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "ip.stats-dev", value = "Show network statistics for specific interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipStatsDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("cat /proc/net/dev | grep -E '(%s|Inter|face)'", device);
        scmd(command);
    }

    /**
     * Show interface statistics using ifconfig (BusyBox compatible)
     *
     * Example: ifconfig-stats --device eth0
     *
     * @param device Network interface name (optional)
     */
    @ShellMethod(key = "ifconfig-stats", value = "Show interface statistics using ifconfig")
    @ShellMethodAvailability("availabilityCheck")
    protected void ifconfigStats(
            @ShellOption(value = { "--device", "-d" }, defaultValue = "") String device) {
        String command = device.isEmpty() ? "ifconfig" : String.format("ifconfig %s", device);
        scmd(command);
    }

    /**
     * Show network connections and sockets
     *
     * Example: netstat
     */
    @ShellMethod(key = "netstat", value = "Show network connections and sockets")
    @ShellMethodAvailability("availabilityCheck")
    protected void netstat() {
        scmd("netstat -tuln");
    }

    /**
     * Show all network connections
     *
     * Example: netstat-all
     */
    @ShellMethod(key = "netstat-all", value = "Show all network connections")
    @ShellMethodAvailability("availabilityCheck")
    protected void netstatAll() {
        scmd("netstat -a");
    }

    /**
     * Execute custom ip command
     *
     * Example: ip.custom "ip addr show dev eth0"
     *
     * @param ipCommand The complete ip command to execute
     */
    @ShellMethod(key = "ip.custom", value = "Execute custom ip command")
    @ShellMethodAvailability("availabilityCheck")
    protected void ipCustom(String ipCommand) {
        scmd(ipCommand);
    }

    // ==================== NC (NETCAT) COMMANDS - BusyBox Compatible ====================

    /**
     * Test if a TCP port is open on a remote host (BusyBox nc)
     * BusyBox nc uses -z for zero-I/O scan mode
     *
     * Example: nc.port-check --host 192.168.1.1 --port 22
     *
     * @param host    Remote host IP or hostname
     * @param port    Port number to check
     * @param timeout Timeout in seconds (default: 3)
     */
    @ShellMethod(key = "nc.port-check", value = "Test if a TCP port is open on a remote host")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncPortCheck(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "3") int timeout) {
        // BusyBox nc: nc -z -w <timeout> <host> <port>
        String command = String.format("nc -z -w %d %s %d && echo 'Port %d is OPEN' || echo 'Port %d is CLOSED'",
                timeout, host, port, port, port);
        scmd(command);
    }

    /**
     * Connect to a remote TCP host and send data (BusyBox nc)
     * Useful for testing services like HTTP, SMTP, Redis
     *
     * Example: nc.connect --host 192.168.1.1 --port 80
     *
     * @param host    Remote host IP or hostname
     * @param port    Port number to connect to
     * @param timeout Timeout in seconds (default: 5)
     */
    @ShellMethod(key = "nc.connect", value = "Connect to a remote TCP host (interactive)")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncConnect(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "5") int timeout) {
        // BusyBox nc: nc -w <timeout> <host> <port>
        String command = String.format("nc -w %d %s %d", timeout, host, port);
        scmd(command);
    }

    /**
     * Listen on a TCP port (BusyBox nc server mode)
     * Waits for a single incoming connection
     *
     * Example: nc.listen --port 1234
     *
     * @param port    Port number to listen on
     * @param timeout Timeout in seconds (default: 30)
     */
    @ShellMethod(key = "nc.listen", value = "Listen on a TCP port for incoming connection")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncListen(
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "30") int timeout) {
        // BusyBox nc: nc -l -p <port> -w <timeout>
        String command = String.format("nc -l -p %d -w %d", port, timeout);
        scmd(command);
    }

    /**
     * Send a string/message to a remote TCP host (BusyBox nc)
     * Useful for sending commands to services or simple messaging
     *
     * Example: nc.send --host 192.168.1.1 --port 1234 --message "hello"
     *
     * @param host    Remote host IP or hostname
     * @param port    Port number
     * @param message Message to send
     * @param timeout Timeout in seconds (default: 5)
     */
    @ShellMethod(key = "nc.send", value = "Send a message to a remote TCP host")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncSend(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--message", "-m" }) String message,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "5") int timeout) {
        // BusyBox nc: echo "<message>" | nc -w <timeout> <host> <port>
        String command = String.format("echo '%s' | nc -w %d %s %d", message, timeout, host, port);
        scmd(command);
    }

    /**
     * Send UDP data to a remote host (BusyBox nc)
     * Useful for syslog, SNMP traps, or UDP service testing
     *
     * Example: nc.send-udp --host 192.168.1.1 --port 514 --message "test syslog"
     *
     * @param host    Remote host IP or hostname
     * @param port    UDP port number
     * @param message Message to send
     * @param timeout Timeout in seconds (default: 3)
     */
    @ShellMethod(key = "nc.send-udp", value = "Send UDP data to a remote host")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncSendUdp(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--message", "-m" }) String message,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "3") int timeout) {
        // BusyBox nc: echo "<message>" | nc -u -w <timeout> <host> <port>
        String command = String.format("echo '%s' | nc -u -w %d %s %d", message, timeout, host, port);
        scmd(command);
    }

    /**
     * Scan a range of TCP ports on a remote host (BusyBox nc)
     * Checks each port in the given range sequentially
     *
     * Example: nc.port-scan --host 192.168.1.1 --start-port 20 --end-port 100
     *
     * @param host      Remote host IP or hostname
     * @param startPort Start of port range
     * @param endPort   End of port range
     * @param timeout   Timeout per port in seconds (default: 1)
     */
    @ShellMethod(key = "nc.port-scan", value = "Scan a range of TCP ports on a remote host")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncPortScan(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--start-port", "-s" }) int startPort,
            @ShellOption(value = { "--end-port", "-e" }) int endPort,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "1") int timeout) {
        // BusyBox nc loop: for each port, nc -z -w <timeout> <host> <port>
        String command = String.format(
                "for port in $(seq %d %d); do " +
                "nc -z -w %d %s $port 2>/dev/null && echo \"Port $port: OPEN\"; " +
                "done",
                startPort, endPort, timeout, host);
        scmd(command);
    }

    /**
     * Test HTTP service on a remote host using nc (BusyBox nc)
     * Sends a minimal HTTP GET request and shows the response headers
     *
     * Example: nc.http-test --host 192.168.1.1 --port 80 --path /
     *
     * @param host    Remote host IP or hostname
     * @param port    HTTP port (default: 80)
     * @param path    URL path (default: /)
     * @param timeout Timeout in seconds (default: 5)
     */
    @ShellMethod(key = "nc.http-test", value = "Test HTTP service using nc")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncHttpTest(
            @ShellOption(value = { "--host", "-h" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "80") int port,
            @ShellOption(value = { "--path" }, defaultValue = "/") String path,
            @ShellOption(value = { "--timeout", "-t" }, defaultValue = "5") int timeout) {
        // BusyBox nc: printf "GET <path> HTTP/1.0\r\nHost: <host>\r\n\r\n" | nc -w <timeout> <host> <port>
        String command = String.format(
                "printf 'GET %s HTTP/1.0\\r\\nHost: %s\\r\\n\\r\\n' | nc -w %d %s %d",
                path, host, timeout, host, port);
        scmd(command);
    }

    /**
     * Execute custom nc (netcat) command (BusyBox compatible)
     *
     * Example: nc.custom "nc -z -w 3 192.168.1.1 22"
     *
     * @param ncCommand The complete nc command to execute
     */
    @ShellMethod(key = "nc.custom", value = "Execute custom nc (netcat) command")
    @ShellMethodAvailability("availabilityCheck")
    protected void ncCustom(String ncCommand) {
        scmd(ncCommand);
    }

    // ==================== LLDP COMMANDS ====================

    /**
     * Show all LLDP neighbors discovered on all interfaces
     * Requires lldpd daemon to be running
     *
     * Example: lldp.neighbors
     */
    @ShellMethod(key = "lldp.neighbors", value = "Show all LLDP neighbors on all interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighbors() {
        scmd("lldpctl");
    }

    /**
     * Show LLDP neighbors on a specific interface
     *
     * Example: lldp.neighbors-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.neighbors-dev", value = "Show LLDP neighbors on a specific interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldpctl %s", device);
        scmd(command);
    }

    /**
     * Show LLDP neighbors in JSON format
     * Useful for programmatic parsing
     *
     * Example: lldp.neighbors-json
     */
    @ShellMethod(key = "lldp.neighbors-json", value = "Show LLDP neighbors in JSON format")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsJson() {
        scmd("lldpctl -f json");
    }

    /**
     * Show LLDP neighbors in JSON format for a specific interface
     *
     * Example: lldp.neighbors-json-dev --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.neighbors-json-dev", value = "Show LLDP neighbors in JSON format for a specific interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsJsonDev(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldpctl -f json %s", device);
        scmd(command);
    }

    /**
     * Show LLDP neighbors in XML format
     *
     * Example: lldp.neighbors-xml
     */
    @ShellMethod(key = "lldp.neighbors-xml", value = "Show LLDP neighbors in XML format")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsXml() {
        scmd("lldpctl -f xml");
    }

    /**
     * Show detailed LLDP neighbor information (via lldpcli)
     * Includes all TLVs: chassis, port, system name, capabilities, management address
     *
     * Example: lldp.neighbors-details
     */
    @ShellMethod(key = "lldp.neighbors-details", value = "Show detailed LLDP neighbor information")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsDetails() {
        scmd("lldpcli show neighbors details");
    }

    /**
     * Show detailed LLDP neighbor information for a specific port (via lldpcli)
     *
     * Example: lldp.neighbors-details-port --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.neighbors-details-port", value = "Show detailed LLDP neighbor info for a specific port")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborsDetailsPort(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldpcli show neighbors ports %s details", device);
        scmd(command);
    }

    /**
     * Show local chassis LLDP information being advertised
     * Shows what this device is announcing to its neighbors
     *
     * Example: lldp.chassis
     */
    @ShellMethod(key = "lldp.chassis", value = "Show local chassis LLDP information being advertised")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpChassis() {
        scmd("lldpcli show chassis");
    }

    /**
     * Show LLDP statistics (frame counts per interface)
     *
     * Example: lldp.statistics
     */
    @ShellMethod(key = "lldp.statistics", value = "Show LLDP frame statistics per interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpStatistics() {
        scmd("lldpcli show statistics");
    }

    /**
     * Show LLDP statistics summary across all interfaces
     *
     * Example: lldp.statistics-summary
     */
    @ShellMethod(key = "lldp.statistics-summary", value = "Show LLDP statistics summary")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpStatisticsSummary() {
        scmd("lldpcli show statistics summary");
    }

    /**
     * Show LLDP-enabled interfaces and their configuration
     *
     * Example: lldp.interfaces
     */
    @ShellMethod(key = "lldp.interfaces", value = "Show LLDP-enabled interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpInterfaces() {
        scmd("lldpcli show interfaces");
    }

    /**
     * Show lldpd daemon configuration
     *
     * Example: lldp.config
     */
    @ShellMethod(key = "lldp.config", value = "Show lldpd daemon configuration")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpConfig() {
        scmd("lldpcli show configuration");
    }

    /**
     * Check lldpd daemon status
     *
     * Example: lldp.status
     */
    @ShellMethod(key = "lldp.status", value = "Check lldpd daemon status")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpStatus() {
        scmd("systemctl status lldpd 2>/dev/null || " +
             "sv status lldpd 2>/dev/null || " +
             "ps aux | grep -v grep | grep lldp");
    }

    /**
     * Show all LLDP neighbor TLVs on an interface using lldptool (lldpad)
     * Alternative to lldpctl for systems using Intel's lldpad daemon
     *
     * Example: lldp.tlv-show --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.tlv-show", value = "Show all LLDP neighbor TLVs via lldptool")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpTlvShow(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldptool get-tlv -n -i %s", device);
        scmd(command);
    }

    /**
     * Show local LLDP TLVs being sent on an interface using lldptool
     *
     * Example: lldp.tlv-local --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.tlv-local", value = "Show local LLDP TLVs being sent on an interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpTlvLocal(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldptool get-tlv -i %s", device);
        scmd(command);
    }

    /**
     * Show a specific LLDP TLV from neighbor using lldptool
     * Common TLV names: sysName, sysDesc, portDesc, mngAddr, chassisID, portID
     *
     * Example: lldp.tlv-get --device eth0 --tlv sysName
     *
     * @param device  Network interface name
     * @param tlvName TLV name (e.g., sysName, sysDesc, portDesc, mngAddr)
     */
    @ShellMethod(key = "lldp.tlv-get", value = "Show a specific LLDP TLV from neighbor via lldptool")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpTlvGet(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--tlv", "-t" }) String tlvName) {
        String command = String.format("lldptool get-tlv -n -i %s -V %s", device, tlvName);
        scmd(command);
    }

    /**
     * Show LLDP admin status for an interface using lldptool
     * Returns: disabled, rx, tx, or rxtx
     *
     * Example: lldp.admin-status --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.admin-status", value = "Show LLDP admin status for an interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpAdminStatus(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldptool get-lldp -i %s adminStatus", device);
        scmd(command);
    }

    /**
     * Enable LLDP transmit and receive on an interface using lldptool
     *
     * Example: lldp.enable --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.enable", value = "Enable LLDP TX+RX on an interface via lldptool")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpEnable(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldptool set-lldp -i %s adminStatus=rxtx", device);
        scmd(command);
    }

    /**
     * Disable LLDP on an interface using lldptool
     *
     * Example: lldp.disable --device eth0
     *
     * @param device Network interface name
     */
    @ShellMethod(key = "lldp.disable", value = "Disable LLDP on an interface via lldptool")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpDisable(
            @ShellOption(value = { "--device", "-d" }) String device) {
        String command = String.format("lldptool set-lldp -i %s adminStatus=disabled", device);
        scmd(command);
    }

    /**
     * Capture raw LLDP frames on an interface using tcpdump
     * LLDP uses EtherType 0x88CC and multicast MAC 01:80:C2:00:00:0E
     *
     * Example: lldp.capture --device eth0 --count 5
     *
     * @param device Network interface name
     * @param count  Number of LLDP frames to capture (default: 5)
     */
    @ShellMethod(key = "lldp.capture", value = "Capture raw LLDP frames using tcpdump")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpCapture(
            @ShellOption(value = { "--device", "-d" }) String device,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "5") int count) {
        String command = String.format("tcpdump -i %s -nn -v -c %d ether proto 0x88cc", device, count);
        scmd(command);
    }

    /**
     * Show neighbor switch name and port from LLDP (quick summary)
     * Extracts System Name and Port ID from lldpctl output
     *
     * Example: lldp.neighbor-summary
     */
    @ShellMethod(key = "lldp.neighbor-summary", value = "Show quick summary of LLDP neighbor switch and port")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborSummary() {
        scmd("lldpctl -f json 2>/dev/null | grep -E '\"name\"|\"descr\"|\"id\"' || " +
             "lldpctl 2>/dev/null | grep -E 'SysName|PortID|ChassisID|MgmtIP'");
    }

    /**
     * Show LLDP neighbor management IP addresses
     * Useful for finding the management IP of connected switches
     *
     * Example: lldp.neighbor-mgmt-ip
     */
    @ShellMethod(key = "lldp.neighbor-mgmt-ip", value = "Show LLDP neighbor management IP addresses")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpNeighborMgmtIp() {
        scmd("lldpctl 2>/dev/null | grep -i 'MgmtIP\\|Management'");
    }

    /**
     * Execute custom lldpctl command
     *
     * Example: lldp.custom "lldpctl -f json eth0"
     *
     * @param lldpCommand The complete lldpctl/lldpcli command to execute
     */
    @ShellMethod(key = "lldp.custom", value = "Execute custom lldpctl/lldpcli command")
    @ShellMethodAvailability("availabilityCheck")
    protected void lldpCustom(String lldpCommand) {
        scmd(lldpCommand);
    }
}

// Made with Bob
