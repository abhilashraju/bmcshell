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
     * @param ipAddress     The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count         Number of packets to send (default: 3)
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
     * @param ipAddress     The IP address to probe
     * @param interfaceName Network interface (default: eth2)
     * @param count         Number of packets to send (default: 3)
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
     * @param ipAddress     The IP address to announce
     * @param interfaceName Network interface (default: eth2)
     * @param count         Number of packets to send (default: 3)
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
     * @param ipAddress     The IP address to check
     * @param interfaceName Network interface (default: eth2)
     * @param count         Number of packets to send (default: 2)
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
     * @param count         Number of packets to send (default: 5)
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
     * @param count         Number of packets to send (default: 3)
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
     * @param device    Network interface name
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
     * @param host  The host to ping
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
     * @param device     Network interface name
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
     * @param device     Network interface name
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
     * @param device     Network interface name
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
     * @param mtu    MTU value
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
     * @param via         Gateway IP address
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
     * Example: ip.route-add-dev --destination 192.168.2.0/24 --via 192.168.1.1
     * --device eth0
     *
     * @param destination Destination network in CIDR notation
     * @param via         Gateway IP address
     * @param device      Network interface name
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
     * Example: ip.neigh-add --ip 192.168.1.100 --mac 00:11:22:33:44:55 --device
     * eth0
     *
     * @param ipAddress  IP address
     * @param macAddress MAC address
     * @param device     Network interface name
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
     * @param device    Network interface name
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
     * @param device    Network interface name
     * @param timeoutMs Base reachable time in milliseconds (default: 1000ms = 1
     *                  second)
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
     * @param device  Network interface name
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
     * @param device    Network interface name
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

    // ==================== NC (NETCAT) COMMANDS - BusyBox Compatible
    // ====================

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
        // BusyBox nc: printf "GET <path> HTTP/1.0\r\nHost: <host>\r\n\r\n" | nc -w
        // <timeout> <host> <port>
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
     * Includes all TLVs: chassis, port, system name, capabilities, management
     * address
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

    // ============================================================================
    // Redfish Network Commands (based on ethernet.hpp)
    // ============================================================================

    /**
     * List all Ethernet interfaces available on the BMC
     * Retrieves interface names from Redfish EthernetInterface collection
     * 
     * Example: net.interfaces
     */
    @ShellMethod(key = "net.interfaces", value = "List all Ethernet interfaces")
    @ShellMethodAvailability("availabilityCheck")
    protected void netInterfaces() {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces", "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error listing interfaces: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about a specific Ethernet interface
     * Shows MAC address, IP addresses, DHCP settings, MTU, etc.
     * 
     * Example: net.interface.get --interface eth0
     * 
     * @param interfaceName The network interface name (e.g., eth0, eth1)
     */
    @ShellMethod(key = "net.interface.get", value = "Get Ethernet interface details")
    @ShellMethodAvailability("availabilityCheck")
    protected void netInterfaceGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting interface details: " + e.getMessage());
        }
    }

    /**
     * Set hostname for the BMC
     * Updates the system hostname via Redfish
     * 
     * Example: net.hostname.set --hostname mybmc --interface eth0
     * 
     * @param hostname      The new hostname (max 255 characters)
     * @param interfaceName The network interface name (default: eth0)
     */
    @ShellMethod(key = "net.hostname.set", value = "Set BMC hostname")
    @ShellMethodAvailability("availabilityCheck")
    protected void netHostnameSet(
            @ShellOption(value = { "--hostname", "-h" }) String hostname,
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth0") String interfaceName) {
        try {
            String data = String.format("{\"HostName\":\"%s\"}", hostname);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting hostname: " + e.getMessage());
        }
    }

    /**
     * Get current hostname
     * Retrieves the BMC hostname from Redfish
     * 
     * Example: net.hostname.get --interface eth0
     * 
     * @param interfaceName The network interface name (default: eth0)
     */
    @ShellMethod(key = "net.hostname.get", value = "Get BMC hostname")
    @ShellMethodAvailability("availabilityCheck")
    protected void netHostnameGet(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth0") String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting hostname: " + e.getMessage());
        }
    }

    /**
     * Set MTU size for an interface
     * Updates the Maximum Transmission Unit size
     * 
     * Example: net.mtu.set --interface eth0 --mtu 1500
     * 
     * @param interfaceName The network interface name
     * @param mtuSize       The MTU size in bytes (typically 1500 or 9000)
     */
    @ShellMethod(key = "net.mtu.set", value = "Set interface MTU size")
    @ShellMethodAvailability("availabilityCheck")
    protected void netMtuSet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--mtu", "-m" }) int mtuSize) {
        try {
            String data = String.format("{\"MTUSize\":%d}", mtuSize);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting MTU: " + e.getMessage());
        }
    }

    /**
     * Get MTU size for an interface
     * Retrieves the current MTU size
     * 
     * Example: net.mtu.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.mtu.get", value = "Get interface MTU size")
    @ShellMethodAvailability("availabilityCheck")
    protected void netMtuGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting MTU: " + e.getMessage());
        }
    }

    /**
     * Set FQDN (Fully Qualified Domain Name)
     * Updates both hostname and domain name
     * 
     * Example: net.fqdn.set --interface eth0 --fqdn mybmc.example.com
     * 
     * @param interfaceName The network interface name
     * @param fqdn          The fully qualified domain name (max 255 characters)
     */
    @ShellMethod(key = "net.fqdn.set", value = "Set FQDN (hostname.domain)")
    @ShellMethodAvailability("availabilityCheck")
    protected void netFqdnSet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--fqdn", "-f" }) String fqdn) {
        try {
            String data = String.format("{\"FQDN\":\"%s\"}", fqdn);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting FQDN: " + e.getMessage());
        }
    }

    /**
     * Get FQDN for an interface
     * Retrieves the fully qualified domain name
     * 
     * Example: net.fqdn.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.fqdn.get", value = "Get interface FQDN")
    @ShellMethodAvailability("availabilityCheck")
    protected void netFqdnGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting FQDN: " + e.getMessage());
        }
    }

    /**
     * Set MAC address for an interface
     * Updates the hardware MAC address
     * 
     * Example: net.mac.set --interface eth0 --mac 00:11:22:33:44:55
     * 
     * @param interfaceName The network interface name
     * @param macAddress    The MAC address in format XX:XX:XX:XX:XX:XX
     */
    @ShellMethod(key = "net.mac.set", value = "Set interface MAC address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netMacSet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--mac", "-m" }) String macAddress) {
        try {
            String data = String.format("{\"MACAddress\":\"%s\"}", macAddress);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting MAC address: " + e.getMessage());
        }
    }

    /**
     * Get MAC address for an interface
     * Retrieves the hardware MAC address
     * 
     * Example: net.mac.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.mac.get", value = "Get interface MAC address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netMacGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting MAC address: " + e.getMessage());
        }
    }

    /**
     * Enable DHCP for IPv4, IPv6, or both
     * Configures DHCP settings via Redfish
     * 
     * Example: net.dhcp.enable --interface eth0 --version v4
     * Example: net.dhcp.enable --interface eth0 --version both
     * 
     * @param interfaceName The network interface name
     * @param version       DHCP version: v4, v6, or both (default: v4)
     */
    @ShellMethod(key = "net.dhcp.enable", value = "Enable DHCP")
    @ShellMethodAvailability("availabilityCheck")
    protected void netDhcpEnable(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--version", "-v" }, defaultValue = "v4") String version) {
        try {
            String data;
            if (version.equalsIgnoreCase("both")) {
                data = "{\"DHCPv4\":{\"DHCPEnabled\":true},\"DHCPv6\":{\"OperatingMode\":\"Enabled\"}}";
            } else if (version.equalsIgnoreCase("v6")) {
                data = "{\"DHCPv6\":{\"OperatingMode\":\"Enabled\"}}";
            } else {
                data = "{\"DHCPv4\":{\"DHCPEnabled\":true}}";
            }
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error enabling DHCP: " + e.getMessage());
        }
    }

    /**
     * Disable DHCP for IPv4, IPv6, or both
     * Disables DHCP settings via Redfish
     * 
     * Example: net.dhcp.disable --interface eth0 --version v4
     * 
     * @param interfaceName The network interface name
     * @param version       DHCP version: v4, v6, or both (default: v4)
     */
    @ShellMethod(key = "net.dhcp.disable", value = "Disable DHCP")
    @ShellMethodAvailability("availabilityCheck")
    protected void netDhcpDisable(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--version", "-v" }, defaultValue = "v4") String version) {
        try {
            String data;
            if (version.equalsIgnoreCase("both")) {
                data = "{\"DHCPv4\":{\"DHCPEnabled\":false},\"DHCPv6\":{\"OperatingMode\":\"Disabled\"}}";
            } else if (version.equalsIgnoreCase("v6")) {
                data = "{\"DHCPv6\":{\"OperatingMode\":\"Disabled\"}}";
            } else {
                data = "{\"DHCPv4\":{\"DHCPEnabled\":false}}";
            }
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error disabling DHCP: " + e.getMessage());
        }
    }

    /**
     * Get DHCP status for an interface
     * Shows DHCPv4 and DHCPv6 configuration
     * 
     * Example: net.dhcp.status --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.dhcp.status", value = "Get DHCP status")
    @ShellMethodAvailability("availabilityCheck")
    protected void netDhcpStatus(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting DHCP status: " + e.getMessage());
        }
    }

    /**
     * Configure DHCPv4 options (DNS, NTP, Domain Name)
     * Enables or disables DHCP-provided configuration
     * 
     * Example: net.dhcp.config --interface eth0 --dns true --ntp true --domain
     * false
     * 
     * @param interfaceName The network interface name
     * @param useDns        Use DHCP-provided DNS servers (default: true)
     * @param useNtp        Use DHCP-provided NTP servers (default: true)
     * @param useDomain     Use DHCP-provided domain name (default: true)
     */
    @ShellMethod(key = "net.dhcp.config", value = "Configure DHCPv4 options")
    @ShellMethodAvailability("availabilityCheck")
    protected void netDhcpConfig(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--dns" }, defaultValue = "true") boolean useDns,
            @ShellOption(value = { "--ntp" }, defaultValue = "true") boolean useNtp,
            @ShellOption(value = { "--domain" }, defaultValue = "true") boolean useDomain) {
        try {
            String data = String.format("{\"DHCPv4\":{\"UseDNSServers\":%b,\"UseNTPServers\":%b,\"UseDomainName\":%b}}",
                    useDns, useNtp, useDomain);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error configuring DHCP options: " + e.getMessage());
        }
    }

    /**
     * Enable IPv6 SLAAC (Stateless Address Auto Configuration)
     * Enables IPv6 Router Advertisement acceptance
     * 
     * Example: net.ipv6.slaac.enable --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv6.slaac.enable", value = "Enable IPv6 SLAAC")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6SlaacEnable(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String data = "{\"StatelessAddressAutoConfig\":{\"IPv6AutoConfigEnabled\":true}}";
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error enabling IPv6 SLAAC: " + e.getMessage());
        }
    }

    /**
     * Disable IPv6 SLAAC
     * Disables IPv6 Router Advertisement acceptance
     * 
     * Example: net.ipv6.slaac.disable --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv6.slaac.disable", value = "Disable IPv6 SLAAC")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6SlaacDisable(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String data = "{\"StatelessAddressAutoConfig\":{\"IPv6AutoConfigEnabled\":false}}";
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error disabling IPv6 SLAAC: " + e.getMessage());
        }
    }

    /**
     * List all IPv4 addresses on an interface
     * Shows address, subnet mask, gateway, and origin
     * 
     * Example: net.ipv4.list --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv4.list", value = "List IPv4 addresses")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv4List(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error listing IPv4 addresses: " + e.getMessage());
        }
    }

    /**
     * Add a static IPv4 address
     * Creates a new static IPv4 configuration
     * 
     * Example: net.ipv4.add --interface eth0 --address 192.168.1.100 --netmask
     * 255.255.255.0 --gateway 192.168.1.1
     * 
     * @param interfaceName The network interface name
     * @param address       The IPv4 address
     * @param netmask       The subnet mask
     * @param gateway       The default gateway (optional)
     */
    @ShellMethod(key = "net.ipv4.add", value = "Add static IPv4 address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv4Add(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--address", "-a" }) String address,
            @ShellOption(value = { "--netmask", "-n" }) String netmask,
            @ShellOption(value = { "--gateway", "-g" }, defaultValue = "") String gateway) {
        try {
            String data;
            if (gateway.isEmpty()) {
                data = String.format("{\"IPv4StaticAddresses\":[{\"Address\":\"%s\",\"SubnetMask\":\"%s\"}]}",
                        address, netmask);
            } else {
                data = String.format(
                        "{\"IPv4StaticAddresses\":[{\"Address\":\"%s\",\"SubnetMask\":\"%s\",\"Gateway\":\"%s\"}]}",
                        address, netmask, gateway);
            }
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error adding IPv4 address: " + e.getMessage());
        }
    }

    /**
     * Delete an IPv4 address by setting it to null
     * Removes a static IPv4 configuration
     * 
     * Example: net.ipv4.delete --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv4.delete", value = "Delete IPv4 address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv4Delete(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String data = "{\"IPv4StaticAddresses\":[null]}";
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error deleting IPv4 address: " + e.getMessage());
        }
    }

    /**
     * List all IPv6 addresses on an interface
     * Shows address, prefix length, and origin
     * 
     * Example: net.ipv6.list --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv6.list", value = "List IPv6 addresses")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6List(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error listing IPv6 addresses: " + e.getMessage());
        }
    }

    /**
     * Add a static IPv6 address
     * Creates a new static IPv6 configuration
     * 
     * Example: net.ipv6.add --interface eth0 --address fe80::1 --prefix 64
     * 
     * @param interfaceName The network interface name
     * @param address       The IPv6 address
     * @param prefixLength  The prefix length (default: 64)
     */
    @ShellMethod(key = "net.ipv6.add", value = "Add static IPv6 address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6Add(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--address", "-a" }) String address,
            @ShellOption(value = { "--prefix", "-p" }, defaultValue = "64") int prefixLength) {
        try {
            String data = String.format("{\"IPv6StaticAddresses\":[{\"Address\":\"%s\",\"PrefixLength\":%d}]}",
                    address, prefixLength);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error adding IPv6 address: " + e.getMessage());
        }
    }

    /**
     * Delete an IPv6 address by setting it to null
     * Removes a static IPv6 configuration
     * 
     * Example: net.ipv6.delete --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv6.delete", value = "Delete IPv6 address")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6Delete(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String data = "{\"IPv6StaticAddresses\":[null]}";
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error deleting IPv6 address: " + e.getMessage());
        }
    }

    /**
     * Set IPv6 default gateway
     * Configures the default gateway for IPv6
     * 
     * Example: net.ipv6.gateway.set --interface eth0 --gateway fe80::1
     * 
     * @param interfaceName The network interface name
     * @param gateway       The IPv6 gateway address
     */
    @ShellMethod(key = "net.ipv6.gateway.set", value = "Set IPv6 default gateway")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6GatewaySet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--gateway", "-g" }) String gateway) {
        try {
            String data = String.format("{\"IPv6DefaultGateway\":\"%s\"}", gateway);
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting IPv6 gateway: " + e.getMessage());
        }
    }

    /**
     * Get IPv6 default gateway
     * Retrieves the default gateway for IPv6
     * 
     * Example: net.ipv6.gateway.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.ipv6.gateway.get", value = "Get IPv6 default gateway")
    @ShellMethodAvailability("availabilityCheck")
    protected void netIpv6GatewayGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting IPv6 gateway: " + e.getMessage());
        }
    }

    /**
     * Set static name servers (DNS)
     * Configures static DNS servers for the interface
     * 
     * Example: net.nameservers.set --interface eth0 --servers "8.8.8.8,8.8.4.4"
     * 
     * @param interfaceName The network interface name
     * @param servers       Comma-separated list of DNS server IPs
     */
    @ShellMethod(key = "net.nameservers.set", value = "Set static DNS servers")
    @ShellMethodAvailability("availabilityCheck")
    protected void netNameserversSet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName,
            @ShellOption(value = { "--servers", "-s" }) String servers) {
        try {
            String[] serverArray = servers.split(",");
            StringBuilder jsonArray = new StringBuilder("[");
            for (int i = 0; i < serverArray.length; i++) {
                jsonArray.append("\"").append(serverArray[i].trim()).append("\"");
                if (i < serverArray.length - 1) {
                    jsonArray.append(",");
                }
            }
            jsonArray.append("]");
            String data = String.format("{\"StaticNameServers\":%s}", jsonArray.toString());
            String response = makePatchRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, data);
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error setting name servers: " + e.getMessage());
        }
    }

    /**
     * Get name servers (DNS)
     * Retrieves configured DNS servers
     * 
     * Example: net.nameservers.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.nameservers.get", value = "Get DNS servers")
    @ShellMethodAvailability("availabilityCheck")
    protected void netNameserversGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting name servers: " + e.getMessage());
        }
    }

    /**
     * Get interface link status
     * Shows if the interface link is up or down
     * 
     * Example: net.link.status --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.link.status", value = "Get interface link status")
    @ShellMethodAvailability("availabilityCheck")
    protected void netLinkStatus(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting link status: " + e.getMessage());
        }
    }

    /**
     * Get VLAN information
     * Shows VLAN ID and enabled status
     * 
     * Example: net.vlan.get --interface eth0
     * 
     * @param interfaceName The network interface name
     */
    @ShellMethod(key = "net.vlan.get", value = "Get VLAN information")
    @ShellMethodAvailability("availabilityCheck")
    protected void netVlanGet(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            String response = makeGetRequest("/redfish/v1/Managers/bmc/EthernetInterfaces/" + interfaceName, "");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Error getting VLAN info: " + e.getMessage());
        }
    }

    /**
     * Show ethtool information for an interface
     * Displays link status, speed, duplex, and other interface details
     *
     * Example: ethtool.show --interface eth2
     *
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.show", value = "Show ethtool information for an interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolShow(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool %s", interfaceName);
        scmd(command);
    }

    /**
     * Show link detection status
     * Quick check if physical link is detected on interface
     * 
     * Example: ethtool.link --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.link", value = "Show link detection status")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolLink(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool %s | grep 'Link detected'", interfaceName);
        scmd(command);
    }

    /**
     * Show interface statistics
     * Displays packet counters, errors, and drops
     * 
     * Example: ethtool.stats --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.stats", value = "Show interface statistics")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolStats(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -S %s", interfaceName);
        scmd(command);
    }

    /**
     * Show driver information
     * Displays driver name, version, firmware version
     * 
     * Example: ethtool.driver --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.driver", value = "Show driver information")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolDriver(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -i %s", interfaceName);
        scmd(command);
    }

    /**
     * Show ring buffer parameters
     * Displays RX/TX ring buffer sizes
     * 
     * Example: ethtool.ring --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.ring", value = "Show ring buffer parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolRing(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -g %s", interfaceName);
        scmd(command);
    }

    /**
     * Show pause parameters
     * Displays flow control settings
     * 
     * Example: ethtool.pause --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.pause", value = "Show pause parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolPause(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -a %s", interfaceName);
        scmd(command);
    }

    /**
     * Show coalesce parameters
     * Displays interrupt coalescing settings
     * 
     * Example: ethtool.coalesce --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.coalesce", value = "Show coalesce parameters")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolCoalesce(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -c %s", interfaceName);
        scmd(command);
    }

    /**
     * Show offload features
     * Displays hardware offload capabilities (TSO, GSO, etc.)
     * 
     * Example: ethtool.features --interface eth2
     * 
     * @param interfaceName The network interface name (default: eth2)
     */
    @ShellMethod(key = "ethtool.features", value = "Show offload features")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolFeatures(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName) {
        String command = String.format("ethtool -k %s", interfaceName);
        scmd(command);
    }

    /**
     * Set interface speed and duplex
     * Configure link speed and duplex mode
     * 
     * Example: ethtool.speed --interface eth2 --speed 1000 --duplex full
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param speed         Link speed in Mbps (10, 100, 1000, etc.)
     * @param duplex        Duplex mode: full or half
     */
    @ShellMethod(key = "ethtool.speed", value = "Set interface speed and duplex")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolSpeed(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--speed", "-s" }) int speed,
            @ShellOption(value = { "--duplex", "-d" }, defaultValue = "full") String duplex) {
        String command = String.format("ethtool -s %s speed %d duplex %s", interfaceName, speed, duplex);
        scmd(command);
    }

    /**
     * Enable/disable autonegotiation
     * Configure auto-negotiation for speed and duplex
     * 
     * Example: ethtool.autoneg --interface eth2 --enable
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param enable        Enable autonegotiation (default: true)
     */
    @ShellMethod(key = "ethtool.autoneg", value = "Enable/disable autonegotiation")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolAutoneg(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--enable", "-e" }, defaultValue = "true") boolean enable) {
        String command = String.format("ethtool -s %s autoneg %s", interfaceName, enable ? "on" : "off");
        scmd(command);
    }

    /**
     * Run custom ethtool command
     * Execute any ethtool command with custom parameters
     * 
     * Example: ethtool.custom "--command -P eth2"
     * 
     * @param command The ethtool command with parameters
     */
    @ShellMethod(key = "ethtool.custom", value = "Run custom ethtool command")
    @ShellMethodAvailability("availabilityCheck")
    protected void ethtoolCustom(
            @ShellOption(value = { "--command", "-c" }) String command) {
        String fullCommand = String.format("ethtool %s", command);
        scmd(fullCommand);
    }

    /**
     * Capture ARP traffic on an interface
     * Monitor ARP requests and replies for troubleshooting
     *
     * Example: tcpdump.arp --interface eth2 --count 10
     *
     * @param interfaceName The network interface name (default: eth2)
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.arp", value = "Capture ARP traffic on an interface")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpArp(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n arp -c %d", interfaceName, count)
                : String.format("tcpdump -i %s -n arp", interfaceName);
        scmd(command);
    }

    /**
     * Capture ICMP (ping) traffic on an interface
     * Monitor ping requests and replies
     * 
     * Example: tcpdump.icmp --interface eth2 --count 10
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.icmp", value = "Capture ICMP (ping) traffic")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpIcmp(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n icmp -c %d", interfaceName, count)
                : String.format("tcpdump -i %s -n icmp", interfaceName);
        scmd(command);
    }

    /**
     * Capture traffic for a specific host
     * Monitor all traffic to/from a specific IP address
     * 
     * Example: tcpdump.host --ip 9.6.28.101 --interface eth2 --count 20
     * 
     * @param ipAddress     The IP address to monitor
     * @param interfaceName The network interface name (default: eth2)
     * @param count         Number of packets to capture (default: 20, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.host", value = "Capture traffic for a specific host")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpHost(
            @ShellOption(value = { "--ip", "-h" }) String ipAddress,
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "20") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n host %s -c %d", interfaceName, ipAddress, count)
                : String.format("tcpdump -i %s -n host %s", interfaceName, ipAddress);
        scmd(command);
    }

    /**
     * Capture traffic on a specific port
     * Monitor TCP/UDP traffic on a port
     * 
     * Example: tcpdump.port --port 443 --interface eth0 --count 10
     * 
     * @param port          The port number to monitor
     * @param interfaceName The network interface name (default: eth0)
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.port", value = "Capture traffic on a specific port")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpPort(
            @ShellOption(value = { "--port", "-p" }) int port,
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth0") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n port %d -c %d", interfaceName, port, count)
                : String.format("tcpdump -i %s -n port %d", interfaceName, port);
        scmd(command);
    }

    /**
     * Capture all traffic on an interface with verbose output
     * Shows detailed packet information
     * 
     * Example: tcpdump.verbose --interface eth2 --count 10 --verbosity 2
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     * @param verbosity     Verbosity level: 1, 2, or 3 (default: 1)
     */
    @ShellMethod(key = "tcpdump.verbose", value = "Capture traffic with verbose output")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpVerbose(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count,
            @ShellOption(value = { "--verbosity", "-v" }, defaultValue = "1") int verbosity) {
        String vFlag = "-" + "v".repeat(Math.min(verbosity, 3));
        String command = count > 0
                ? String.format("tcpdump -i %s -n %s -c %d", interfaceName, vFlag, count)
                : String.format("tcpdump -i %s -n %s", interfaceName, vFlag);
        scmd(command);
    }

    /**
     * Capture traffic and save to file
     * Save packet capture to a pcap file for later analysis
     * 
     * Example: tcpdump.save --interface eth2 --file /tmp/capture.pcap --count 100
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param filename      Output pcap filename (default: /tmp/capture.pcap)
     * @param count         Number of packets to capture (default: 100, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.save", value = "Capture traffic and save to file")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpSave(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--file", "-f" }, defaultValue = "/tmp/capture.pcap") String filename,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "100") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n -w %s -c %d", interfaceName, filename, count)
                : String.format("tcpdump -i %s -n -w %s", interfaceName, filename);
        scmd(command);
    }

    /**
     * Read and display a pcap file
     * Display contents of a previously saved packet capture
     * 
     * Example: tcpdump.read --file /tmp/capture.pcap --count 20
     * 
     * @param filename The pcap file to read
     * @param count    Number of packets to display (default: 20, 0 = all)
     */
    @ShellMethod(key = "tcpdump.read", value = "Read and display a pcap file")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpRead(
            @ShellOption(value = { "--file", "-f" }) String filename,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "20") int count) {
        String command = count > 0
                ? String.format("tcpdump -n -r %s -c %d", filename, count)
                : String.format("tcpdump -n -r %s", filename);
        scmd(command);
    }

    /**
     * Capture traffic with specific filter expression
     * Use tcpdump filter syntax for advanced filtering
     * 
     * Example: tcpdump.filter --interface eth2 --filter "tcp and port 443" --count
     * 10
     * 
     * @param interfaceName The network interface name (default: eth2)
     * @param filter        tcpdump filter expression
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.filter", value = "Capture traffic with custom filter")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpFilter(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--filter", "-f" }) String filter,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n '%s' -c %d", interfaceName, filter, count)
                : String.format("tcpdump -i %s -n '%s'", interfaceName, filter);
        scmd(command);
    }

    /**
     * Capture traffic between two hosts
     * Monitor traffic between two specific IP addresses
     * 
     * Example: tcpdump.between --host1 9.6.28.100 --host2 9.6.28.101 --interface
     * eth2 --count 20
     * 
     * @param host1         First IP address
     * @param host2         Second IP address
     * @param interfaceName The network interface name (default: eth2)
     * @param count         Number of packets to capture (default: 20, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.between", value = "Capture traffic between two hosts")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpBetween(
            @ShellOption(value = { "--host1", "-h1" }) String host1,
            @ShellOption(value = { "--host2", "-h2" }) String host2,
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth2") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "20") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n 'host %s and host %s' -c %d", interfaceName, host1, host2, count)
                : String.format("tcpdump -i %s -n 'host %s and host %s'", interfaceName, host1, host2);
        scmd(command);
    }

    /**
     * Capture only TCP SYN packets
     * Monitor TCP connection attempts
     * 
     * Example: tcpdump.syn --interface eth0 --count 10
     * 
     * @param interfaceName The network interface name (default: eth0)
     * @param count         Number of packets to capture (default: 10, 0 =
     *                      unlimited)
     */
    @ShellMethod(key = "tcpdump.syn", value = "Capture TCP SYN packets")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpSyn(
            @ShellOption(value = { "--interface", "-i" }, defaultValue = "eth0") String interfaceName,
            @ShellOption(value = { "--count", "-c" }, defaultValue = "10") int count) {
        String command = count > 0
                ? String.format("tcpdump -i %s -n 'tcp[tcpflags] & tcp-syn != 0' -c %d", interfaceName, count)
                : String.format("tcpdump -i %s -n 'tcp[tcpflags] & tcp-syn != 0'", interfaceName);
        scmd(command);
    }

    /**
     * Run custom tcpdump command
     * Execute any tcpdump command with custom parameters
     * 
     * Example: tcpdump.custom --command "-i eth2 -n -vv udp port 67"
     * 
     * @param command The tcpdump command with parameters
     */
    @ShellMethod(key = "tcpdump.custom", value = "Run custom tcpdump command")
    @ShellMethodAvailability("availabilityCheck")
    protected void tcpdumpCustom(
            @ShellOption(value = { "--command", "-c" }) String command) {
        String fullCommand = String.format("tcpdump %s", command);
        scmd(fullCommand);
    }
}

// Made with Bob
