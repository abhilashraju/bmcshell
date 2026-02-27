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
public class ServiceCommands extends CommonCommands {
    @Component
    public static class ServiceProvider implements ValueProvider {
        public static List<String> serviceNames = new ArrayList<>();

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (serviceNames != null) {
                return serviceNames.stream()
                        .filter(name -> !name.startsWith("-"))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return null;
        }
    }

    String currentService;
    
    // Cache for service dependencies to improve performance
    private static Map<String, Map<String, Set<String>>> serviceDependencyCache = new HashMap<>();
    private static Map<String, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    protected ServiceCommands() throws IOException {

    }

    @ShellMethod(key = "service.list", value = "eg: service.list")
    @ShellMethodAvailability("availabilityCheck")
    void serviceList() throws IOException {
        serviceList(true);
    }

    void serviceList(boolean displayTable) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("systemctl list-units --type=service"));

            ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();

            runCommandShort(outputStream2, Util.fullMachineName(machine), userName, passwd,
                    String.format("ls -alhS /etc/systemd/system"));
            ByteArrayOutputStream outputStream3 = new ByteArrayOutputStream();
            runCommandShort(outputStream3, Util.fullMachineName(machine), userName, passwd,
                    String.format("ls -alhS /usr/lib/systemd/system"));

            ServiceProvider.serviceNames = extractServiceNamesFromSysctl(outputStream.toString());
            var filenames = parseFilenamesFromls(outputStream2.toString());
            filenames.stream().filter(a -> !ServiceProvider.serviceNames.contains(a))
                    .forEach(a -> ServiceProvider.serviceNames.add(a));
            var filenames2 = parseFilenamesFromls(outputStream3.toString());
            filenames2.stream().filter(a -> !ServiceProvider.serviceNames.contains(a))
                    .forEach(a -> ServiceProvider.serviceNames.add(a));
            
            // Display services in table format only if requested
            if (displayTable) {
                displayServicesTable(outputStream.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void displayServicesTable(String systemctlOutput) {
        List<ServiceInfo> services = parseServiceInfo(systemctlOutput);
        
        if (services.isEmpty()) {
            System.out.println("No services found.");
            return;
        }

        // Calculate column widths
        int unitWidth = Math.max(20, services.stream().mapToInt(s -> s.unit.length()).max().orElse(20));
        int loadWidth = 10;
        int activeWidth = 10;
        int subWidth = 10;
        int descWidth = Math.max(40, services.stream().mapToInt(s -> s.description.length()).max().orElse(40));
        
        // Limit description width to 60 characters for better readability
        descWidth = Math.min(descWidth, 60);

        // Print header
        String headerFormat = "%-" + unitWidth + "s  %-" + loadWidth + "s  %-" + activeWidth + "s  %-" + subWidth + "s  %-" + descWidth + "s%n";
        String rowFormat = "%-" + unitWidth + "s  %-" + loadWidth + "s  %-" + activeWidth + "s  %-" + subWidth + "s  %-" + descWidth + "s%n";
        
        System.out.printf(headerFormat, "UNIT", "LOAD", "ACTIVE", "SUB", "DESCRIPTION");
        System.out.println("=".repeat(unitWidth + loadWidth + activeWidth + subWidth + descWidth + 8));

        // Print services
        for (ServiceInfo service : services) {
            String desc = service.description.length() > descWidth
                ? service.description.substring(0, descWidth - 3) + "..."
                : service.description;
            System.out.printf(rowFormat, service.unit, service.load, service.active, service.sub, desc);
        }

        System.out.println("\nTotal services: " + services.size());
    }

    private List<ServiceInfo> parseServiceInfo(String systemctlOutput) {
        List<ServiceInfo> services = new ArrayList<>();
        String[] lines = systemctlOutput.split("\\R");
        boolean isHeader = true;

        for (String line : lines) {
            // Skip until we find the header line
            if (isHeader) {
                if (line.trim().startsWith("UNIT")) {
                    isHeader = false;
                }
                continue;
            }

            // Skip empty lines and footer lines
            if (line.trim().isEmpty() || line.contains("loaded units listed") || line.contains("To show all")) {
                continue;
            }

            // Parse service line
            ServiceInfo info = parseServiceLine(line);
            if (info != null) {
                services.add(info);
            }
        }

        return services;
    }

    private ServiceInfo parseServiceLine(String line) {
        // Remove bullet point if present
        line = line.replaceFirst("^\\s*●\\s*", "  ");
        
        // Split by whitespace, but preserve description
        String[] parts = line.trim().split("\\s+", 5);
        
        if (parts.length >= 4) {
            String unit = parts[0];
            String load = parts[1];
            String active = parts[2];
            String sub = parts[3];
            String description = parts.length > 4 ? parts[4] : "";
            
            return new ServiceInfo(unit, load, active, sub, description);
        }
        
        return null;
    }

    private static class ServiceInfo {
        String unit;
        String load;
        String active;
        String sub;
        String description;

        ServiceInfo(String unit, String load, String active, String sub, String description) {
            this.unit = unit;
            this.load = load;
            this.active = active;
            this.sub = sub;
            this.description = description;
        }
    }

    @ShellMethod(key = "service", value = "eg: service servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "") String s) throws IOException {

        if (ServiceProvider.serviceNames.isEmpty()) {
            serviceList(false);
        }

        List<String> matchingServices = ServiceProvider.serviceNames.stream().filter(nm -> {
            if (s == null || s.isEmpty())
                return true;
            else
                return nm.toLowerCase().contains(s.toLowerCase());
        }).collect(Collectors.toList());

        if (matchingServices.isEmpty()) {
            System.out.println("No matching services found.");
            return;
        }

        // If only one service matches, set it as current and show details
        if (matchingServices.size() == 1) {
            currentService = matchingServices.get(0);
            displayServiceDetails(currentService);
        } else {
            // Multiple matches - display them in a table with basic info
            displayMatchingServicesTable(matchingServices);
            if (!matchingServices.isEmpty()) {
                currentService = matchingServices.get(0);
            }
        }
    }

    private void displayServiceDetails(String serviceName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("systemctl status %s", serviceName));
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Service Details: " + serviceName);
            System.out.println("=".repeat(80));
            System.out.println(outputStream.toString());
            System.out.println("=".repeat(80));
            
            // Also get key properties
            ByteArrayOutputStream showStream = new ByteArrayOutputStream();
            runCommandShort(showStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("systemctl show %s --no-pager", serviceName));
            
            String showOutput = showStream.toString();
            Map<String, String> properties = parseServiceProperties(showOutput);
            
            System.out.println("\nKey Properties:");
            System.out.println("-".repeat(80));
            displayProperty(properties, "Description");
            displayProperty(properties, "LoadState");
            displayProperty(properties, "ActiveState");
            displayProperty(properties, "SubState");
            displayProperty(properties, "MainPID");
            displayProperty(properties, "ExecStart");
            displayProperty(properties, "ExecMainStartTimestamp");
            displayProperty(properties, "MemoryCurrent");
            displayProperty(properties, "CPUUsageNSec");
            System.out.println("-".repeat(80));
            
        } catch (Exception e) {
            System.err.println("Error fetching service details: " + e.getMessage());
        }
    }

    private Map<String, String> parseServiceProperties(String showOutput) {
        Map<String, String> properties = new HashMap<>();
        String[] lines = showOutput.split("\\R");
        
        for (String line : lines) {
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex);
                String value = line.substring(equalsIndex + 1);
                properties.put(key, value);
            }
        }
        
        return properties;
    }

    private void displayProperty(Map<String, String> properties, String key) {
        String value = properties.get(key);
        if (value != null && !value.isEmpty()) {
            // Format memory values
            if (key.equals("MemoryCurrent") && !value.equals("[not set]")) {
                try {
                    long bytes = Long.parseLong(value);
                    value = formatBytes(bytes);
                } catch (NumberFormatException e) {
                    // Keep original value
                }
            }
            // Format CPU time
            if (key.equals("CPUUsageNSec") && !value.equals("[not set]")) {
                try {
                    long nanoseconds = Long.parseLong(value);
                    double seconds = nanoseconds / 1_000_000_000.0;
                    value = String.format("%.2f seconds", seconds);
                } catch (NumberFormatException e) {
                    // Keep original value
                }
            }
            System.out.printf("  %-25s: %s%n", key, value);
        }
    }

    private void displayMatchingServicesTable(List<String> services) {
        try {
            System.out.println("\nFound " + services.size() + " matching service(s):");
            System.out.println("=".repeat(80));
            
            // Get status for each service
            List<ServiceStatusInfo> statusList = new ArrayList<>();
            for (String service : services) {
                ServiceStatusInfo info = getServiceStatus(service);
                if (info != null) {
                    statusList.add(info);
                }
            }
            
            // Display in table format
            if (!statusList.isEmpty()) {
                int nameWidth = Math.max(30, statusList.stream().mapToInt(s -> s.name.length()).max().orElse(30));
                int loadWidth = 10;
                int activeWidth = 10;
                int subWidth = 12;
                
                String headerFormat = "%-" + nameWidth + "s  %-" + loadWidth + "s  %-" + activeWidth + "s  %-" + subWidth + "s%n";
                String rowFormat = "%-" + nameWidth + "s  %-" + loadWidth + "s  %-" + activeWidth + "s  %-" + subWidth + "s%n";
                
                System.out.printf(headerFormat, "SERVICE", "LOAD", "ACTIVE", "SUB");
                System.out.println("-".repeat(nameWidth + loadWidth + activeWidth + subWidth + 6));
                
                for (ServiceStatusInfo info : statusList) {
                    System.out.printf(rowFormat, info.name, info.load, info.active, info.sub);
                }
                
                System.out.println("=".repeat(80));
                System.out.println("\nUse 'ro.service -s <exact_name>' to see detailed information for a specific service.");
            }
        } catch (Exception e) {
            System.err.println("Error displaying service table: " + e.getMessage());
        }
    }

    private ServiceStatusInfo getServiceStatus(String serviceName) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("systemctl show %s --property=LoadState,ActiveState,SubState", serviceName));
            
            String output = outputStream.toString();
            Map<String, String> props = parseServiceProperties(output);
            
            return new ServiceStatusInfo(
                serviceName,
                props.getOrDefault("LoadState", "unknown"),
                props.getOrDefault("ActiveState", "unknown"),
                props.getOrDefault("SubState", "unknown")
            );
        } catch (Exception e) {
            return new ServiceStatusInfo(serviceName, "error", "error", "error");
        }
    }

    private static class ServiceStatusInfo {
        String name;
        String load;
        String active;
        String sub;

        ServiceStatusInfo(String name, String load, String active, String sub) {
            this.name = name;
            this.load = load;
            this.active = active;
            this.sub = sub;
        }
    }

    @ShellMethod(key = "service.show", value = "eg: service.show servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_show(
            @ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s,
            @ShellOption(value = { "--reg", "-r" }, defaultValue = ".") String reg) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl show %s |grep -i %s", s, reg));

    }

    @ShellMethod(key = "service.status", value = "eg: service.status servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_status(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl status %s", s));
    }

    @ShellMethod(key = "service.start", value = "eg: service.start servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_start(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl start %s", s));
    }

    @ShellMethod(key = "service.stop", value = "eg: service.stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_stop(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl stop %s", s));
    }

    @ShellMethod(key = "service.restart", value = "eg: service.restart servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_restart(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl restart %s", s));
    }

    @ShellMethod(key = "service.log", value = "eg: service.log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_log(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("journalctl -u %s", s));
    }

    @ShellMethod(key = "service.cat", value = "eg: service.cat servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_cat(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl cat %s", s));
    }

    @ShellMethod(key = "service.enable", value = "eg: service.enable servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_enable(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl enable %s", s));
    }

    @ShellMethod(key = "service.dependencies", value = "eg: service.dependencies servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_dependencies(
            @ShellOption(value = { "--ser" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl list-dependencies %s", s));
    }

    @ShellMethod(key = "service.graph", value = "eg: service.graph [servicename] [--depth 2] [--nocache]")
    @ShellMethodAvailability("availabilityCheck")
    void service_graph(
            @ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s,
            @ShellOption(value = { "--depth", "-d" }, defaultValue = "2") int depth,
            @ShellOption(value = { "--reverse", "-r" }, defaultValue = "false") boolean reverse,
            @ShellOption(value = { "--nocache" }, defaultValue = "false") boolean noCache) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        if (s == null || s.isEmpty()) {
            System.out.println("Please specify a service name or use service to set current service");
            return;
        }
        currentService = s;
        
        try {
            String cacheKey = getCacheKey(s, depth, reverse);
            Map<String, Set<String>> serviceGraph = null;
            
            // Check cache if not disabled
            if (!noCache && isCacheValid(cacheKey)) {
                serviceGraph = serviceDependencyCache.get(cacheKey);
                System.out.println(ColorPrinter.cyan("Using cached data..."));
            }
            
            // Build graph if not in cache or cache disabled
            if (serviceGraph == null) {
                serviceGraph = buildServiceGraph(s, depth, reverse);
                // Store in cache
                serviceDependencyCache.put(cacheKey, serviceGraph);
                cacheTimestamps.put(cacheKey, System.currentTimeMillis());
                System.out.println(ColorPrinter.green("Data cached for future queries"));
            }
            
            displayServiceGraph(s, serviceGraph, reverse);
        } catch (Exception e) {
            System.err.println("Error building service graph: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @ShellMethod(key = "service.graph.clear", value = "Clear service graph cache")
    @ShellMethodAvailability("availabilityCheck")
    void service_graph_clear() {
        int size = serviceDependencyCache.size();
        serviceDependencyCache.clear();
        cacheTimestamps.clear();
        System.out.println(ColorPrinter.green("Cache cleared. Removed " + size + " cached entries."));
    }
    
    @ShellMethod(key = "service.graph.cache", value = "Show cache statistics")
    @ShellMethodAvailability("availabilityCheck")
    void service_graph_cache() {
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Service Graph Cache Statistics"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println("Total cached entries: " + serviceDependencyCache.size());
        System.out.println("Cache expiry time: " + (CACHE_EXPIRY_MS / 1000 / 60) + " minutes");
        
        if (!serviceDependencyCache.isEmpty()) {
            System.out.println("\n" + ColorPrinter.yellow("Cached Entries:"));
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
                String key = entry.getKey();
                long timestamp = entry.getValue();
                long ageSeconds = (currentTime - timestamp) / 1000;
                long remainingSeconds = (CACHE_EXPIRY_MS - (currentTime - timestamp)) / 1000;
                
                String status = remainingSeconds > 0 ?
                    ColorPrinter.green("Valid (" + remainingSeconds + "s remaining)") :
                    ColorPrinter.red("Expired");
                    
                System.out.println("  " + key + " - Age: " + ageSeconds + "s - " + status);
            }
        }
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }
    
    private String getCacheKey(String service, int depth, boolean reverse) {
        return service + "|" + depth + "|" + (reverse ? "R" : "F");
    }
    
    private boolean isCacheValid(String cacheKey) {
        if (!serviceDependencyCache.containsKey(cacheKey)) {
            return false;
        }
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return false;
        }
        long age = System.currentTimeMillis() - timestamp;
        return age < CACHE_EXPIRY_MS;
    }

    private Map<String, Set<String>> buildServiceGraph(String rootService, int maxDepth, boolean reverse) throws IOException {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        buildServiceGraphRecursive(rootService, graph, visited, 0, maxDepth, reverse);
        return graph;
    }

    private void buildServiceGraphRecursive(String service, Map<String, Set<String>> graph,
                                           Set<String> visited, int currentDepth, int maxDepth, boolean reverse) throws IOException {
        if (currentDepth > maxDepth || visited.contains(service)) {
            return;
        }
        
        visited.add(service);
        Set<String> dependencies = getServiceDependencies(service, reverse);
        graph.put(service, dependencies);
        
        if (currentDepth < maxDepth) {
            for (String dep : dependencies) {
                buildServiceGraphRecursive(dep, graph, visited, currentDepth + 1, maxDepth, reverse);
            }
        }
    }

    private Set<String> getServiceDependencies(String service, boolean reverse) throws IOException {
        Set<String> dependencies = new HashSet<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            String command = reverse ?
                String.format("systemctl list-dependencies --reverse --plain %s", service) :
                String.format("systemctl list-dependencies --plain %s", service);
                
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);
            String output = outputStream.toString();
            
            // Parse the output to extract service names
            String[] lines = output.split("\\R");
            for (String line : lines) {
                // Skip the first line (the service itself) and empty lines
                if (line.trim().isEmpty() || line.trim().equals(service)) {
                    continue;
                }
                
                // Remove tree characters and extract service name
                String cleaned = line.replaceAll("[│├└─●\\s]+", "").trim();
                if (!cleaned.isEmpty() && cleaned.endsWith(".service")) {
                    dependencies.add(cleaned);
                } else if (!cleaned.isEmpty() && !cleaned.contains(".")) {
                    // Some dependencies might not have .service extension
                    dependencies.add(cleaned);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching dependencies for " + service + ": " + e.getMessage());
        }
        
        return dependencies;
    }

    private void displayServiceGraph(String rootService, Map<String, Set<String>> graph, boolean reverse) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Service Dependency Graph" + (reverse ? " (Reverse)" : "")));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.yellow("Root Service: ") + rootService);
        System.out.println(ColorPrinter.yellow("Direction: ") +
                          (reverse ? "Services that depend on this service" : "Services this service depends on"));
        System.out.println(ColorPrinter.cyan("───────────────────────────────────────────────────────") + "\n");
        
        // Display graph in tree format
        Set<String> displayed = new HashSet<>();
        displayServiceNode(rootService, graph, displayed, "", true, reverse);
        
        // Display summary
        System.out.println("\n" + ColorPrinter.cyan("───────────────────────────────────────────────────────"));
        System.out.println(ColorPrinter.green("Summary:"));
        System.out.println("  Total services in graph: " + graph.size());
        int totalDeps = graph.values().stream().mapToInt(Set::size).sum();
        System.out.println("  Total relationships: " + totalDeps);
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════") + "\n");
    }

    private void displayServiceNode(String service, Map<String, Set<String>> graph,
                                    Set<String> displayed, String prefix, boolean isRoot, boolean reverse) {
        if (displayed.contains(service)) {
            System.out.println(ColorPrinter.magenta(service + " (already shown)"));
            return;
        }
        
        displayed.add(service);
        
        if (isRoot) {
            System.out.println(ColorPrinter.green("● " + service));
        } else {
            System.out.println(ColorPrinter.yellow(service));
        }
        
        Set<String> dependencies = graph.getOrDefault(service, new HashSet<>());
        if (!dependencies.isEmpty()) {
            List<String> depList = new ArrayList<>(dependencies);
            for (int i = 0; i < depList.size(); i++) {
                boolean isLast = (i == depList.size() - 1);
                String connector = isLast ? "└── " : "├── ";
                String childPrefix = prefix + (isLast ? "    " : "│   ");
                
                System.out.print(prefix + connector);
                displayServiceNode(depList.get(i), graph, displayed, childPrefix, false, reverse);
            }
        }
    }

    // Helper methods that need to be accessible from RemoteCommands
    List<String> extractServiceNamesFromSysctl(String output) {
        List<String> serviceNames = new ArrayList<>();
        String[] lines = output.split("\\R");
        
        for (String line : lines) {
            line = line.trim();
            // Skip header and footer lines
            if (line.isEmpty() || line.startsWith("UNIT") || line.contains("loaded units listed")) {
                continue;
            }
            
            // Remove bullet point if present
            line = line.replaceFirst("^●\\s*", "");
            
            // Extract service name (first word)
            String[] parts = line.split("\\s+");
            if (parts.length > 0 && parts[0].endsWith(".service")) {
                serviceNames.add(parts[0]);
            }
        }
        
        return serviceNames;
    }

    List<String> parseFilenamesFromls(String output) {
        List<String> filenames = new ArrayList<>();
        String[] lines = output.split("\\R");
        
        for (String line : lines) {
            // Skip total line and empty lines
            if (line.trim().isEmpty() || line.startsWith("total")) {
                continue;
            }
            
            // Parse ls -l output format
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 9) {
                // The filename is the last part (or parts if it contains spaces)
                String filename = parts[parts.length - 1];
                
                // Only include .service files
                if (filename.endsWith(".service")) {
                    filenames.add(filename);
                }
            }
        }
        
        return filenames;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}

// Made with Bob
