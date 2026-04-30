package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.Utils.Util;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

/**
 * VPD (Vital Product Data) Commands for querying
 * /var/lib/vpd/vpd_inventory.json on the remote BMC machine.
 * 
 * This implementation fetches the JSON from the remote machine and parses it
 * client-side using Jackson tree parsing for maximum flexibility.
 */
@ShellComponent
public class VpdCommands extends CommonCommands {

    private static final String VPD_JSON = "/var/lib/vpd/vpd_inventory.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache for VPD data to avoid repeated fetches
    private JsonNode cachedVpdData = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_VALIDITY_MS = 30000; // 30 seconds

    protected VpdCommands() throws IOException {
    }

    /**
     * Fetch and parse VPD inventory JSON from remote machine using tree parsing
     */
    private JsonNode fetchVpdData() throws Exception {
        // Check cache validity
        long now = System.currentTimeMillis();
        if (cachedVpdData != null && (now - cacheTimestamp) < CACHE_VALIDITY_MS) {
            return cachedVpdData;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                String.format("cat %s 2>/dev/null || echo '{}'", VPD_JSON));

        String jsonContent = outputStream.toString("UTF-8");
        cachedVpdData = objectMapper.readTree(jsonContent);
        cacheTimestamp = now;

        return cachedVpdData;
    }

    /**
     * Get all VPD entries as a list of JsonNodes
     * Handles both top-level arrays and nested structures
     */
    private List<JsonNode> getAllEntries() throws Exception {
        JsonNode root = fetchVpdData();
        List<JsonNode> allEntries = new ArrayList<>();

        // Recursively collect all array entries
        collectEntries(root, allEntries);

        return allEntries;
    }

    /**
     * Recursively collect all array entries from JSON tree
     */
    private void collectEntries(JsonNode node, List<JsonNode> allEntries) {
        if (node == null) {
            return;
        }

        if (node.isArray()) {
            // If it's an array, add all items
            for (JsonNode item : node) {
                allEntries.add(item);
            }
        } else if (node.isObject()) {
            // If it's an object, recursively check all fields
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                collectEntries(entry.getValue(), allEntries);
            }
        }
    }

    /**
     * Helper: Get text value from JsonNode, return null if missing
     */
    private String getText(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            if (current == null || !current.has(key)) {
                return null;
            }
            current = current.get(key);
        }
        return current != null && !current.isNull() ? current.asText() : null;
    }

    /**
     * Helper: Check if entry has a specific interface
     */
    private boolean hasInterface(JsonNode entry, String interfaceName) {
        JsonNode extraInterfaces = entry.get("extraInterfaces");
        return extraInterfaces != null && extraInterfaces.has(interfaceName);
    }

    /**
     * Helper: Get location code from entry
     */
    private String getLocationCode(JsonNode entry) {
        return getText(entry, "extraInterfaces", "com.ibm.ipzvpd.Location", "LocationCode");
    }

    /**
     * Helper: Get pretty name from entry
     */
    private String getPrettyName(JsonNode entry) {
        return getText(entry, "extraInterfaces", "xyz.openbmc_project.Inventory.Item", "PrettyName");
    }

    /**
     * Helper: Get inventory path from entry
     */
    private String getInventoryPath(JsonNode entry) {
        return getText(entry, "inventoryPath");
    }

    /**
     * Clear the VPD data cache to force a fresh fetch
     */
    @ShellMethod(key = "vpd.refresh", value = "Clear VPD cache and fetch fresh data")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdRefresh() {
        cachedVpdData = null;
        System.out.println("VPD cache cleared. Next command will fetch fresh data.");
    }

    // ==================== BASIC FILE COMMANDS ====================

    @ShellMethod(key = "vpd.show", value = "Display the full VPD inventory JSON file")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdShow() {
        try {
            JsonNode data = fetchVpdData();
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            System.out.println(prettyJson);
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.show-pretty", value = "Display the VPD inventory JSON file with pretty formatting")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdShowPretty() {
        vpdShow();
    }

    // ==================== EEPROM PATH QUERIES ====================

    @ShellMethod(key = "vpd.list-eeprom-paths", value = "List all EEPROM paths in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListEepromPaths() {
        try {
            JsonNode root = fetchVpdData();
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode value = root.get(fieldName);
                if (value.isArray()) {
                    System.out.println(fieldName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== INVENTORY PATH QUERIES ====================

    @ShellMethod(key = "vpd.list-inventory-paths", value = "List all inventory paths in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListInventoryPaths() {
        try {
            getAllEntries().forEach(entry -> {
                String path = getInventoryPath(entry);
                if (path != null) {
                    System.out.println(path);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.search-inventory-path", value = "Search inventory paths by pattern")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSearchInventoryPath(@ShellOption(value = { "--pattern", "-p" }) String pattern) {
        try {
            getAllEntries().forEach(entry -> {
                String path = getInventoryPath(entry);
                if (path != null && path.contains(pattern)) {
                    System.out.println(path);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== LOCATION CODE QUERIES ====================

    @ShellMethod(key = "vpd.list-location-codes", value = "List all location codes in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListLocationCodes() {
        try {
            getAllEntries().forEach(entry -> {
                String locationCode = getLocationCode(entry);
                String inventoryPath = getInventoryPath(entry);
                if (locationCode != null) {
                    System.out.println(locationCode + " -> " + inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.search-location-code", value = "Search location codes by pattern")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSearchLocationCode(@ShellOption(value = { "--pattern", "-p" }) String pattern) {
        try {
            getAllEntries().forEach(entry -> {
                String locationCode = getLocationCode(entry);
                String inventoryPath = getInventoryPath(entry);
                if (locationCode != null && locationCode.contains(pattern)) {
                    System.out.println(locationCode + " -> " + inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== PRETTY NAME QUERIES ====================

    @ShellMethod(key = "vpd.list-pretty-names", value = "List all pretty names in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPrettyNames() {
        try {
            getAllEntries().forEach(entry -> {
                String prettyName = getPrettyName(entry);
                String inventoryPath = getInventoryPath(entry);
                if (prettyName != null) {
                    System.out.println(prettyName + " -> " + inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.find-pretty-name", value = "Find inventory entries by pretty name")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindPrettyName(@ShellOption(value = { "--name", "-n" }) String name) {
        try {
            getAllEntries().forEach(entry -> {
                String prettyName = getPrettyName(entry);
                String inventoryPath = getInventoryPath(entry);
                if (prettyName != null && prettyName.contains(name)) {
                    System.out.println(prettyName + " -> " + inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== INTERFACE QUERIES ====================

    @ShellMethod(key = "vpd.list-interfaces", value = "List all unique extra interface names in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListInterfaces() {
        try {
            Set<String> interfaces = new TreeSet<>();
            getAllEntries().forEach(entry -> {
                JsonNode extraInterfaces = entry.get("extraInterfaces");
                if (extraInterfaces != null) {
                    extraInterfaces.fieldNames().forEachRemaining(interfaces::add);
                }
            });
            interfaces.forEach(System.out::println);
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.find-by-interface", value = "Find inventory entries by extra interface name")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindByInterface(@ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        try {
            getAllEntries().forEach(entry -> {
                if (hasInterface(entry, interfaceName)) {
                    String path = getInventoryPath(entry);
                    if (path != null) {
                        System.out.println(path);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== CONNECTOR QUERIES ====================

    @ShellMethod(key = "vpd.list-connectors", value = "List all connector/port entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListConnectors() {
        try {
            getAllEntries().forEach(entry -> {
                if (hasInterface(entry, "xyz.openbmc_project.Inventory.Item.Connector")) {
                    String locationCode = getLocationCode(entry);
                    String prettyName = getPrettyName(entry);
                    String inventoryPath = getInventoryPath(entry);
                    System.out.println(
                            (locationCode != null ? locationCode : "N/A") + " | " +
                                    (prettyName != null ? prettyName : "N/A") + " | " +
                                    inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== PCIe SLOT QUERIES ====================

    @ShellMethod(key = "vpd.list-pcie-devices", value = "List all PCIe device entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPcieDevices() {
        try {
            getAllEntries().forEach(entry -> {
                if (hasInterface(entry, "xyz.openbmc_project.Inventory.Item.PCIeDevice")) {
                    String locationCode = getLocationCode(entry);
                    String prettyName = getPrettyName(entry);
                    String inventoryPath = getInventoryPath(entry);
                    System.out.println(
                            (locationCode != null ? locationCode : "N/A") + " | " +
                                    (prettyName != null ? prettyName : "N/A") + " | " +
                                    inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.list-pcie-slots", value = "List all PCIe slot entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPcieSlots() {
        try {
            getAllEntries().forEach(entry -> {
                if (hasInterface(entry, "xyz.openbmc_project.Inventory.Item.PCIeSlot")) {
                    String locationCode = getLocationCode(entry);
                    String prettyName = getPrettyName(entry);
                    String inventoryPath = getInventoryPath(entry);
                    System.out.println(
                            (locationCode != null ? locationCode : "N/A") + " | " +
                                    (prettyName != null ? prettyName : "N/A") + " | " +
                                    inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== CABLE QUERIES ====================

    @ShellMethod(key = "vpd.list-cables", value = "List all cable entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListCables() {
        try {
            getAllEntries().forEach(entry -> {
                if (hasInterface(entry, "xyz.openbmc_project.Inventory.Item.Cable")) {
                    String prettyName = getPrettyName(entry);
                    String inventoryPath = getInventoryPath(entry);
                    System.out.println(
                            (prettyName != null ? prettyName : "N/A") + " | " + inventoryPath);
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== SUMMARY / STATISTICS ====================

    @ShellMethod(key = "vpd.summary", value = "Show a summary of the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSummary() {
        try {
            JsonNode root = fetchVpdData();
            int totalEepromPaths = 0;
            int totalEntries = 0;

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (value.isArray()) {
                    totalEepromPaths++;
                    int count = value.size();
                    totalEntries += count;
                    System.out.println("  " + entry.getKey() + " -> " + count + " entries");
                }
            }

            System.out.println("Total EEPROM paths: " + totalEepromPaths);
            System.out.println("Total inventory entries: " + totalEntries);
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    @ShellMethod(key = "vpd.table", value = "Show a tabular view of all VPD inventory entries")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdTable() {
        try {
            System.out.printf("%-30s %-40s %s%n", "LocationCode", "PrettyName", "InventoryPath");
            System.out.println("-".repeat(120));
            getAllEntries().forEach(entry -> {
                String locationCode = getLocationCode(entry);
                String prettyName = getPrettyName(entry);
                String inventoryPath = getInventoryPath(entry);
                System.out.printf("%-30s %-40s %s%n",
                        locationCode != null ? locationCode : "N/A",
                        prettyName != null ? prettyName : "N/A",
                        inventoryPath != null ? inventoryPath : "");
            });
        } catch (Exception e) {
            System.err.println("Error fetching VPD data: " + e.getMessage());
        }
    }

    // ==================== RAW GREP FALLBACK ====================

    @ShellMethod(key = "vpd.grep", value = "Raw grep search in the VPD inventory JSON file")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdGrep(@ShellOption(value = { "--pattern", "-p" }) String pattern) {
        scmd(String.format("grep -n '%s' %s", pattern, VPD_JSON));
    }

    // ==================== HELP ====================

    @ShellMethod(key = "vpd.help", value = "Show VPD commands help")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdHelp() {
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  VPD (Vital Product Data) Commands");
        System.out.println("═══════════════════════════════════════════════════════");

        System.out.println("\nBasic File Commands:");
        System.out.println("  vpd.show              - Display full VPD inventory JSON");
        System.out.println("  vpd.show-pretty       - Display VPD with pretty formatting");
        System.out.println("  vpd.summary           - Show summary statistics");
        System.out.println("  vpd.table             - Show tabular view of all entries");
        System.out.println("  vpd.refresh           - Clear cache and fetch fresh data");

        System.out.println("\nEEPROM Path Queries:");
        System.out.println("  vpd.list-eeprom-paths - List all EEPROM paths");

        System.out.println("\nInventory Path Queries:");
        System.out.println("  vpd.list-inventory-paths - List all inventory paths");
        System.out.println("  vpd.search-inventory-path -p <pattern> - Search inventory paths");

        System.out.println("\nLocation Code Queries:");
        System.out.println("  vpd.list-location-codes - List all location codes");
        System.out.println("  vpd.search-location-code -p <pattern> - Search location codes");

        System.out.println("\nPretty Name Queries:");
        System.out.println("  vpd.list-pretty-names - List all pretty names");
        System.out.println("  vpd.find-pretty-name -n <name> - Find entries by pretty name");

        System.out.println("\nInterface Queries:");
        System.out.println("  vpd.list-interfaces   - List all unique interface names");
        System.out.println("  vpd.find-by-interface -i <interface> - Find entries by interface");

        System.out.println("\nHardware Component Queries:");
        System.out.println("  vpd.list-connectors   - List all connector/port entries");
        System.out.println("  vpd.list-pcie-devices - List all PCIe device entries");
        System.out.println("  vpd.list-pcie-slots   - List all PCIe slot entries");
        System.out.println("  vpd.list-cables       - List all cable entries");

        System.out.println("\nUtility:");
        System.out.println("  vpd.grep -p <pattern> - Raw grep search in VPD JSON");
        System.out.println("  vpd.help              - Show this help message");

        System.out.println("\nExamples:");
        System.out.println("  vpd.table");
        System.out.println("  vpd.list-pcie-slots");
        System.out.println("  vpd.search-inventory-path -p pcieslot");
        System.out.println("  vpd.grep -p PCIe");
        System.out.println("═══════════════════════════════════════════════════════\n");
    }
}
