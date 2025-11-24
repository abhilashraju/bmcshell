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
                String userInput = context.currentWordUpToCursor();
                return serviceNames.stream()
                        .filter(nm -> nm.startsWith(userInput))
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
        scmd(String.format("systemctl show %s |grep %s", s, reg));

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
            System.out.println(prefix + ColorPrinter.blue(service));
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
}
