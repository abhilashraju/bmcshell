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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.RemoteCommands.ServiceProvider;
import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class RemoteCommands extends CommonCommands {
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
    
    // Monitoring state
    private String monitoringService = null;
    private String monitorLogFile = null;
    private String monitorPidFile = null;

    protected RemoteCommands() throws IOException {

    }

    @ShellMethod(key = "ro.ls", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void ls(@ShellOption(valueProvider = RemoteFileCompleter.class) String path) {
        scmd(String.format("ls -alhS %s", path));
    }

    @ShellMethod(key = "ro.mv", value = "eg: ro.mv source dest")
    @ShellMethodAvailability("availabilityCheck")
    void mv(@ShellOption(valueProvider = RemoteFileCompleter.class) String source,
            @ShellOption(valueProvider = RemoteFileCompleter.class) String dest) {
        scmd(String.format("mv %s %s", source, dest));
    }

    @ShellMethod(key = "ro.cmd", value = "eg: ro.cmd command")
    @ShellMethodAvailability("availabilityCheck")
    void cmd(String cmd) {
        scmd(cmd);
    }

    @ShellMethod(key = "ro.cat", value = "eg: ro.cat filepath")
    @ShellMethodAvailability("availabilityCheck")
    void cat(@ShellOption(valueProvider = RemoteFileCompleter.class) String p) {
        scmd(String.format("cat %s", p));
    }

    @ShellMethod(key = "ro.service.list", value = "eg: ro.service.list")
    @ShellMethodAvailability("availabilityCheck")
    void serviceList() throws IOException {
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
            System.out.println("Services fetched");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ShellMethod(key = "ro.service", value = "eg: ro.service servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "") String s) throws IOException {

        if (ServiceProvider.serviceNames.isEmpty()) {
            serviceList();
        }

        ServiceProvider.serviceNames.stream().filter(nm -> {
            if (s == null || s.isEmpty())
                return true;
            else
                return nm.toLowerCase().contains(s.toLowerCase());
        }).forEach(nm -> {
            System.out.println(nm);
            currentService = nm;
        });

    }

    @ShellMethod(key = "ro.service.show", value = "eg: ro.service_show servicename")
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

    @ShellMethod(key = "ro.service.status", value = "eg: ro.service_status servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_status(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl status %s", s));
    }

    @ShellMethod(key = "ro.service.start", value = "eg: ro.service_start servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_start(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl start %s", s));
    }

    @ShellMethod(key = "ro.service.stop", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_stop(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl stop %s", s));
    }

    @ShellMethod(key = "ro.service.restart", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_restart(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl restart %s", s));
    }

    @ShellMethod(key = "ro.service.log", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_log(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("journalctl -u %s", s));
    }

    @ShellMethod(key = "ro.service.cat", value = "eg: ro.service.cat servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_cat(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl cat %s", s));
    }

    @ShellMethod(key = "ro.service.enable", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_enable(@ShellOption(value = { "--ser",
            "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl enable %s", s));
    }

    @ShellMethod(key = "ro.service.dependencies", value = "eg: ro.service.dependencies servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_dependencies(
            @ShellOption(value = { "--ser" }, valueProvider = ServiceProvider.class, defaultValue = "") String s) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        currentService = s;
        scmd(String.format("systemctl list-dependencies %s", s));
    }

    @ShellMethod(key = "ro.service.graph", value = "eg: ro.service.graph [servicename] [--depth 2] [--nocache]")
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
            System.out.println("Please specify a service name or use ro.service to set current service");
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
    
    @ShellMethod(key = "ro.service.graph.clear", value = "Clear service graph cache")
    @ShellMethodAvailability("availabilityCheck")
    void service_graph_clear() {
        int size = serviceDependencyCache.size();
        serviceDependencyCache.clear();
        cacheTimestamps.clear();
        System.out.println(ColorPrinter.green("Cache cleared. Removed " + size + " cached entries."));
    }
    
    @ShellMethod(key = "ro.service.graph.cache", value = "Show cache statistics")
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
            System.out.println(prefix + ColorPrinter.magenta(service + " (already shown)"));
            return;
        }
        
        displayed.add(service);
        
        if (isRoot) {
            System.out.println(ColorPrinter.green("● " + service));
        } else {
            System.out.println(prefix + ColorPrinter.yellow(service));
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

    @ShellMethod(key = "ro.find", value = "eg: ro.find filename [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void findFile(String filename,
            @ShellOption(value = { "--path", "-p" }, valueProvider = RemoteFileCompleter.class, defaultValue = "/") String path) {
        scmd(String.format("find %s -iname *%s*", path, filename));
    }

    @ShellMethod(key = "ro.grep", value = "eg: ro.grep pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void grep(String pattern,
            @ShellOption(value = { "--path", "-p" }, valueProvider = RemoteFileCompleter.class, defaultValue = "/") String path) {
        scmd(String.format("grep -nr %s %s", pattern, path));
    }

    @ShellMethod(key = "ro.running", value = "eg: ro.running pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void running(String pattern,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("ps | grep %s", pattern));
    }

    @ShellMethod(key = "ro.makefile", value = "eg: ro.makefile path content")
    @ShellMethodAvailability("availabilityCheck")
    void makeFile(@ShellOption(valueProvider = RemoteFileCompleter.class) String path, String data) {
        scmd(String.format("mkdir -p %s", path.substring(0, path.lastIndexOf('/'))));
        scmd(String.format("chmod 777 %s;echo %s > %s", path, data, path));
        scmd(String.format("ls -lh %s", path));
    }

    @ShellMethod(key = "ro.digest", value = "eg: ro.digest path to file")
    @ShellMethodAvailability("availabilityCheck")
    void digest(@ShellOption(valueProvider = RemoteFileCompleter.class) String path) {
        scmd(String.format("openssl dgst -sha256 %s", path));
    }

    @ShellMethod(key = "ro.ping", value = "eg: ro.ping ip")
    @ShellMethodAvailability("availabilityCheck")
    void ping(String ip) {
        scmd(String.format("ping -c 1 %s", ip));
    }
    @ShellMethod(key = "ro.mem.stat", value = "eg: ro.mem.stat processname - Display memory statistics from /proc/<pid>/statm")
    @ShellMethodAvailability("availabilityCheck")
    void mem_stat(String processname) {
        try {
            // Get connection details
            String machine = CommonCommands.machine;
            String userName = CommonCommands.getUserName();
            String passwd = CommonCommands.getPasswd();
            
            // Step 1: Find the process ID
            ByteArrayOutputStream pidOutput = new ByteArrayOutputStream();
            String pidCommand = String.format("pgrep -f '%s' | head -n 1", processname);
            runCommandShort(pidOutput, Util.fullMachineName(machine), userName, passwd, pidCommand);
            
            String pid = pidOutput.toString().trim();
            if (pid.isEmpty()) {
                System.out.println("Error: Process '" + processname + "' not found");
                return;
            }
            
            System.out.println("Process: " + processname + " (PID: " + pid + ")");
            System.out.println("======================================");
            
            // Step 2: Read /proc/<pid>/statm
            ByteArrayOutputStream statmOutput = new ByteArrayOutputStream();
            String statmCommand = String.format("cat /proc/%s/statm", pid);
            runCommandShort(statmOutput, Util.fullMachineName(machine), userName, passwd, statmCommand);
            
            String statmData = statmOutput.toString().trim();
            if (statmData.isEmpty()) {
                System.out.println("Error: Could not read /proc/" + pid + "/statm");
                return;
            }
            
            // Parse and display the statm values
            String[] values = statmData.split("\\s+");
            String[] headers = {
                "size     - Total program size (pages)",
                "resident - Resident set size (pages)",
                "shared   - Shared pages",
                "text     - Text (code) pages",
                "lib      - Library pages (unused since Linux 2.6)",
                "data     - Data + stack pages",
                "dt       - Dirty pages (unused since Linux 2.6)"
            };
            
            System.out.println("\nField       Value      Description");
            System.out.println("--------------------------------------");
            for (int i = 0; i < Math.min(values.length, headers.length); i++) {
                String[] parts = headers[i].split(" - ");
                System.out.printf("%-11s %-10s %s%n", parts[0].trim(), values[i], parts.length > 1 ? parts[1] : "");
            }
            
            System.out.println("\nNote: Values are in pages. Page size is typically 4096 bytes (4 KB)");
            
            // Display human-readable format
            if (values.length >= 2) {
                try {
                    long totalPages = Long.parseLong(values[0]);
                    long residentPages = Long.parseLong(values[1]);
                    long totalBytes = totalPages * 4096;
                    long residentBytes = residentPages * 4096;
                    
                    System.out.println("\nHuman-readable format:");
                    System.out.printf("  Total size: %s%n", formatBytes(totalBytes));
                    System.out.printf("  Resident:   %s%n", formatBytes(residentBytes));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    public static List<String> extractServiceNamesFromSysctl(String systemctlOutput) {
        List<String> serviceNames = new ArrayList<>();
        // Split the entire output into individual lines
        String[] lines = systemctlOutput.split("\\R");
        boolean isHeader = true;

        for (String line : lines) {
            // Skip the header line and any empty lines
            if (isHeader) {
                if (line.trim().startsWith("UNIT")) {
                    isHeader = false;
                }
                continue;
            }

            // A regular expression is a good approach for robust parsing
            // This pattern looks for an optional bullet point (●) followed by a non-space
            // group.
            Pattern pattern = Pattern.compile("^\\s*(?<serviceName>\\S+)\\s.*");
            if (line.contains("●")) {
                pattern = Pattern.compile("^\\s*\\S*\\s*(?<serviceName>\\S+)\\s.*");
            }
            Matcher matcher = pattern.matcher(line);

            if (matcher.matches()) {
                // The first non-whitespace token is the service name.
                String serviceName = matcher.group("serviceName");
                serviceNames.add(serviceName);
            }
        }

        return serviceNames;
    }

    public static List<String> parseFilenamesFromls(String lsOutput) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(lsOutput))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("total")) {
                    continue; // skip empty or "total" lines
                }

                // Split by whitespace into at most 9 parts
                String[] parts = line.split("\\s+", 9);

                if (parts.length >= 9) {
                    // For symlinks, keep only the part before "->"
                    String name = parts[8].split(" -> ")[0];
                    filenames.add(name);
                }
            }
        }
        return filenames;
    }

    @ShellMethod(key = "ro.monitor.start", value = "eg: ro.monitor.start servicename [--interval 5]")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_start(
            @ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class, defaultValue = "") String s,
            @ShellOption(value = { "--interval", "-i" }, defaultValue = "5") int interval) {
        if (s == null || s.isEmpty()) {
            s = currentService;
        }
        if (s == null || s.isEmpty()) {
            System.out.println(ColorPrinter.red("Please specify a service name"));
            return;
        }
        
        if (monitoringService != null) {
            System.out.println(ColorPrinter.yellow("Already monitoring service: " + monitoringService));
            System.out.println("Stop current monitoring with ro.monitor.stop before starting a new one");
            return;
        }
        
        currentService = s;
        
        // Sanitize service name for file paths (replace special chars with underscores)
        String sanitizedName = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        String tempPidFile = "/tmp/monitor_" + sanitizedName + ".pid";
        
        // Clean up any existing monitoring process for this service
        try {
            System.out.println(ColorPrinter.cyan("Checking for existing monitoring processes..."));
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            String cleanupCmd = String.format(
                "# Kill all service_monitor scripts for this service\n" +
                "PIDS=$(ps | grep 'service_monitor.*%s' | grep -v grep | awk '{print $1}');\n" +
                "if [ -n \"$PIDS\" ]; then\n" +
                "  echo \"Stopping $(echo $PIDS | wc -w) old monitoring process(es)\";\n" +
                "  for pid in $PIDS; do kill $pid 2>/dev/null; done;\n" +
                "  sleep 1;\n" +
                "fi;\n" +
                "# Clean up files\n" +
                "rm -f %s /tmp/service_monitor_*.sh /tmp/monitor_%s_*.csv /tmp/monitor_debug_*.log 2>/dev/null;\n" +
                "echo 'Cleanup complete'",
                sanitizedName, tempPidFile, sanitizedName
            );
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
            String cleanupResult = cleanupStream.toString().trim();
            if (cleanupResult.contains("Stopping")) {
                System.out.println(ColorPrinter.yellow("  " + cleanupResult));
            }
        } catch (Exception e) {
            System.out.println(ColorPrinter.yellow("  Warning: Cleanup check failed: " + e.getMessage()));
        }
        
        monitoringService = s;
        monitorLogFile = "/tmp/monitor_" + sanitizedName + "_" + System.currentTimeMillis() + ".csv";
        monitorPidFile = tempPidFile;
        
        try {
            // Read the monitoring script from resources
            String scriptContent = new String(
                getClass().getResourceAsStream("/service_monitor.sh").readAllBytes()
            );
            
            // Upload script to remote machine
            String remoteScriptPath = "/tmp/service_monitor_" + System.currentTimeMillis() + ".sh";
            ByteArrayOutputStream uploadStream = new ByteArrayOutputStream();
            
            // Create the script on remote machine
            String createScriptCmd = String.format(
                "cat > %s << 'EOFSCRIPT'\n%s\nEOFSCRIPT\nchmod +x %s",
                remoteScriptPath, scriptContent, remoteScriptPath
            );
            
            runCommandShort(uploadStream, Util.fullMachineName(machine), userName, passwd, createScriptCmd);
            
            // Start monitoring in background - pass sanitized name for PID file
            String startCmd = String.format(
                "nohup %s '%s' '%s' %d '%s' > /dev/null 2>&1 &",
                remoteScriptPath, s, monitorLogFile, interval, monitorPidFile
            );
            
            ByteArrayOutputStream startStream = new ByteArrayOutputStream();
            runCommandShort(startStream, Util.fullMachineName(machine), userName, passwd, startCmd);
            
            System.out.println(ColorPrinter.cyan("Starting monitoring process..."));
            
            // Wait for the background process to initialize and write first entry
            Thread.sleep(3000);
            
            // Verify the monitoring process started
            ByteArrayOutputStream verifyStream = new ByteArrayOutputStream();
            String verifyCmd = String.format(
                "if [ -f %s ]; then " +
                "  MONITOR_PID=$(cat %s); " +
                "  if kill -0 $MONITOR_PID 2>/dev/null; then " +
                "    echo 'RUNNING'; " +
                "  else " +
                "    echo 'PID_FILE_EXISTS_BUT_PROCESS_DEAD'; " +
                "  fi; " +
                "else " +
                "  echo 'PID_FILE_NOT_FOUND'; " +
                "fi",
                monitorPidFile, monitorPidFile
            );
            
            runCommandShort(verifyStream, Util.fullMachineName(machine), userName, passwd, verifyCmd);
            
            String verifyResult = verifyStream.toString().trim();
            
            if (verifyResult.contains("RUNNING")) {
                // Also verify log file is being created and check if process is actually running
                ByteArrayOutputStream logCheckStream = new ByteArrayOutputStream();
                runCommandShort(logCheckStream, Util.fullMachineName(machine), userName, passwd,
                    "test -f " + monitorLogFile + " && (wc -l " + monitorLogFile + "; tail -2 " + monitorLogFile + ") || echo '0'");
                
                String logStatus = logCheckStream.toString();
                int lineCount = 0;
                try {
                    String firstLine = logStatus.split("\n")[0].trim();
                    lineCount = Integer.parseInt(firstLine.split("\\s+")[0]);
                } catch (Exception e) {
                    // Ignore parsing errors
                }
                
                boolean processNotRunning = logStatus.contains(",N/A,0,0,0,0,0,stopped") ||
                                           logStatus.contains(",N/A,0,0,0,0,0,not_running");
                
                // Check if log file has at least 2 lines (header + data)
                if (lineCount < 2) {
                    System.out.println(ColorPrinter.yellow("⚠ Monitoring process started but log file not yet populated"));
                    System.out.println(ColorPrinter.yellow("  Waiting for first data entry..."));
                    Thread.sleep(interval * 1000 + 1000); // Wait one interval plus buffer
                    
                    // Re-check
                    ByteArrayOutputStream recheckStream = new ByteArrayOutputStream();
                    runCommandShort(recheckStream, Util.fullMachineName(machine), userName, passwd,
                        "test -f " + monitorLogFile + " && wc -l " + monitorLogFile + " || echo '0'");
                    String recheckResult = recheckStream.toString().trim();
                    try {
                        lineCount = Integer.parseInt(recheckResult.split("\\s+")[0]);
                    } catch (Exception e) {
                        lineCount = 0;
                    }
                }
                
                System.out.println(ColorPrinter.green("✓ Monitoring started successfully for service: " + s));
                System.out.println(ColorPrinter.cyan("  Interval: " + interval + " seconds"));
                System.out.println(ColorPrinter.cyan("  Log file: " + monitorLogFile));
                System.out.println(ColorPrinter.cyan("  PID file: " + monitorPidFile));
                System.out.println(ColorPrinter.cyan("  Data entries: " + Math.max(0, lineCount - 1)));
                
                if (processNotRunning) {
                    System.out.println(ColorPrinter.yellow("  ⚠ Warning: Target service '" + s + "' is not currently running"));
                    System.out.println(ColorPrinter.yellow("     Monitoring will show 'stopped' or 'not_running' until the service starts"));
                    System.out.println(ColorPrinter.yellow("     The monitor script will continue running and capture data when service starts"));
                }
                
                System.out.println(ColorPrinter.yellow("\nUse 'ro.monitor.stop' to stop monitoring and view results"));
            } else {
                System.out.println(ColorPrinter.red("✗ Failed to start monitoring"));
                System.out.println(ColorPrinter.red("  Verification result: " + verifyResult));
                
                // Debug: Check what went wrong
                ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
                runCommandShort(debugStream, Util.fullMachineName(machine), userName, passwd,
                    "ls -la /tmp/monitor_* 2>&1 | tail -10");
                System.out.println(ColorPrinter.yellow("  Files in /tmp:\n" + debugStream.toString()));
                
                // Show debug log if it exists - match the pattern used in the script
                ByteArrayOutputStream debugLogStream = new ByteArrayOutputStream();
                runCommandShort(debugLogStream, Util.fullMachineName(machine), userName, passwd,
                    "ls /tmp/monitor_debug_*.log 2>/dev/null | tail -1 | xargs cat 2>/dev/null || echo 'No debug log found'");
                System.out.println(ColorPrinter.yellow("  Debug log:\n" + debugLogStream.toString()));
                
                // Also check if CSV file has data
                if (monitorLogFile != null) {
                    ByteArrayOutputStream csvCheckStream = new ByteArrayOutputStream();
                    runCommandShort(csvCheckStream, Util.fullMachineName(machine), userName, passwd,
                        "test -f " + monitorLogFile + " && (wc -l " + monitorLogFile + "; tail -3 " + monitorLogFile + ") || echo 'No CSV file'");
                    System.out.println(ColorPrinter.yellow("  CSV file status:\n" + csvCheckStream.toString()));
                }
                
                // Clean up failed monitoring attempt
                System.out.println(ColorPrinter.cyan("\nCleaning up failed monitoring attempt..."));
                try {
                    ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
                    String cleanupCmd = String.format(
                        "if [ -f %s ]; then " +
                        "  DEAD_PID=$(cat %s); " +
                        "  kill $DEAD_PID 2>/dev/null; " +
                        "  rm -f %s; " +
                        "fi; " +
                        "rm -f /tmp/service_monitor_*.sh /tmp/monitor_debug_%s.log 2>/dev/null; " +
                        "echo 'Cleanup done'",
                        monitorPidFile, monitorPidFile, monitorPidFile, sanitizedName
                    );
                    runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
                    System.out.println(ColorPrinter.green("  " + cleanupStream.toString().trim()));
                } catch (Exception cleanupEx) {
                    System.out.println(ColorPrinter.yellow("  Warning: Cleanup failed: " + cleanupEx.getMessage()));
                }
                
                monitoringService = null;
                monitorLogFile = null;
                monitorPidFile = null;
            }
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error starting monitor: " + e.getMessage()));
            e.printStackTrace();
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
        }
    }

    @ShellMethod(key = "ro.monitor.stop", value = "eg: ro.monitor.stop")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_stop() {
        if (monitoringService == null) {
            System.out.println(ColorPrinter.yellow("No active monitoring session"));
            return;
        }
        
        try {
            System.out.println(ColorPrinter.cyan("Stopping monitoring for: " + monitoringService));
            
            // Stop the monitoring process
            String stopCmd = String.format(
                "test -f %s && kill $(cat %s) 2>/dev/null; rm -f %s",
                monitorPidFile, monitorPidFile, monitorPidFile
            );
            
            ByteArrayOutputStream stopStream = new ByteArrayOutputStream();
            runCommandShort(stopStream, Util.fullMachineName(machine), userName, passwd, stopCmd);
            
            // Wait a moment for the process to stop
            Thread.sleep(500);
            
            // Download and display the log file
            ByteArrayOutputStream logStream = new ByteArrayOutputStream();
            runCommandShort(logStream, Util.fullMachineName(machine), userName, passwd,
                "cat " + monitorLogFile);
            
            String logContent = logStream.toString();
            
            if (logContent.isEmpty()) {
                System.out.println(ColorPrinter.red("No monitoring data collected"));
            } else {
                // Parse and display the results
                displayMonitoringResults(logContent, monitoringService);
                
                // Save locally
                String localLogFile = "monitor_" + monitoringService.replace(".service", "") + "_" +
                                     System.currentTimeMillis() + ".csv";
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get(localLogFile),
                    logContent
                );
                System.out.println(ColorPrinter.green("\n✓ Monitoring data saved to: " + localLogFile));
            }
            
            // Cleanup remote files
            String cleanupCmd = String.format("rm -f %s /tmp/service_monitor_*.sh", monitorLogFile);
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
            monitor_cleanup();
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error stopping monitor: " + e.getMessage()));
            e.printStackTrace();
        } finally {
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
        }
    }

    @ShellMethod(key = "ro.monitor.cleanup", value = "eg: ro.monitor.cleanup - Stop all monitoring processes")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_cleanup() {
        try {
            System.out.println(ColorPrinter.cyan("Cleaning up all monitoring processes..."));
            
            // Find and kill all service_monitor processes
            ByteArrayOutputStream listStream = new ByteArrayOutputStream();
            runCommandShort(listStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep");
            
            String processList = listStream.toString();
            if (processList.trim().isEmpty()) {
                System.out.println(ColorPrinter.green("✓ No monitoring processes found"));
                return;
            }
            
            // Show what will be killed
            System.out.println(ColorPrinter.yellow("Found monitoring processes:"));
            System.out.println(processList);
            
            // Kill all service_monitor processes
            ByteArrayOutputStream killStream = new ByteArrayOutputStream();
            runCommandShort(killStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep | awk '{print $1}' | xargs kill 2>/dev/null; " +
                "sleep 1; " +
                "ps | grep 'service_monitor' | grep -v grep | awk '{print $1}' | xargs kill -9 2>/dev/null");
            
            // Clean up all related files
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd,
                "rm -f /tmp/monitor_*.pid /tmp/service_monitor_*.sh /tmp/monitor_debug_*.log /tmp/monitor_*.csv 2>/dev/null; " +
                "echo 'Removed PID files, scripts, debug logs, and CSV files'");
            
            System.out.println(ColorPrinter.green("\n✓ Cleanup complete"));
            System.out.println(ColorPrinter.cyan(cleanupStream.toString()));
            
            // Verify no processes remain
            ByteArrayOutputStream verifyStream = new ByteArrayOutputStream();
            runCommandShort(verifyStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep | wc -l");
            
            int remaining = 0;
            try {
                remaining = Integer.parseInt(verifyStream.toString().trim());
            } catch (NumberFormatException e) {
                // ignore
            }
            
            if (remaining > 0) {
                System.out.println(ColorPrinter.yellow("⚠ Warning: " + remaining + " process(es) still running (may require manual cleanup)"));
            } else {
                System.out.println(ColorPrinter.green("✓ All monitoring processes stopped"));
            }
            
            // Reset local state
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error during cleanup: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @ShellMethod(key = "ro.monitor.status", value = "eg: ro.monitor.status")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_status() {
        if (monitoringService == null) {
            System.out.println(ColorPrinter.yellow("No active monitoring session"));
            return;
        }
        
        try {
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Active Monitoring Session"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.yellow("Service: ") + monitoringService);
            System.out.println(ColorPrinter.yellow("Log file: ") + monitorLogFile);
            System.out.println(ColorPrinter.yellow("PID file: ") + monitorPidFile);
            
            // Check if still running
            ByteArrayOutputStream checkStream = new ByteArrayOutputStream();
            runCommandShort(checkStream, Util.fullMachineName(machine), userName, passwd,
                "test -f " + monitorPidFile + " && ps -p $(cat " + monitorPidFile + ") > /dev/null && echo 'RUNNING' || echo 'STOPPED'");
            
            String status = checkStream.toString().trim();
            if (status.contains("RUNNING")) {
                System.out.println(ColorPrinter.green("Status: ") + ColorPrinter.green("● Running"));
                
                // Show sample of recent data
                ByteArrayOutputStream tailStream = new ByteArrayOutputStream();
                runCommandShort(tailStream, Util.fullMachineName(machine), userName, passwd,
                    "tail -n 5 " + monitorLogFile);
                
                System.out.println(ColorPrinter.cyan("\nRecent data (last 5 entries):"));
                System.out.println(tailStream.toString());
            } else {
                System.out.println(ColorPrinter.red("Status: ") + ColorPrinter.red("● Stopped"));
            }
            
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error checking monitor status: " + e.getMessage()));
        }
    }

    private void displayMonitoringResults(String csvContent, String serviceName) {
        try {
            String[] lines = csvContent.split("\\R");
            if (lines.length < 2) {
                System.out.println(ColorPrinter.red("Insufficient data collected"));
                return;
            }
            
            // Parse CSV data
            List<MonitorData> dataPoints = new ArrayList<>();
            String processName = "unknown";
            
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(",");
                if (parts.length >= 9) {
                    try {
                        // New format: timestamp,process_name,pid,full_command_path,threads,mem_percent,mem_vsz_kb,unknown_field,cpu_percent
                        if (i == 1) {
                            processName = parts[1]; // Capture process name from first data line
                        }
                        
                        // Parse percentages (remove % suffix if present)
                        String memStr = parts[5].replace("%", "");
                        String cpuStr = parts[8].replace("%", "");
                        
                        MonitorData data = new MonitorData(
                            parts[0], // timestamp
                            parts[1], // process_name
                            parts[2], // pid
                            Double.parseDouble(cpuStr), // cpu (field 8)
                            Double.parseDouble(memStr), // mem (field 5)
                            0, // rss (not available in new format)
                            Long.parseLong(parts[6]), // vsz (field 6)
                            Integer.parseInt(parts[4]), // threads (field 4)
                            "R"  // status (not available, default to Running)
                        );
                        dataPoints.add(data);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines (like N/A entries)
                    }
                } else if (parts.length >= 8) {
                    // Old format fallback: timestamp,pid,cpu,mem,rss,vsz,threads,status
                    try {
                        MonitorData data = new MonitorData(
                            parts[0], // timestamp
                            "unknown", // process_name
                            parts[1], // pid
                            Double.parseDouble(parts[2]), // cpu
                            Double.parseDouble(parts[3]), // mem
                            Long.parseLong(parts[4]), // rss
                            Long.parseLong(parts[5]), // vsz
                            Integer.parseInt(parts[6]), // threads
                            parts[7]  // status
                        );
                        dataPoints.add(data);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                    }
                }
            }
            
            if (dataPoints.isEmpty()) {
                System.out.println(ColorPrinter.red("No valid data points collected"));
                return;
            }
            
            // Display summary statistics
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Monitoring Results for: " + serviceName));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
            double avgCpu = dataPoints.stream().mapToDouble(d -> d.cpu).average().orElse(0);
            double maxCpu = dataPoints.stream().mapToDouble(d -> d.cpu).max().orElse(0);
            double avgMem = dataPoints.stream().mapToDouble(d -> d.mem).average().orElse(0);
            double maxMem = dataPoints.stream().mapToDouble(d -> d.mem).max().orElse(0);
            long avgRss = (long) dataPoints.stream().mapToLong(d -> d.rss).average().orElse(0);
            long maxRss = dataPoints.stream().mapToLong(d -> d.rss).max().orElse(0);
            
            System.out.println(ColorPrinter.yellow("Process Name: ") + processName);
            System.out.println(ColorPrinter.yellow("Data Points: ") + dataPoints.size());
            System.out.println(ColorPrinter.yellow("Duration: ") + dataPoints.get(0).timestamp +
                             " to " + dataPoints.get(dataPoints.size() - 1).timestamp);
            System.out.println();
            System.out.println(ColorPrinter.green("CPU Usage:"));
            System.out.println("  Average: " + String.format("%.2f%%", avgCpu));
            System.out.println("  Maximum: " + String.format("%.2f%%", maxCpu));
            System.out.println();
            System.out.println(ColorPrinter.green("Memory Usage:"));
            System.out.println("  Average: " + String.format("%.2f%%", avgMem) +
                             " (" + (avgRss / 1024) + " MB)");
            System.out.println("  Maximum: " + String.format("%.2f%%", maxMem) +
                             " (" + (maxRss / 1024) + " MB)");
            
            // Display side-by-side graphs for CPU and Memory
            System.out.println("\n" + ColorPrinter.cyan("Resource Usage Over Time:"));
            displaySideBySideGraphs(
                dataPoints.stream().mapToDouble(d -> d.cpu).toArray(),
                dataPoints.stream().mapToDouble(d -> d.vsz / 1024.0).toArray(),
                "CPU %", "MEM MB", 20, 30
            );
            
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error displaying results: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    @ShellMethod(key = "ro.statistics", value = "Display system statistics graphs. Usage: ro.statistics <max_items>")
    @ShellMethodAvailability("availabilityCheck")
    void statistics(@ShellOption(defaultValue = "10") int maxItems) {
        try {
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("                    SYSTEM STATISTICS"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════════════"));
            
            // 1. Memory Usage Graph
            displayMemoryUsageGraph(maxItems);
            
            // 2. CPU Usage Graph
            displayCpuUsageGraph(maxItems);
            
            // 3. Disk Usage Graph
            displayDiskUsageGraph(maxItems);
            
        } catch (Exception e) {
            System.out.println(ColorPrinter.red("Error generating statistics: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private void displayMemoryUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("1. Memory Usage by Process (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use top command to get memory usage - BusyBox top shows VSZ
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "top -bn1 | grep -v 'top -' | grep -v 'Mem:' | grep -v 'CPU:' | grep -v 'Load' | grep -E '^\\s*[0-9]+' | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No process data available"));
            return;
        }
        
        List<ProcessMemInfo> processes = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // BusyBox top format: PID PPID USER STAT VSZ %VSZ %CPU COMMAND
            String[] parts = line.split("\\s+");
            if (parts.length >= 5) {
                try {
                    String processName = parts[parts.length - 1];
                    // VSZ is typically in column 4 (0-indexed)
                    long vszKB = Long.parseLong(parts[4]);
                    // Convert KB to MB for display
                    double memMB = vszKB / 1024.0;
                    processes.add(new ProcessMemInfo(parts[0],processName, memMB));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Sort by memory usage
        processes.sort((a, b) -> Double.compare(b.memPercent, a.memPercent));
        if (processes.size() > maxItems) {
            processes = processes.subList(0, maxItems);
        }
        
        // Display horizontal bar chart
        if (!processes.isEmpty()) {
            double maxMem = processes.stream().mapToDouble(p -> p.memPercent).max().orElse(1.0);
            int maxNameLength = processes.stream().mapToInt(p -> p.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (ProcessMemInfo proc : processes) {
                String name = String.format("%s %s", proc.id,proc.name);
                name = name.length() > maxNameLength ? name.substring(0, maxNameLength-3) + "..." : name;
                int barLength = (int) ((proc.memPercent / maxMem) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                
                System.out.printf("%-50s │ %s %.2f MB\n", name, bar, proc.memPercent);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                Memory Usage (MB)");
        }
    }
    
    private void displayCpuUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("2. CPU Usage by Process (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // BusyBox top command to get CPU usage - run once and parse output
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "top -bn1 | grep -v 'top -' | grep -v 'Mem:' | grep -v 'CPU:' | grep -v 'Load' | grep -E '^\\s*[0-9]+' | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No process data available"));
            return;
        }
        
        List<ProcessCpuInfo> processes = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // BusyBox top format: PID PPID USER STAT VSZ %VSZ %CPU COMMAND
            String[] parts = line.split("\\s+");
            if (parts.length >= 7) {
                try {
                    String processName = parts[parts.length - 1];
                    // %CPU is typically in column 6 (0-indexed)
                    String cpuStr = parts[6].replace("%", "");
                    double cpuPercent = Double.parseDouble(cpuStr);
                    processes.add(new ProcessCpuInfo(processName, cpuPercent));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Sort by CPU usage
        processes.sort((a, b) -> Double.compare(b.cpuPercent, a.cpuPercent));
        if (processes.size() > maxItems) {
            processes = processes.subList(0, maxItems);
        }
        
        // Display horizontal bar chart
        if (!processes.isEmpty()) {
            double maxCpu = processes.stream().mapToDouble(p -> p.cpuPercent).max().orElse(1.0);
            int maxNameLength = processes.stream().mapToInt(p -> p.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (ProcessCpuInfo proc : processes) {

                String name = proc.name.length() > maxNameLength ? proc.name.substring(0, maxNameLength-3) + "..." : proc.name;
                int barLength = (int) ((proc.cpuPercent / maxCpu) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                
                System.out.printf("%-50s │ %s %.1f%%\n", name, bar, proc.cpuPercent);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                CPU Usage (%)");
        }
    }
    
    private void displayDiskUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("3. Disk Usage by Binary in /usr/bin (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Get disk usage of binaries in /usr/bin sorted by size
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "du -b /usr/bin/* 2>/dev/null | sort -rn | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No binary data available"));
            return;
        }
        
        List<BinaryDiskInfo> binaries = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\s+", 2);
            if (parts.length >= 2) {
                try {
                    long sizeBytes = Long.parseLong(parts[0]);
                    String fullPath = parts[1];
                    String binaryName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                    binaries.add(new BinaryDiskInfo(binaryName, sizeBytes));
                } catch (NumberFormatException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Display horizontal bar chart
        if (!binaries.isEmpty()) {
            long maxSize = binaries.stream().mapToLong(b -> b.sizeBytes).max().orElse(1L);
            int maxNameLength = binaries.stream().mapToInt(b -> b.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (BinaryDiskInfo binary : binaries) {
                String name = binary.name.length() > maxNameLength ? binary.name.substring(0, maxNameLength-3) + "..." : binary.name;
                int barLength = (int) ((binary.sizeBytes / (double) maxSize) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                String sizeStr = formatBytes(binary.sizeBytes);
                
                System.out.printf("%-50s │ %s %s\n", name, bar, sizeStr);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                Disk Usage");
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // Helper classes for statistics
    private static class ProcessMemInfo {
        String id;
        String name;
        double memPercent;
        
        ProcessMemInfo(String id,String name, double memPercent) {
            this.id = id;
            this.name = name;
            this.memPercent = memPercent;
        }
    }
    
    private static class ProcessCpuInfo {
        String name;
        double cpuPercent;
        
        ProcessCpuInfo(String name, double cpuPercent) {
            this.name = name;
            this.cpuPercent = cpuPercent;
        }
    }
    
    private static class BinaryDiskInfo {
        String name;
        long sizeBytes;
        
        BinaryDiskInfo(String name, long sizeBytes) {
            this.name = name;
            this.sizeBytes = sizeBytes;
        }
    }


    private void displayAsciiGraph(double[] values, String label, int height, int width) {
        if (values.length == 0) return;
        
        double max = java.util.Arrays.stream(values).max().orElse(1);
        double min = java.util.Arrays.stream(values).min().orElse(0);
        double range = max - min;
        if (range == 0) range = 1;
        
        // Normalize values to graph height
        int[] normalized = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (int) ((values[i] - min) / range * (height - 1));
        }
        
        // Sample data points if too many
        int step = Math.max(1, values.length / width);
        
        // Draw graph from top to bottom
        for (int row = height - 1; row >= 0; row--) {
            double valueAtRow = min + (range * row / (height - 1));
            System.out.printf("%6.1f │", valueAtRow);
            
            for (int col = 0; col < Math.min(values.length, width); col += step) {
                int idx = col;
                if (normalized[idx] == row) {
                    System.out.print("●");
                } else if (normalized[idx] > row) {
                    System.out.print("│");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Draw x-axis
        System.out.print("       └");
        for (int i = 0; i < Math.min(width, values.length / step); i++) {
            System.out.print("─");
        }
        System.out.println(">");
        System.out.println("        " + label + " (Time →)");
    }

    private void displaySideBySideGraphs(double[] values1, double[] values2,
                                         String label1, String label2, int height, int width) {
        if (values1.length == 0 || values2.length == 0) return;
        
        // Normalize first dataset
        double max1 = java.util.Arrays.stream(values1).max().orElse(1);
        double min1 = java.util.Arrays.stream(values1).min().orElse(0);
        double range1 = max1 - min1;
        if (range1 == 0) range1 = 1;
        
        int[] normalized1 = new int[values1.length];
        for (int i = 0; i < values1.length; i++) {
            normalized1[i] = (int) ((values1[i] - min1) / range1 * (height - 1));
        }
        
        // Normalize second dataset
        double max2 = java.util.Arrays.stream(values2).max().orElse(1);
        double min2 = java.util.Arrays.stream(values2).min().orElse(0);
        double range2 = max2 - min2;
        if (range2 == 0) range2 = 1;
        
        int[] normalized2 = new int[values2.length];
        for (int i = 0; i < values2.length; i++) {
            normalized2[i] = (int) ((values2[i] - min2) / range2 * (height - 1));
        }
        
        int step = Math.max(1, Math.max(values1.length, values2.length) / width);
        
        // Display first graph (CPU Usage)
        System.out.println();
        System.out.printf("%-8s - CPU Usage%n", label1);
        System.out.println(ColorPrinter.cyan("─".repeat(width + 20)));
        
        for (int row = height - 1; row >= 0; row--) {
            double value1AtRow = min1 + (range1 * row / (height - 1));
            
            System.out.printf("%6.1f │", value1AtRow);
            for (int col = 0; col < Math.min(values1.length, width); col += step) {
                int idx = col;
                if (normalized1[idx] == row) {
                    System.out.print(ColorPrinter.green("●"));
                } else if (normalized1[idx] > row) {
                    System.out.print(ColorPrinter.green("│"));
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Draw x-axis for first graph
        System.out.print("       └");
        for (int i = 0; i < Math.min(width, values1.length / step); i++) {
            System.out.print("─");
        }
        System.out.println(">");
        System.out.printf("        %-" + width + "s%n", "Time →");
        
        // Display second graph (Memory Usage)
        System.out.println();
        System.out.printf("%-8s - Memory Usage%n", label2);
        System.out.println(ColorPrinter.cyan("─".repeat(width + 20)));
        
        for (int row = height - 1; row >= 0; row--) {
            double value2AtRow = min2 + (range2 * row / (height - 1));
            
            System.out.printf("%6.1f │", value2AtRow);
            for (int col = 0; col < Math.min(values2.length, width); col += step) {
                int idx = col;
                if (normalized2[idx] == row) {
                    System.out.print(ColorPrinter.yellow("●"));
                } else if (normalized2[idx] > row) {
                    System.out.print(ColorPrinter.yellow("│"));
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Draw x-axis for second graph
        System.out.print("       └");
        for (int i = 0; i < Math.min(width, values2.length / step); i++) {
            System.out.print("─");
        }
        System.out.println(">");
        System.out.printf("        %-" + width + "s%n", "Time →");
        System.out.println();
    }

    // Helper class to store monitoring data
    private static class MonitorData {
        String timestamp;
        String processName;
        String pid;
        double cpu;
        double mem;
        long rss;
        long vsz;
        int threads;
        String status;
        
        MonitorData(String timestamp, String processName, String pid, double cpu, double mem,
                   long rss, long vsz, int threads, String status) {
            this.timestamp = timestamp;
            this.processName = processName;
            this.pid = pid;
            this.cpu = cpu;
            this.mem = mem;
            this.rss = rss;
            this.vsz = vsz;
            this.threads = threads;
            this.status = status;
        }
    }
}
