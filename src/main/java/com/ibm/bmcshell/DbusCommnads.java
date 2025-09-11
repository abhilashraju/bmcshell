package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import com.ibm.bmcshell.Introspectables.Member;
import com.ibm.bmcshell.Introspectables.ServiceDescription;
import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class DbusCommnads extends CommonCommands {
    

    @Component
    public static class BusNameProvider implements ValueProvider {
        public static List<String> busnames;
        public static String currentService;
        static String serViceName(){
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
    static String  getArg(List<String> args, String key) {
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
        public static String currentPathName(){
            return currentPath;
        }
        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (pathnames != null) {
                String sername = getArg(context.getWords(), "--ser");
                if(sername!=null){    
                    BusNameProvider.currentService = sername; 
                }     
                var stream =pathnames.get(BusNameProvider.currentService);
                String userInput = context.currentWordUpToCursor();
                if(stream==null){
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
        public static List<Interface> getCurrentInterfaces(){
            if(!introspectables.containsKey(getCurrentKey())){
                return List.of();
            }
            return introspectables.get(getCurrentKey()).getInterfaces();    
        }
        static String currentInterface;
        static String getCurrentInterface(){
            return currentInterface;
        }
        static String getCurrentKey(){
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

    protected DbusCommnads() throws IOException {
    }

    @ShellMethod(key = "bs.introspect", value = "eg: bs.introspect xyz.openbmc_project.Network.Hypervisor /xyz/openbmc_project/network/hypervisor/eth0/ipv4")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(
            @ShellOption(value = { "--ser",
                    "-s" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path",
                    "-p" }, defaultValue = "", valueProvider = PathNameProvider.class) String path)
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
        final String ser = service;
        final String pth = path;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        redirector(outputStream, () -> {
            try {
                runCommandShort(Util.fullMachineName(machine), userName, passwd,
                        String.format("busctl introspect --json=pretty %s %s", ser, pth));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(outputStream.toString());
        ObjectMapper mapper = new ObjectMapper();

        ServiceDescription serviceInterfaces = mapper.readValue(convertToIntrospectJson(outputStream.toString()),
                ServiceDescription.class);
        InterfaceProvider.introspectables.put(InterfaceProvider.getCurrentKey(), serviceInterfaces);

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
        if (BusNameProvider.busnames == null) {
            getBusNames();
        }
        BusNameProvider.busnames.stream().filter(a -> a.toLowerCase().contains(service.toLowerCase()))
                .forEach(n -> {System.out.println(n);tree(n);});
    }

    @ShellMethod(key = "bs.list", value = "eg: bs.list ")
    @ShellMethodAvailability("availabilityCheck")
    List<String> getBusNames() throws JsonParseException, JsonProcessingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        redirector(outputStream, () -> {
            try {
                runCommandShort(Util.fullMachineName(machine), userName, passwd,
                        String.format("busctl --json=short list"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ObjectMapper mapper = new ObjectMapper();
        String out = new String(outputStream.toByteArray());
        JsonNode rootNode = mapper.readTree(out);
        BusNameProvider.busnames = StreamSupport.stream(rootNode.spliterator(), false)
                .filter(JsonNode::isObject) // Filter out any non-object nodes
                .filter(busNode -> busNode.has("name")) // Ensure the "name" field exists
                .map(busNode -> busNode.get("name").asText()) // Extract the "name" as a String
                .collect(Collectors.toList()); // Collect the results into a List<String>
        return BusNameProvider.busnames;
    }

    @ShellMethod(key = "bs.tree", value = "eg: bs.tree xyz.openbmc_project.Network.Hypervisor")
    @ShellMethodAvailability("availabilityCheck")
    public void tree(@ShellOption(valueProvider = BusNameProvider.class, value = { "--ser", "-s" }) String service) {
        if (!service.equals("*")) {
            BusNameProvider.currentService = service;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        redirector(outputStream, () -> {
            try {
                runCommandShort(Util.fullMachineName(machine), userName, passwd,
                        String.format("busctl tree %s", service.equals("*") ? "" : service));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(outputStream);

        PathNameProvider.pathnames.put(service, convertTreeToPaths(outputStream.toString()));

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
                "busctl call %s %s org.freedesktop.DBus.ObjectManager GetManagedObjects --verbose", service, path));
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
        var format = "busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas %s %d";
        var infaces = iface.split(",");
        var commd = String.format(format, obj, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd + " --verbose";
        scmd(commd);
    }

    @ShellMethod(key = "bs.call", value = "eg: bs.call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas /xyz/openbmc_project/sensors/power/total_power 1 xyz.openbmc_project.Sensor.Value")
    @ShellMethodAvailability("availabilityCheck")
    public void call(@ShellOption(value = { "--ser"}, defaultValue = "",valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path"}, defaultValue = "",valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = { "--iface",
                    "-i" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = { "--method"}, valueProvider = MethodProvider.class) String method,
            @ShellOption(value = { "--sig"}, defaultValue = "", valueProvider = SignatureProvider.class) String sig,
            @ShellOption(value = { "--args"}, defaultValue = "") String args) {
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
        var format = "busctl call %s %s %s %s";
        String commd;
        if (args != null) {

            var str = Arrays.stream(args.split("\\|")).map(a -> "\"" + a + "\" ").reduce((a, b) -> a + b).orElse("");
            commd = String.format(formatwitharg, service, path, iface, method, sig, str);
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
            @ShellOption(value = { "--ser",
                    "-s" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path",
                    "-p" }, defaultValue = "", valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = { "--iface",
                    "-i" }, defaultValue = "", valueProvider = InterfaceProvider.class) String iFace,
            @ShellOption(value = { "--property", "-pn" }, valueProvider = MethodProvider.class) String property,
            @ShellOption(value = { "--sig", "-si" }, valueProvider = SignatureProvider.class) String sig,
            @ShellOption(value = { "--args", "-a" }) String args) {
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

    private void execMapperCall(String iface, String method, String obj, int depth, String argformat) {
        var format = "busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper %s %s %s %d %d";
        var infaces = iface.split(",");
        var commd = String.format(format, method, argformat, obj, depth, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd + " --verbose";
        scmd(commd);
    }

    @ShellMethod(key = "bs.property", value = "eg: bs.property xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable true")
    @ShellMethodAvailability("availabilityCheck")
    public void property(@ShellOption(value = { "--ser", "-s" }, defaultValue = "",valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "",valueProvider = PathNameProvider.class) String path,
            @ShellOption(value = { "--iface", "-i" }, defaultValue = "",valueProvider = InterfaceProvider.class) String iface,
            @ShellOption(value = { "--prop", "-pn" }, defaultValue = "",valueProvider = PropertyProvider.class) String prop) {
        runCommand(Util.fullMachineName(machine), userName, passwd,
                String.format("busctl get-property %s %s %s %s %s", service, path, iface, prop, "--verbos"));

    }

    @ShellMethod(key = "bs.monitor", value = "eg: bs.monitor output-filename")
    @ShellMethodAvailability("availabilityCheck")
    public void monitor(String filename) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            redirector(outputStream, () -> scmd("busctl monitor"));
        } catch (Exception e) {

        }
        new FileOutputStream(new File(filename)).write(outputStream.toByteArray());
        System.out.println("Content is available in " + filename);
    }

    @ShellMethod(key = "bs.serial_number", value = "eg: bs.serial_number")
    @ShellMethodAvailability("availabilityCheck")
    public void serial_number() throws IOException {
        String commd = " busctl get-property xyz.openbmc_project.Inventory.Manager /xyz/openbmc_project/inventory/system xyz.openbmc_project.Inventory.Decorator.Asset SerialNumber";
        scmd(commd);
    }

}
