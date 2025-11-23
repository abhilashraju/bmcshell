package com.ibm.bmcshell;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.bmcshell.Introspectables.Interface;
import com.ibm.bmcshell.Introspectables.ServiceDescription;
import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class DbusCommnads extends CommonCommands {

    Thread busMonitorThread = null;
    
    // Data structures for capturing bus communications
    private static class BusMessage {
        String timestamp;
        String type;
        String sender;
        String destination;
        String path;
        String iface;
        String member;
        String signature;
        String body;
        
        public BusMessage(String timestamp, String type, String sender, String destination,
                         String path, String iface, String member, String signature, String body) {
            this.timestamp = timestamp;
            this.type = type;
            this.sender = sender;
            this.destination = destination;
            this.path = path;
            this.iface = iface;
            this.member = member;
            this.signature = signature;
            this.body = body;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s -> %s | %s.%s(%s)",
                timestamp, type, sender, destination, iface, member, signature);
        }
    }
    
    private Queue<BusMessage> capturedMessages = new LinkedList<>();
    private volatile boolean isCapturing = false;
    private int maxCapturedMessages = 1000; // Limit to prevent memory issues

    @Component
    public static class BusNameProvider implements ValueProvider {
        public static List<String> busnames;
        public static String currentService;

        static String serViceName() {
            return currentService;
        }

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (busnames != null) {
                String userInput = context.currentWordUpToCursor();
                return busnames.stream()
                        .filter(name -> name.startsWith(userInput)) // Filter based on user
                                                                    // input
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    static String getArg(List<String> args, String key) {
        int index = args.indexOf(key);
        if (index != -1 && index + 1 < args.size()) {
            return args.get(index + 1);
        }
        return null;
    }

    @Component
    public static class PathNameProvider implements ValueProvider {
        public static HashMap<String, List<String>> pathnames = new HashMap<>();
        public static String currentPath;

        public static String currentPathName() {
            return currentPath;
        }

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (pathnames != null) {
                String sername = getArg(context.getWords(), "--ser");
                if (sername != null) {
                    BusNameProvider.currentService = sername;
                }
                var stream = pathnames.get(BusNameProvider.currentService);
                String userInput = context.currentWordUpToCursor();
                if (stream == null) {
                    return List.of(new CompletionProposal(userInput));
                }

                return stream
                        .stream()
                        .filter(name -> name.startsWith(userInput))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    @Component
    public static class InterfaceProvider implements ValueProvider {
        public static HashMap<String, ServiceDescription> introspectables = new HashMap<>();

        public static List<Interface> getCurrentInterfaces() {
            if (!introspectables.containsKey(getCurrentKey())) {
                return List.of();
            }
            return introspectables.get(getCurrentKey()).getInterfaces();
        }

        static String currentInterface;

        static String getCurrentInterface() {
            return currentInterface;
        }

        static String getCurrentKey() {
            return BusNameProvider.currentService + PathNameProvider.currentPath;
        }

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (introspectables != null) {
                String serName = getArg(context.getWords(), "--ser");
                String pathName = getArg(context.getWords(), "--path");
                if (serName != null) {
                    BusNameProvider.currentService = serName;
                }
                if (pathName != null) {
                    PathNameProvider.currentPath = pathName;
                }

                String userInput = context.currentWordUpToCursor();
                return getCurrentInterfaces().stream()
                        .filter(nm -> nm.getName().startsWith(userInput))
                        .map(iname -> iname.getName())
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }

        public static String makeKey(String service, String path) {
            return service + path;
        }
    }

    @Component
    public static class MethodProvider implements ValueProvider {
        static String currentMethod;

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (InterfaceProvider.introspectables != null) {
                var interfaceName = getArg(context.getWords(), "--iface");
                if (interfaceName != null) {
                    InterfaceProvider.currentInterface = interfaceName;
                }
                String userInput = context.currentWordUpToCursor();
                return InterfaceProvider.getCurrentInterfaces()
                        .stream()
                        .filter(iface -> iface.getName().equals(InterfaceProvider.currentInterface))
                        .flatMap(iface -> iface.getMembers().stream())
                        .filter(m -> m.getType().equals("method"))
                        .filter(nm -> nm.getName().startsWith(userInput))
                        .map(iname -> iname.getName())
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    @Component
    public static class SignatureProvider implements ValueProvider {

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (InterfaceProvider.introspectables != null) {
                var interfaceName = getArg(context.getWords(), "--iface");
                if (interfaceName != null) {
                    InterfaceProvider.currentInterface = interfaceName;
                }
                var methodName = getArg(context.getWords(), "--method");
                if (methodName != null) {
                    MethodProvider.currentMethod = methodName;
                }
                String userInput = context.currentWordUpToCursor();
                return InterfaceProvider.getCurrentInterfaces()
                        .stream()
                        .filter(iface -> iface.getName().equals(InterfaceProvider.currentInterface))
                        .flatMap(iface -> iface.getMembers().stream())
                        .filter(nm -> nm.getName().equals(MethodProvider.currentMethod))
                        .filter(nm -> nm.getSignature() != null)
                        .filter(nm -> nm.getSignature().startsWith(userInput))
                        .map(iname -> iname.getSignature())
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    @Component
    public static class PropertyProvider implements ValueProvider {

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (InterfaceProvider.introspectables != null) {
                var interfaceName = getArg(context.getWords(), "--iface");
                if (interfaceName != null) {
                    InterfaceProvider.currentInterface = interfaceName;
                }
                String userInput = context.currentWordUpToCursor();
                return InterfaceProvider.getCurrentInterfaces()
                        .stream()
                        .filter(iface -> iface.getName().equals(InterfaceProvider.currentInterface))
                        .flatMap(iface -> iface.getMembers().stream())
                        .filter(m -> m.getType().equals("property"))
                        .filter(nm -> nm.getName().startsWith(userInput))
                        .map(iname -> iname.getName())
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }

    }

    @Component
    public static class SignalValueProvider implements ValueProvider {
        static String currentSignal;

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (InterfaceProvider.introspectables != null) {
                var interfaceName = getArg(context.getWords(), "--iface");
                if (interfaceName != null) {
                    InterfaceProvider.currentInterface = interfaceName;
                }
                String userInput = context.currentWordUpToCursor();
                return InterfaceProvider.getCurrentInterfaces()
                        .stream()
                        .filter(iface -> iface.getName().equals(InterfaceProvider.currentInterface))
                        .flatMap(iface -> iface.getMembers().stream())
                        .filter(m -> m.getType().equals("signal"))
                        .filter(nm -> nm.getName().startsWith(userInput))
                        .map(iname -> iname.getName())
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }

    protected DbusCommnads() throws IOException {
    }

    @ShellMethod(key = "bs.introspect", value = "eg: bs.introspect xyz.openbmc_project.Network.Hypervisor /xyz/openbmc_project/network/hypervisor/eth0/ipv4")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(
            @ShellOption(value = { "--ser" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path)
            throws Exception {
        if (service.equals("")) {
            service = BusNameProvider.currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = PathNameProvider.currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        BusNameProvider.currentService = service;
        PathNameProvider.currentPath = path;
        System.out.println(populateIntrospectables(service, path));

    }

    public String populateIntrospectables(String service, String path) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("busctl introspect --json=pretty %s %s", service, path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String ret = outputStream.toString();

        ObjectMapper mapper = new ObjectMapper();

        ServiceDescription serviceInterfaces = mapper.readValue(convertToIntrospectJson(ret),
                ServiceDescription.class);
        InterfaceProvider.introspectables.put(InterfaceProvider.makeKey(service, path), serviceInterfaces);
        return ret;

    }

    public static String convertToIntrospectJson(String input) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode interfacesArray = root.putArray("interfaces");

        ObjectNode currentInterface = null;

        // Regex to match an interface line
        Pattern interfacePattern = Pattern.compile("^(\\S+)\\s+interface.*");

        // Regex to match a member (method, signal, property) line
        Pattern memberPattern = Pattern
                .compile("^\\.(\\S+)\\s+(method|signal|property)\\s+(\\S+)?\\s+(\\S+)?\\s+(.*)?");

        for (String line : input.split("\\R")) {
            if (line.isBlank() || line.startsWith("NAME")) {
                continue;
            }

            Matcher interfaceMatcher = interfacePattern.matcher(line);
            Matcher memberMatcher = memberPattern.matcher(line);

            if (interfaceMatcher.find()) {
                currentInterface = interfacesArray.addObject();
                currentInterface.put("name", interfaceMatcher.group(1));
                currentInterface.putArray("members");
            } else if (memberMatcher.find() && currentInterface != null) {
                ArrayNode membersArray = (ArrayNode) currentInterface.get("members");
                ObjectNode member = membersArray.addObject();

                member.put("name", memberMatcher.group(1));
                member.put("type", memberMatcher.group(2));

                String signature = memberMatcher.group(3);
                if (signature != null && !signature.equals("-")) {
                    member.put("signature", signature);
                }

                String value = memberMatcher.group(4);
                if (value != null && !value.equals("-")) {
                    member.put("result_value", value);
                }

                String flags = memberMatcher.group(5);
                if (flags != null && !flags.isBlank() && !flags.equals("-")) {
                    ArrayNode flagsArray = member.putArray("flags");
                    for (String flag : flags.trim().split("\\s+")) {
                        flagsArray.add(flag);
                    }
                }
            }
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    @ShellMethod(key = "bs.search", value = "eg: bs.search network")
    @ShellMethodAvailability("availabilityCheck")
    public void search(@ShellOption(valueProvider = BusNameProvider.class, value = { "--ser", "-s" }) String service)
            throws JsonParseException, JsonProcessingException {
        if (BusNameProvider.busnames == null || BusNameProvider.busnames.size() == 0) {
            getBusNames(false);
        }
        BusNameProvider.busnames.stream().filter(a -> a.toLowerCase().contains(service.toLowerCase()))
                .forEach(n -> {
                    System.out.println(n);
                    tree(n);
                });
    }

    @ShellMethod(key = "bs.list", value = "eg: bs.list ")
    @ShellMethodAvailability("availabilityCheck")
    List<String> getBusNames(@ShellOption(defaultValue = "true") boolean displayMode) throws JsonParseException, JsonProcessingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("busctl --json=short list"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ObjectMapper mapper = new ObjectMapper();
        String out = new String(outputStream.toByteArray());
        JsonNode rootNode = mapper.readTree(out);
        
        // Parse all bus service details
        List<JsonNode> busServices = StreamSupport.stream(rootNode.spliterator(), false)
                .filter(JsonNode::isObject)
                .collect(Collectors.toList());
        
        // Extract names for the provider
        BusNameProvider.busnames = busServices.stream()
                .filter(busNode -> busNode.has("name"))
                .map(busNode -> busNode.get("name").asText())
                .collect(Collectors.toList());
        
        // Display details in tabular format if displayMode is true
        if (displayMode) {
            System.out.println("\n" + "=".repeat(120));
            System.out.printf("%-50s %-15s %-15s %-15s %-20s%n",
                "NAME", "PID", "PROCESS", "USER", "CONNECTION");
            System.out.println("=".repeat(120));
            
            for (JsonNode busNode : busServices) {
                String name = busNode.has("name") ? busNode.get("name").asText() : "";
                String pid = busNode.has("pid") ? busNode.get("pid").asText() : "-";
                String process = busNode.has("process") ? busNode.get("process").asText() : "-";
                String user = busNode.has("user") ? busNode.get("user").asText() : "-";
                String connection = busNode.has("connection") ? busNode.get("connection").asText() : "-";
                
                System.out.printf("%-50s %-15s %-15s %-15s %-20s%n",
                    name, pid, process, user, connection);
            }
            System.out.println("=".repeat(120));
            System.out.println("Total services: " + busServices.size() + "\n");
            return null;
        }
        
        return BusNameProvider.busnames;
    }

    @ShellMethod(key = "bs.tree", value = "eg: bs.tree xyz.openbmc_project.Network.Hypervisor")
    @ShellMethodAvailability("availabilityCheck")
    public void tree(@ShellOption(valueProvider = BusNameProvider.class, value = { "--ser", "-s" }) String service) {
        if (!service.equals("*")) {
            BusNameProvider.currentService = service;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("busctl tree %s", service.equals("*") ? "" : service));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(outputStream);
        var paths = convertTreeToPaths(outputStream.toString());
        PathNameProvider.pathnames.put(service, paths);
        populateInterfaceList(paths, service);

    }

    void populateInterfaceList(List<String> paths, String service) {
        Thread thread = new Thread(() -> {
            try {
                for (String path : paths) {
                    if (!InterfaceProvider.introspectables.containsKey(InterfaceProvider.makeKey(service, path))) {
                        populateIntrospectables(service, path);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start(); // Start the thread
    }

    public static List<String> convertTreeToPaths(String input) {
        List<String> result = new ArrayList<>();
        // Regex matches strings like -/path or `-/path or |-/path
        Pattern pattern = Pattern.compile("[`|\\- ]+(/[^\\s]+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    @ShellMethod(key = "bs.managed_objects", value = "eg: bs.managed_objects xyz.openbmc_project.Network /xyz/openbmc_project/network")
    @ShellMethodAvailability("availabilityCheck")
    public void managed_objects(
            @ShellOption(valueProvider = BusNameProvider.class, value = { "--ser", "-s" }) String service,
            String path) {
        runCommand(Util.fullMachineName(machine), userName, passwd, String.format(
                "busctl --json=pretty call %s %s org.freedesktop.DBus.ObjectManager GetManagedObjects --verbose", service, path));
    }

    @ShellMethod(key = "bs.subtree", value = "eg: bs.subtree /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void subtree(String obj, int depth, String iface) {
        execMapperCall(iface, "GetSubTree", obj, depth, "sias");
    }

    @ShellMethod(key = "bs.subtreepaths", value = "eg: bs.subtreepaths /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void GetSubTreePaths(String obj, int depth, String iface) {
        execMapperCall(iface, "GetSubTreePaths", obj, depth, "sias");
    }

    @ShellMethod(key = "bs.associatedsubtreepaths", value = "eg: bs.associatedsubtreepaths /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void GetAssociatedSubTreePaths(String obj, int depth, String iface) {
        execMapperCall(iface, "GetAssociatedSubTreePaths", obj, depth, "ooias");
          
    }

    @ShellMethod(key = "bs.object", value = "eg: bs.object /xyz/openbmc_project/sensors/power/total_power xyz.openbmc_project.Sensor.Value")
    @ShellMethodAvailability("availabilityCheck")
    public void getObject(String obj, String iface) {
        var format = "busctl --json=pretty call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas %s %d";
        var infaces = iface.split(",");
        var commd = String.format(format, obj, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd;
        scmd(commd);
    }

    @ShellMethod(key = "bs.call", value = "eg: bs.call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas \"3 1 2 3 test123 0\"")
    @ShellMethodAvailability("availabilityCheck")
    public void call(
            @ShellOption(value = { "--ser" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = {
                    "--iface" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = { "--method" }, valueProvider = MethodProvider.class) String method,
            @ShellOption(value = {
                    "--signature" }, defaultValue = "", valueProvider = SignatureProvider.class) String sig,
            @ShellOption(defaultValue = "") String args) {
        if (service.equals("")) {
            service = BusNameProvider.currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = PathNameProvider.currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iface.equals("")) {
            iface = InterfaceProvider.currentInterface;
            System.out.println("Interface is null, using default interface " + iface);
        }
        BusNameProvider.currentService = service;
        PathNameProvider.currentPath = path;
        InterfaceProvider.currentInterface = iface;
        
        var formatwitharg = "busctl call %s %s %s %s %s %s";
        var format = "busctl --json=pretty call %s %s %s %s";
        String commd;
        
        // Check if args were provided
        boolean hasArgs = args != null && !args.isEmpty();
        
        if (hasArgs) {
            // Split args by space, but preserve quoted strings
            List<String> argList = new ArrayList<>();
            Pattern pattern = Pattern.compile("\"([^\"]*)\"|\\S+");
            Matcher matcher = pattern.matcher(args);
            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    // Quoted string - keep the quotes
                    argList.add("\"" + matcher.group(1) + "\"");
                } else {
                    // Unquoted token
                    String token = matcher.group();
                    // If it's a number, keep as-is; otherwise quote it
                    if (token.matches("-?\\d+(\\.\\d+)?")) {
                        argList.add(token);
                    } else {
                        argList.add("\"" + token + "\"");
                    }
                }
            }
            
            String processedArgs = String.join(" ", argList);
            commd = String.format(formatwitharg, service, path, iface, method, sig, processedArgs);
        }
        else
            commd = String.format(format, service, path, iface, method);

        commd = commd + " --verbose";
        System.err.println(commd);
        scmd(commd);
    }

    @ShellMethod(key = "bs.setproperty", value = "eg: bs.setproperty xyz.openbmc_project.EntityManager /xyz/openbmc_project/inventory/system/chassis/Tacoma_Rack_Controller/aggregated0 xyz.openbmc_project.Configuration.SatelliteController Hostname s Tacoma_Rack_Controller")
    @ShellMethodAvailability("availabilityCheck")
    public void setProperty(
            @ShellOption(value = { "--ser" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = {
                    "--iface" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iFace,
            @ShellOption(value = { "--prop" }, valueProvider = PropertyProvider.class) String property,
            @ShellOption(value = { "--signature" }, valueProvider = SignatureProvider.class) String sig,
            @ShellOption(value = { "--args" }) String args) {
        if (service.equals("")) {
            service = BusNameProvider.currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = PathNameProvider.currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iFace.equals("")) {
            iFace = InterfaceProvider.currentInterface;
            System.out.println("Interface is null, using default interface " + iFace);
        }
        BusNameProvider.currentService = service;
        PathNameProvider.currentPath = path;
        InterfaceProvider.currentInterface = iFace;
        String commd = String.format("busctl set-property %s %s %s %s %s %s", service, path, iFace, property, sig,
                args);
        commd = commd + " --verbose";
        System.out.println("Executing");
        System.out.println(commd);
        scmd(commd);

    }

    private String execMapperCall(String iface, String method, String obj, int depth, String argformat) {
        var format = "sudo -i busctl --json=pretty call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper %s %s %s %d %d";
        var infaces = iface.split(",");
        var commd = String.format(format, method, argformat, obj, depth, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd + " --verbose";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    commd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(outputStream);
        return outputStream.toString();
    }

    @ShellMethod(key = "bs.property", value = "eg: bs.property xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable true")
    @ShellMethodAvailability("availabilityCheck")
    public void property(
            @ShellOption(value = { "--ser",
                    "-s" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = {
                    "--iface" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = { "--prop" }, defaultValue = "", valueProvider = PropertyProvider.class) String prop) {
        if (service.equals("")) {
            service = BusNameProvider.currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = PathNameProvider.currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iface.equals("")) {
            iface = InterfaceProvider.currentInterface;
            System.out.println("Interface is null, using default interface " + iface);
        }
        runCommand(Util.fullMachineName(machine), userName, passwd,
                String.format("busctl get-property %s %s %s %s %s", service, path, iface, prop, "--verbos"));

    }

    @Component
    public static class TypeValueProvider implements ValueProvider {

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (InterfaceProvider.introspectables != null) {

                String userInput = context.currentWordUpToCursor();
                var list = List.of("method_call", "method_return", "error", "signal");
                return list.stream()
                        .filter(nm -> nm.startsWith(userInput))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }
    
    // Parse busctl monitor output line and extract message information
    private BusMessage parseBusMessage(String line) {
        try {
            // Example busctl monitor output format:
            // ‣ Type=signal  Endian=l  Flags=1  Version=1  Priority=0 Cookie=123
            //   Sender=:1.23  Destination=n/a  Path=/xyz/openbmc_project/sensors/temperature/CPU
            //   Interface=org.freedesktop.DBus.Properties  Member=PropertiesChanged
            //   UniqueName=:1.23
            
            if (line.trim().startsWith("‣") || line.trim().startsWith("Type=")) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                String type = extractValue(line, "Type=");
                String sender = "";
                String destination = "";
                String path = "";
                String iface = "";
                String member = "";
                String signature = "";
                String body = "";
                
                return new BusMessage(timestamp, type, sender, destination, path, iface, member, signature, body);
            } else if (line.trim().startsWith("Sender=")) {
                // This is a continuation line with more details
                return null; // Will be handled by accumulating lines
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return null;
    }
    
    private String extractValue(String line, String key) {
        int start = line.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = line.indexOf(" ", start);
        if (end == -1) end = line.length();
        return line.substring(start, end).trim();
    }
    
    // Parse complete busctl monitor message from multiple lines
    private BusMessage parseCompleteBusMessage(List<String> lines) {
        if (lines.isEmpty()) return null;
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String type = "";
        String sender = "";
        String destination = "";
        String path = "";
        String iface = "";
        String member = "";
        String signature = "";
        String body = "";
        
        for (String line : lines) {
            if (line.contains("Type=")) type = extractValue(line, "Type=");
            if (line.contains("Sender=")) sender = extractValue(line, "Sender=");
            if (line.contains("Destination=")) destination = extractValue(line, "Destination=");
            if (line.contains("Path=")) path = extractValue(line, "Path=");
            if (line.contains("Interface=")) iface = extractValue(line, "Interface=");
            if (line.contains("Member=")) member = extractValue(line, "Member=");
            if (line.contains("Signature=")) signature = extractValue(line, "Signature=");
        }
        
        return new BusMessage(timestamp, type, sender, destination, path, iface, member, signature, body);
    }
    
    // Generate ASCII sequence diagram from captured messages
    private String generateSequenceDiagram() {
        if (capturedMessages.isEmpty()) {
            return "No messages captured yet.";
        }
        
        StringBuilder diagram = new StringBuilder();
        diagram.append("\n╔════════════════════════════════════════════════════════════════════════════════╗\n");
        diagram.append("║                    D-Bus Communication Sequence Diagram                        ║\n");
        diagram.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");
        
        // Collect unique services
        Map<String, Integer> servicePositions = new LinkedHashMap<>();
        int position = 0;
        for (BusMessage msg : capturedMessages) {
            if (!msg.sender.isEmpty() && !servicePositions.containsKey(msg.sender)) {
                servicePositions.put(msg.sender, position++);
            }
            if (!msg.destination.isEmpty() && !msg.destination.equals("n/a") &&
                !servicePositions.containsKey(msg.destination)) {
                servicePositions.put(msg.destination, position++);
            }
        }
        
        // Draw service headers
        diagram.append("Services:\n");
        for (Map.Entry<String, Integer> entry : servicePositions.entrySet()) {
            diagram.append(String.format("  [%d] %s\n", entry.getValue(),
                entry.getKey().length() > 40 ? entry.getKey().substring(0, 37) + "..." : entry.getKey()));
        }
        diagram.append("\n");
        
        // Draw messages
        diagram.append("Timeline:\n");
        diagram.append("─".repeat(80)).append("\n");
        
        int msgCount = 0;
        for (BusMessage msg : capturedMessages) {
            if (++msgCount > 50) { // Limit display
                diagram.append("... (").append(capturedMessages.size() - 50).append(" more messages)\n");
                break;
            }
            
            String senderLabel = servicePositions.containsKey(msg.sender) ?
                "[" + servicePositions.get(msg.sender) + "]" : msg.sender;
            String destLabel = servicePositions.containsKey(msg.destination) ?
                "[" + servicePositions.get(msg.destination) + "]" : msg.destination;
            
            String arrow = msg.type.equals("signal") ? "~~>" : "--->";
            String methodInfo = msg.member.isEmpty() ? "" : "." + msg.member + "()";
            
            diagram.append(String.format("[%s] %s %s %s : %s%s\n",
                msg.timestamp,
                senderLabel,
                arrow,
                destLabel,
                msg.iface.isEmpty() ? msg.type : msg.iface,
                methodInfo
            ));
        }
        
        diagram.append("─".repeat(80)).append("\n");
        diagram.append(String.format("\nTotal messages captured: %d\n", capturedMessages.size()));
        
        return diagram.toString();
    }
    
    // Generate statistics from captured messages
    private String generateStatistics() {
        if (capturedMessages.isEmpty()) {
            return "No messages captured yet.";
        }
        
        Map<String, Integer> typeCount = new HashMap<>();
        Map<String, Integer> interfaceCount = new HashMap<>();
        Map<String, Integer> senderCount = new HashMap<>();
        
        for (BusMessage msg : capturedMessages) {
            typeCount.put(msg.type, typeCount.getOrDefault(msg.type, 0) + 1);
            if (!msg.iface.isEmpty()) {
                interfaceCount.put(msg.iface, interfaceCount.getOrDefault(msg.iface, 0) + 1);
            }
            if (!msg.sender.isEmpty()) {
                senderCount.put(msg.sender, senderCount.getOrDefault(msg.sender, 0) + 1);
            }
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("\n╔════════════════════════════════════════════════════════════════════════════════╗\n");
        stats.append("║                         D-Bus Communication Statistics                         ║\n");
        stats.append("╚════════════════════════════════════════════════════════════════════════════════╝\n\n");
        
        stats.append("Message Types:\n");
        typeCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(e -> stats.append(String.format("  %-20s : %d\n", e.getKey(), e.getValue())));
        
        stats.append("\nTop Interfaces:\n");
        interfaceCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(e -> stats.append(String.format("  %-50s : %d\n",
                e.getKey().length() > 50 ? e.getKey().substring(0, 47) + "..." : e.getKey(),
                e.getValue())));
        
        stats.append("\nTop Senders:\n");
        senderCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(e -> stats.append(String.format("  %-50s : %d\n",
                e.getKey().length() > 50 ? e.getKey().substring(0, 47) + "..." : e.getKey(),
                e.getValue())));
        
        stats.append(String.format("\nTotal messages: %d\n", capturedMessages.size()));
        
        return stats.toString();
    }

    @ShellMethod(key = "bs.monitor.start", value = "eg: bs.monitor.start --type signal --capture true")
    @ShellMethodAvailability("availabilityCheck")
    public void monitor(
            @ShellOption(value = { "--type" }, defaultValue = "", valueProvider = TypeValueProvider.class) String type,
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = {
                    "--iface" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = {
                    "--sender" }, defaultValue = "", valueProvider = BusNameProvider.class) String sender,
            @ShellOption(value = {
                    "--member" }, defaultValue = "", valueProvider = SignalValueProvider.class) String member,
            @ShellOption(value = { "--capture" }, defaultValue = "false") boolean capture)
            throws IOException {
        monitorStop(); // Stop any existing monitor thread
        
        // Clear previous captures if starting new capture
        if (capture) {
            capturedMessages.clear();
            isCapturing = true;
            System.out.println("📊 Bus monitoring started in background with message capture enabled");
            System.out.println("💡 Capturing silently - no output will be displayed");
            System.out.println("💡 Use 'bs.monitor.diagram' to view sequence diagram");
            System.out.println("💡 Use 'bs.monitor.stats' to view statistics");
            System.out.println("💡 Use 'bs.monitor.stop' to stop monitoring");
        }
        
        StringBuffer command = new StringBuffer();
        command.append("busctl monitor");
        if (type != null && !type.isBlank()) {
            command.append(" ").append("--match=\"%s\"");
            StringBuffer rules = new StringBuffer();
            if (type != null && !type.isBlank()) {
                rules.append("type='").append(type).append("'");
            }
            if (path != null && !path.isBlank()) {
                rules.append(", path='").append(path).append("'");
            }
            if (iface != null && !iface.isBlank()) {
                rules.append(", interface='").append(iface).append("'");
            }

            if (member != null && !member.isBlank()) {
                rules.append(", member='").append(member).append("'");
            }
            if (sender != null && !sender.isBlank()) {
                rules.append(", sender='").append(sender).append("'");
            }
            String monitorCmd = String.format(command.toString(), rules.toString().trim());
            
            if (capture) {
                Thread busThread = new Thread(() -> runMonitorWithCapture(monitorCmd));
                busThread.setName("BusMonitorThread");
                busThread.start();
                this.busMonitorThread = busThread;
            } else {
                Thread busThread = new Thread(() -> scmd(monitorCmd));
                busThread.setName("BusMonitorThread");
                busThread.start();
                this.busMonitorThread = busThread;
            }
            return;
        }
        
        if (capture) {
            Thread busThread = new Thread(() -> runMonitorWithCapture(command.toString()));
            busThread.setName("BusMonitorThread");
            busThread.start();
            this.busMonitorThread = busThread;
        } else {
            Thread busThread = new Thread(() -> scmd(command.toString()));
            busThread.setName("BusMonitorThread");
            busThread.start();
            this.busMonitorThread = busThread;
        }
        return;
    }
    
    // Run busctl monitor and capture messages using SSH (silent mode - no console output)
    private void runMonitorWithCapture(String command) {
        try {
            String name = userName.equals("root") ? userName : "service";
            String fullCommand = name.equals("root") ? command : "sudo -i " + command;
            
            com.jcraft.jsch.Session session = com.ibm.bmcshell.ssh.SSHShellClient.jsch.getSession(
                name, Util.fullMachineName(machine), com.ibm.bmcshell.ssh.SSHShellClient.port);
            session.setPassword(passwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            
            com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
            channel.setCommand(fullCommand);
            
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            
            channel.connect();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(err));
            
            List<String> messageLines = new ArrayList<>();
            String line;
            
            // Read output in a loop (silently - no console output in capture mode)
            while (!Thread.currentThread().isInterrupted()) {
                // Check if data is available
                if (in.available() > 0) {
                    line = reader.readLine();
                    if (line == null) break;
                    
                    // DO NOT print to console in capture mode - silent background capture
                    
                    if (isCapturing) {
                        // Check if this is the start of a new message
                        if (line.trim().startsWith("‣") || line.trim().startsWith("Type=")) {
                            // Process previous message if exists
                            if (!messageLines.isEmpty()) {
                                BusMessage msg = parseCompleteBusMessage(messageLines);
                                if (msg != null) {
                                    capturedMessages.offer(msg);
                                    // Limit queue size
                                    while (capturedMessages.size() > maxCapturedMessages) {
                                        capturedMessages.poll();
                                    }
                                }
                                messageLines.clear();
                            }
                        }
                        messageLines.add(line);
                    }
                }
                
                // Check for errors (also silent in capture mode)
                if (err.available() > 0) {
                    line = errorReader.readLine();
                    // Errors are also not printed in capture mode
                }
                
                // Check if channel is closed
                if (channel.isClosed()) {
                    if (in.available() > 0) continue; // Still data to read
                    break;
                }
                
                Thread.sleep(100); // Small delay to avoid busy waiting
            }
            
            // Process last message
            if (!messageLines.isEmpty() && isCapturing) {
                BusMessage msg = parseCompleteBusMessage(messageLines);
                if (msg != null) {
                    capturedMessages.offer(msg);
                }
            }
            
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("⚠️  Error in bus monitor: " + e.getMessage());
            }
        }
    }

    @ShellMethod(key = "bs.monitor.stop", value = "eg: bs.monitor.stop")
    void monitorStop() {
        isCapturing = false;
        if (busMonitorThread != null && busMonitorThread.isAlive()) {
            busMonitorThread.interrupt();
            try {
                busMonitorThread.join(2000); // Wait up to 2 seconds for thread to stop
            } catch (InterruptedException e) {
                // Ignore
            }
            System.out.println("🛑 Bus Monitor stopped.");
            if (!capturedMessages.isEmpty()) {
                System.out.println(String.format("📊 Captured %d messages", capturedMessages.size()));
            }
        }
    }
    
    @ShellMethod(key = "bs.monitor.diagram", value = "Display sequence diagram of captured D-Bus messages")
    @ShellMethodAvailability("availabilityCheck")
    public void monitorDiagram() {
        // Auto-stop any running capture before displaying
        if (busMonitorThread != null && busMonitorThread.isAlive()) {
            System.out.println("⏸️  Stopping capture to display diagram...");
            monitorStop();
        }
        System.out.println(generateSequenceDiagram());
    }
    
    @ShellMethod(key = "bs.monitor.stats", value = "Display statistics of captured D-Bus messages")
    @ShellMethodAvailability("availabilityCheck")
    public void monitorStats() {
        // Auto-stop any running capture before displaying stats
        if (busMonitorThread != null && busMonitorThread.isAlive()) {
            System.out.println("⏸️  Stopping capture to display statistics...");
            monitorStop();
        }
        System.out.println(generateStatistics());
    }
    
    @ShellMethod(key = "bs.monitor.clear", value = "Clear captured D-Bus messages")
    @ShellMethodAvailability("availabilityCheck")
    public void monitorClear() {
        int count = capturedMessages.size();
        capturedMessages.clear();
        System.out.println(String.format("🗑️  Cleared %d captured messages", count));
    }
    
    @ShellMethod(key = "bs.monitor.export", value = "Export captured messages to file")
    @ShellMethodAvailability("availabilityCheck")
    public void monitorExport(
            @ShellOption(value = { "--file" }, defaultValue = "dbus_capture.txt") String filename) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("D-Bus Message Capture Export\n");
            content.append("Generated: ").append(LocalDateTime.now()).append("\n");
            content.append("Total Messages: ").append(capturedMessages.size()).append("\n\n");
            content.append(generateSequenceDiagram()).append("\n\n");
            content.append(generateStatistics()).append("\n\n");
            content.append("Detailed Messages:\n");
            content.append("=".repeat(80)).append("\n");
            
            for (BusMessage msg : capturedMessages) {
                content.append(msg.toString()).append("\n");
            }
            
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), content.toString().getBytes());
            System.out.println(String.format("✅ Exported %d messages to %s", capturedMessages.size(), filename));
        } catch (IOException e) {
            System.err.println("❌ Error exporting messages: " + e.getMessage());
        }
    }

    @ShellMethod(key = "bs.serial_number", value = "eg: bs.serial_number")
    @ShellMethodAvailability("availabilityCheck")
    public void serial_number() throws IOException {
        String commd = " busctl get-property xyz.openbmc_project.Inventory.Manager /xyz/openbmc_project/inventory/system xyz.openbmc_project.Inventory.Decorator.Asset SerialNumber";
        scmd(commd);
    }

    @ShellMethod(key = "bs.emit_signal", value = "eg: bs.emit_signal --ser xyz.openbmc_project.Service --path /xyz/openbmc_project/path --iface xyz.openbmc_project.Interface --signal MySignal --sig s --args value1|value2")
    @ShellMethodAvailability("availabilityCheck")
    public void emit_signal(
            @ShellOption(value = { "--path" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = {
                    "--iface" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = { "--signal" }, valueProvider = SignalValueProvider.class) String signal,
            @ShellOption(value = {
                    "--signature" }, defaultValue = "", valueProvider = SignatureProvider.class) String sig,
            @ShellOption(value = { "--args" }, defaultValue = "") String args) {

        if (path.equals("")) {
            path = PathNameProvider.currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iface.equals("")) {
            iface = InterfaceProvider.currentInterface;
            System.out.println("Interface is null, using default interface " + iface);
        }

        PathNameProvider.currentPath = path;
        InterfaceProvider.currentInterface = iface;

        String commd;
        String argsPart = "";
        if (args != null && !args.isBlank()) {
            argsPart = Arrays.stream(args.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    // .map(a -> "\"" + a.replace("\"", "\\\"") + "\"")
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
        }

        if (sig != null && !sig.isBlank()) {
            commd = String.format("busctl --system emit %s %s %s %s %s", path, iface, signal, sig,
                    argsPart);
        } else {
            // no signature provided
            commd = String.format("busctl --system emit %s %s %s", path, iface, signal);
            if (!argsPart.isBlank()) {
                commd = commd + " " + argsPart;
            }
        }

        commd = commd + " --verbose";
        System.err.println("Executing: " + commd);
        scmd(commd);
    }

}
