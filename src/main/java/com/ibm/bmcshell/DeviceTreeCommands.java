package com.ibm.bmcshell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class DeviceTreeCommands extends CommonCommands {
    @Component
    public static class DeviceTreeNodeProvider implements ValueProvider {
        public static List<String> deviceTreeNodes = new ArrayList<>();

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (deviceTreeNodes != null) {
                return deviceTreeNodes.stream()
                        .filter(name -> !name.startsWith("-"))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return null;
        }
    }

    String currentNode;

    // Cache for device tree structure to improve performance
    private static Map<String, Map<String, Object>> deviceTreeCache = new HashMap<>();
    private static Map<String, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    protected DeviceTreeCommands() throws IOException {

    }

    @ShellMethod(key = "dt.list", value = "eg: dt.list [--path /proc/device-tree/]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeList(
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/proc/device-tree/") String path)
            throws IOException {
        deviceTreeList(path, true);
    }

    void deviceTreeList(String path, boolean displayTable) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("find %s -maxdepth 3 -type d 2>/dev/null | head -n 100", path));

            DeviceTreeNodeProvider.deviceTreeNodes = extractNodePaths(outputStream.toString(), path);

            if (displayTable) {
                displayDeviceTreeTable(outputStream.toString(), path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void displayDeviceTreeTable(String findOutput, String basePath) {
        List<DeviceTreeNodeInfo> nodes = parseDeviceTreeNodes(findOutput, basePath);

        if (nodes.isEmpty()) {
            System.out.println("No device tree nodes found.");
            return;
        }

        // Calculate column widths
        int pathWidth = Math.max(50, nodes.stream().mapToInt(n -> n.path.length()).max().orElse(50));
        int depthWidth = 8;
        int typeWidth = 15;

        // Limit path width for better readability
        pathWidth = Math.min(pathWidth, 80);

        // Print header
        String headerFormat = "%-" + pathWidth + "s  %-" + depthWidth + "s  %-" + typeWidth + "s%n";
        String rowFormat = "%-" + pathWidth + "s  %-" + depthWidth + "s  %-" + typeWidth + "s%n";

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Device Tree Nodes"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.printf(headerFormat, "PATH", "DEPTH", "TYPE");
        System.out.println("=".repeat(pathWidth + depthWidth + typeWidth + 4));

        // Print nodes
        for (DeviceTreeNodeInfo node : nodes) {
            String displayPath = node.path.length() > pathWidth
                    ? "..." + node.path.substring(node.path.length() - pathWidth + 3)
                    : node.path;
            System.out.printf(rowFormat, displayPath, node.depth, node.type);
        }

        System.out.println("\nTotal nodes: " + nodes.size());
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    private List<DeviceTreeNodeInfo> parseDeviceTreeNodes(String findOutput, String basePath) {
        List<DeviceTreeNodeInfo> nodes = new ArrayList<>();
        String[] lines = findOutput.split("\\R");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.equals(basePath)) {
                continue;
            }

            String relativePath = line.replace(basePath, "").replaceFirst("^/", "");
            int depth = relativePath.isEmpty() ? 0 : relativePath.split("/").length;
            String type = guessNodeType(relativePath);

            nodes.add(new DeviceTreeNodeInfo(relativePath, depth, type));
        }

        return nodes;
    }

    private String guessNodeType(String path) {
        if (path.contains("cpu"))
            return "CPU";
        if (path.contains("memory"))
            return "Memory";
        if (path.contains("i2c"))
            return "I2C";
        if (path.contains("spi"))
            return "SPI";
        if (path.contains("gpio"))
            return "GPIO";
        if (path.contains("uart") || path.contains("serial"))
            return "Serial";
        if (path.contains("ethernet") || path.contains("eth"))
            return "Network";
        if (path.contains("usb"))
            return "USB";
        if (path.contains("pci"))
            return "PCI";
        if (path.contains("flash"))
            return "Flash";
        return "Device";
    }

    private static class DeviceTreeNodeInfo {
        String path;
        int depth;
        String type;

        DeviceTreeNodeInfo(String path, int depth, String type) {
            this.path = path;
            this.depth = depth;
            this.type = type;
        }
    }

    @ShellMethod(key = "dt.node", value = "eg: dt.node [nodepath]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeNode(
            @ShellOption(value = { "--node",
                    "-n" }, valueProvider = DeviceTreeNodeProvider.class, defaultValue = "") String node)
            throws IOException {

        if (DeviceTreeNodeProvider.deviceTreeNodes.isEmpty()) {
            deviceTreeList("/proc/device-tree/", false);
        }

        if (node == null || node.isEmpty()) {
            if (currentNode != null && !currentNode.isEmpty()) {
                node = currentNode;
            } else {
                System.out.println("Please specify a node path");
                return;
            }
        }

        currentNode = node;
        displayNodeDetails(node);
    }

    private void displayNodeDetails(String nodePath) {
        try {
            String fullPath = nodePath.startsWith("/proc/device-tree") ? nodePath : "/proc/device-tree/" + nodePath;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("ls -la %s 2>/dev/null", fullPath));

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Device Tree Node: ") + ColorPrinter.yellow(nodePath));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

            String output = outputStream.toString();
            if (output.isEmpty() || output.contains("No such file")) {
                System.out.println(ColorPrinter.red("Node not found: " + fullPath));
                return;
            }

            System.out.println(output);

            // Get properties
            ByteArrayOutputStream propsStream = new ByteArrayOutputStream();
            runCommandShort(propsStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("find %s -maxdepth 1 -type f 2>/dev/null", fullPath));

            String propsOutput = propsStream.toString();
            if (!propsOutput.isEmpty()) {
                System.out.println("\n" + ColorPrinter.yellow("Properties:"));
                System.out.println("-".repeat(80));
                displayNodeProperties(fullPath, propsOutput);
            }

            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error fetching node details: " + e.getMessage());
        }
    }

    private void displayNodeProperties(String nodePath, String propsOutput) {
        String[] propFiles = propsOutput.split("\\R");

        for (String propFile : propFiles) {
            propFile = propFile.trim();
            if (propFile.isEmpty())
                continue;

            String propName = propFile.substring(propFile.lastIndexOf('/') + 1);

            try {
                ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
                runCommandShort(valueStream, Util.fullMachineName(machine), userName, passwd,
                        String.format("cat %s 2>/dev/null | tr '\\0' ' ' | dd bs=1 count=200 2>/dev/null", propFile));

                String value = valueStream.toString().trim();
                if (!value.isEmpty()) {
                    // Format the output
                    if (value.length() > 60) {
                        value = value.substring(0, 57) + "...";
                    }
                    System.out.printf("  %-30s: %s%n", propName, value);
                }
            } catch (Exception e) {
                // Skip properties that can't be read
            }
        }
    }

    @ShellMethod(key = "dt.show", value = "eg: dt.show [nodepath] [--property name]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeShow(
            @ShellOption(value = { "--node",
                    "-n" }, valueProvider = DeviceTreeNodeProvider.class, defaultValue = "") String node,
            @ShellOption(value = { "--property", "-p" }, defaultValue = "") String property) {
        if (node == null || node.isEmpty()) {
            node = currentNode;
        }
        if (node == null || node.isEmpty()) {
            System.out.println("Please specify a node path");
            return;
        }
        currentNode = node;

        String fullPath = node.startsWith("/proc/device-tree") ? node : "/proc/device-tree/" + node;

        if (property != null && !property.isEmpty()) {
            scmd(String.format("cat %s/%s 2>/dev/null | hexdump -C | head -n 50", fullPath, property));
        } else {
            scmd(String.format(
                    "find %s -type f -exec sh -c 'echo \"File: {}\"; cat {} | tr \"\\0\" \" \" | dd bs=1 count=100 2>/dev/null; echo' \\; 2>/dev/null | head -n 100",
                    fullPath));
        }
    }

    @ShellMethod(key = "dt.search", value = "eg: dt.search --pattern cpu")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeSearch(
            @ShellOption(value = { "--pattern", "-p" }) String pattern,
            @ShellOption(value = { "--path" }, defaultValue = "/proc/device-tree/") String basePath) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("find %s -type d -iname '*%s*' 2>/dev/null | head -n 50", basePath, pattern));

            String output = outputStream.toString();
            if (output.isEmpty()) {
                System.out.println("No matching nodes found for pattern: " + pattern);
                return;
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Search Results for: ") + ColorPrinter.yellow(pattern));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

            String[] lines = output.split("\\R");
            int count = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    String relativePath = line.replace(basePath, "").replaceFirst("^/", "");
                    System.out.println("  " + ColorPrinter.green("→ ") + relativePath);
                    count++;
                }
            }

            System.out.println("\n" + ColorPrinter.yellow("Found " + count + " matching node(s)"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error searching device tree: " + e.getMessage());
        }
    }

    @ShellMethod(key = "dt.compatible", value = "eg: dt.compatible [nodepath]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeCompatible(
            @ShellOption(value = { "--node",
                    "-n" }, valueProvider = DeviceTreeNodeProvider.class, defaultValue = "") String node) {
        if (node == null || node.isEmpty()) {
            node = currentNode;
        }
        if (node == null || node.isEmpty()) {
            System.out.println("Please specify a node path");
            return;
        }
        currentNode = node;

        String fullPath = node.startsWith("/proc/device-tree") ? node : "/proc/device-tree/" + node;

        scmd(String.format("cat %s/compatible 2>/dev/null | tr '\\0' '\\n'", fullPath));
    }

    @ShellMethod(key = "dt.tree", value = "eg: dt.tree [--path /proc/device-tree] [--depth 3]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeTree(
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/proc/device-tree/") String path,
            @ShellOption(value = { "--depth", "-d" }, defaultValue = "3") int depth) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("tree -L %d -F %s 2>/dev/null || find %s -maxdepth %d -type d 2>/dev/null | sort",
                            depth, path, path, depth));

            String output = outputStream.toString();
            if (output.isEmpty()) {
                System.out.println("No device tree structure found");
                return;
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Device Tree Structure"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(output);
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error displaying device tree: " + e.getMessage());
        }
    }

    @ShellMethod(key = "dt.prop", value = "eg: dt.prop --node [nodepath] --property [propname]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeProperty(
            @ShellOption(value = { "--node",
                    "-n" }, valueProvider = DeviceTreeNodeProvider.class, defaultValue = "") String node,
            @ShellOption(value = { "--property", "-p" }) String property,
            @ShellOption(value = { "--format", "-f" }, defaultValue = "hex") String format) {
        if (node == null || node.isEmpty()) {
            node = currentNode;
        }
        if (node == null || node.isEmpty()) {
            System.out.println("Please specify a node path");
            return;
        }
        currentNode = node;

        String fullPath = node.startsWith("/proc/device-tree") ? node : "/proc/device-tree/" + node;

        String propPath = fullPath + "/" + property;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String command;

            switch (format.toLowerCase()) {
                case "hex":
                    command = String.format("hexdump -C %s 2>/dev/null", propPath);
                    break;
                case "string":
                    command = String.format("cat %s 2>/dev/null | tr '\\0' '\\n'", propPath);
                    break;
                case "raw":
                    command = String.format("cat %s 2>/dev/null", propPath);
                    break;
                default:
                    command = String.format("hexdump -C %s 2>/dev/null", propPath);
            }

            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);

            String output = outputStream.toString();
            if (output.isEmpty()) {
                System.out.println(ColorPrinter.red("Property not found: " + property));
                return;
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Property: ") + ColorPrinter.yellow(property));
            System.out.println(ColorPrinter.cyan("  Node: ") + ColorPrinter.yellow(node));
            System.out.println(ColorPrinter.cyan("  Format: ") + ColorPrinter.yellow(format));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(output);
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error reading property: " + e.getMessage());
        }
    }

    @ShellMethod(key = "dt.grep", value = "eg: dt.grep --pattern crypto [--property compatible]")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeGrep(
            @ShellOption(value = { "--pattern", "-p" }) String pattern,
            @ShellOption(value = { "--property" }, defaultValue = "") String property,
            @ShellOption(value = { "--path" }, defaultValue = "/proc/device-tree/") String basePath) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String command;

            if (property != null && !property.isEmpty()) {
                // Search in specific property files
                command = String.format(
                        "find %s -type f -name '%s' -exec sh -c 'echo \"Node: $(dirname {})\"; cat {} | tr \"\\0\" \" \" | grep -i \"%s\" && echo' \\; 2>/dev/null | head -n 100",
                        basePath, property, pattern);
            } else {
                // Search in all properties and node names
                command = String.format(
                        "find %s -type d -iname '*%s*' 2>/dev/null | head -n 50",
                        basePath, pattern);
            }

            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);

            String output = outputStream.toString();
            if (output.isEmpty()) {
                System.out.println("No matches found for pattern: " + pattern);
                return;
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Grep Results for: ") + ColorPrinter.yellow(pattern));
            if (!property.isEmpty()) {
                System.out.println(ColorPrinter.cyan("  Property: ") + ColorPrinter.yellow(property));
            }
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

            String[] lines = output.split("\\R");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    if (line.startsWith("Node:")) {
                        System.out.println("\n" + ColorPrinter.green(line));
                    } else {
                        String relativePath = line.replace(basePath, "").replaceFirst("^/", "");
                        System.out.println("  " + ColorPrinter.yellow("→ ") + relativePath);
                    }
                }
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error searching device tree: " + e.getMessage());
        }
    }

    @ShellMethod(key = "dt.detail", value = "eg: dt.detail --node crypto@12c1e000")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeDetail(
            @ShellOption(value = { "--node",
                    "-n" }, valueProvider = DeviceTreeNodeProvider.class, defaultValue = "") String node) {
        if (node == null || node.isEmpty()) {
            node = currentNode;
        }
        if (node == null || node.isEmpty()) {
            System.out.println("Please specify a node path");
            return;
        }
        currentNode = node;

        String fullPath = node.startsWith("/proc/device-tree") ? node : "/proc/device-tree/" + node;

        try {
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Detailed View: ") + ColorPrinter.yellow(node));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

            // Get all properties
            ByteArrayOutputStream propsStream = new ByteArrayOutputStream();
            runCommandShort(propsStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("find %s -maxdepth 1 -type f 2>/dev/null", fullPath));

            String propsOutput = propsStream.toString();
            if (propsOutput.isEmpty()) {
                System.out.println(ColorPrinter.red("Node not found or has no properties"));
                return;
            }

            String[] propFiles = propsOutput.split("\\R");
            Map<String, String> properties = new LinkedHashMap<>();

            // Read all properties
            for (String propFile : propFiles) {
                propFile = propFile.trim();
                if (propFile.isEmpty())
                    continue;

                String propName = propFile.substring(propFile.lastIndexOf('/') + 1);

                try {
                    ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
                    runCommandShort(valueStream, Util.fullMachineName(machine), userName, passwd,
                            String.format("cat %s 2>/dev/null | tr '\\0' ' ' | dd bs=1 count=500 2>/dev/null",
                                    propFile));

                    String value = valueStream.toString().trim();
                    if (!value.isEmpty()) {
                        properties.put(propName, value);
                    }
                } catch (Exception e) {
                    // Skip properties that can't be read
                }
            }

            // Display properties in organized sections
            displayPropertySection("Basic Information", properties,
                    new String[] { "compatible", "device_type", "model", "name", "status" });

            displayPropertySection("Hardware Addresses", properties,
                    new String[] { "reg", "ranges", "dma-ranges" });

            displayPropertySection("Interrupts", properties,
                    new String[] { "interrupts", "interrupt-parent", "interrupt-controller", "interrupt-cells" });

            displayPropertySection("Clocks & Power", properties,
                    new String[] { "clocks", "clock-names", "clock-frequency", "power-domains" });

            displayPropertySection("Crypto Specific", properties,
                    new String[] { "algorithm-support", "crypto-capabilities", "max-key-size", "dma-coherent" });

            // Display remaining properties
            System.out.println("\n" + ColorPrinter.yellow("Other Properties:"));
            System.out.println("-".repeat(80));
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String value = entry.getValue();
                if (value.length() > 100) {
                    value = value.substring(0, 97) + "...";
                }
                System.out.printf("  %-30s: %s%n", entry.getKey(), value);
            }

            // Get child nodes
            ByteArrayOutputStream childStream = new ByteArrayOutputStream();
            runCommandShort(childStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("find %s -mindepth 1 -maxdepth 1 -type d 2>/dev/null", fullPath));

            String childOutput = childStream.toString();
            if (!childOutput.isEmpty()) {
                System.out.println("\n" + ColorPrinter.yellow("Child Nodes:"));
                System.out.println("-".repeat(80));
                String[] children = childOutput.split("\\R");
                for (String child : children) {
                    if (!child.trim().isEmpty()) {
                        String childName = child.substring(child.lastIndexOf('/') + 1);
                        System.out.println("  " + ColorPrinter.green("→ ") + childName);
                    }
                }
            }

            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        } catch (Exception e) {
            System.err.println("Error fetching detailed view: " + e.getMessage());
        }
    }

    private void displayPropertySection(String sectionName, Map<String, String> properties, String[] propNames) {
        boolean hasAnyProp = false;
        for (String propName : propNames) {
            if (properties.containsKey(propName)) {
                hasAnyProp = true;
                break;
            }
        }

        if (!hasAnyProp) {
            return;
        }

        System.out.println("\n" + ColorPrinter.yellow(sectionName + ":"));
        System.out.println("-".repeat(80));

        for (String propName : propNames) {
            if (properties.containsKey(propName)) {
                String value = properties.get(propName);
                if (value.length() > 100) {
                    value = value.substring(0, 97) + "...";
                }
                System.out.printf("  %-30s: %s%n", propName, value);
                properties.remove(propName); // Remove so it's not displayed again
            }
        }
    }

    @ShellMethod(key = "dt.compare", value = "eg: dt.compare --nodes crypto@12c1e000,crypto@12080000")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeCompare(
            @ShellOption(value = { "--nodes", "-n" }) String nodes) {
        String[] nodeArray = nodes.split(",");
        if (nodeArray.length < 2) {
            System.out.println("Please specify at least 2 nodes separated by comma");
            return;
        }

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Comparing Device Tree Nodes"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        Map<String, Map<String, String>> allNodeProps = new LinkedHashMap<>();
        Set<String> allPropNames = new HashSet<>();

        // Collect properties from all nodes
        for (String node : nodeArray) {
            node = node.trim();
            String fullPath = node.startsWith("/proc/device-tree") ? node : "/proc/device-tree/" + node;

            Map<String, String> nodeProps = new LinkedHashMap<>();

            try {
                ByteArrayOutputStream propsStream = new ByteArrayOutputStream();
                runCommandShort(propsStream, Util.fullMachineName(machine), userName, passwd,
                        String.format("find %s -maxdepth 1 -type f 2>/dev/null", fullPath));

                String propsOutput = propsStream.toString();
                String[] propFiles = propsOutput.split("\\R");

                for (String propFile : propFiles) {
                    propFile = propFile.trim();
                    if (propFile.isEmpty())
                        continue;

                    String propName = propFile.substring(propFile.lastIndexOf('/') + 1);
                    allPropNames.add(propName);

                    try {
                        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
                        runCommandShort(valueStream, Util.fullMachineName(machine), userName, passwd,
                                String.format("cat %s 2>/dev/null | tr '\\0' ' ' | dd bs=1 count=200 2>/dev/null",
                                        propFile));

                        String value = valueStream.toString().trim();
                        if (!value.isEmpty()) {
                            nodeProps.put(propName, value);
                        }
                    } catch (Exception e) {
                        // Skip
                    }
                }

                allNodeProps.put(node, nodeProps);

            } catch (Exception e) {
                System.err.println("Error reading node: " + node);
            }
        }

        // Display comparison table
        System.out.println("\n" + ColorPrinter.yellow("Property Comparison:"));
        System.out.println("-".repeat(120));

        // Header
        System.out.printf("%-30s", "Property");
        for (String node : nodeArray) {
            System.out.printf("  %-35s", node.trim().substring(Math.max(0, node.trim().length() - 35)));
        }
        System.out.println();
        System.out.println("=".repeat(120));

        // Properties
        for (String propName : allPropNames) {
            System.out.printf("%-30s", propName);

            for (String node : nodeArray) {
                node = node.trim();
                Map<String, String> nodeProps = allNodeProps.get(node);
                String value = nodeProps.getOrDefault(propName, "-");
                if (value.length() > 35) {
                    value = value.substring(0, 32) + "...";
                }
                System.out.printf("  %-35s", value);
            }
            System.out.println();
        }

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "dt.cache.clear", value = "Clear device tree cache")
    @ShellMethodAvailability("availabilityCheck")
    void deviceTreeCacheClear() {
        int size = deviceTreeCache.size();
        deviceTreeCache.clear();
        cacheTimestamps.clear();
        DeviceTreeNodeProvider.deviceTreeNodes.clear();
        System.out.println(ColorPrinter.green("Cache cleared. Removed " + size + " cached entries."));
    }

    // Helper methods
    private List<String> extractNodePaths(String output, String basePath) {
        List<String> nodePaths = new ArrayList<>();
        String[] lines = output.split("\\R");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.equals(basePath)) {
                continue;
            }

            String relativePath = line.replace(basePath, "").replaceFirst("^/", "");
            if (!relativePath.isEmpty()) {
                nodePaths.add(relativePath);
            }
        }

        return nodePaths;
    }
}

// Made with Bob