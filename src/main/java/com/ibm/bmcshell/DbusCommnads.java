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
import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class DbusCommnads extends CommonCommands {
    String currentService;
    String currentPath;

    String currentIface;

    @Component
    public static class BusNameProvider implements ValueProvider {
        public static List<String> busnames;

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
            return null;
        }
    }

    @Component
    public static class PathNameProvider implements ValueProvider {
        public static HashMap<String, List<String>> pathnames=new HashMap<>();
        public static String currentBusname;

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            if (pathnames != null) {
                String userInput = context.currentWordUpToCursor();
                return pathnames.get(currentBusname)
                        .stream()
                        .filter(name ->name.startsWith(userInput))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            }
            return null;
        }
    }

    protected DbusCommnads() throws IOException {
    }

    @ShellMethod(key = "bs.introspect", value = "eg: bs.introspect xyz.openbmc_project.Network.Hypervisor /xyz/openbmc_project/network/hypervisor/eth0/ipv4")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(
            @ShellOption(value = { "--ser",
                    "-s" }, defaultValue = "", valueProvider = BusNameProvider.class) String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "",valueProvider = PathNameProvider.class) String path) {
        if (service.equals("")) {
            service = currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        currentService = service;
        currentPath = path;
        runCommand(Util.fullMachineName(machine), userName, passwd,
                String.format("busctl introspect %s %s", service, path));

    }

    @ShellMethod(key = "bs.search", value = "eg: bs.search network")
    @ShellMethodAvailability("availabilityCheck")
    public void search(@ShellOption(valueProvider = BusNameProvider.class, value = { "--ser", "-s" }) String service)
            throws JsonParseException, JsonProcessingException {
        if (BusNameProvider.busnames == null) {
            getBusNames();
        }
        BusNameProvider.busnames.stream().filter(a -> a.toLowerCase().contains(service.toLowerCase()))
                .forEach(n -> tree(n));
    }

    @ShellMethod(key = "bs.list", value = "eg: bs.lsit ")
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
            currentService = service;
            PathNameProvider.currentBusname = service;
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
        if (!PathNameProvider.pathnames.containsKey(service)){
            PathNameProvider.pathnames.put(service,convertTreeToPaths(outputStream.toString()));
        }
    }

    public static List<String> convertTreeToPaths(String treeString) {
        List<String> paths = new ArrayList<>();
        // Use a stack to track the current path, but a simple array works too
        String[] pathSegments = new String[10]; // Max depth of 10, adjust if needed
        int currentDepth = -1;

        // Regular expression to find the path fragment and capture the indentation
        // level
        Pattern pattern = Pattern.compile("^(?:\\s*(?:[│├└]─\\s*))*(.*)$");

        for (String line : treeString.split("\\R")) { // Split by any line break
            if (line.isBlank()) {
                continue;
            }

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                // Calculate the depth based on indentation
                int depth = (line.indexOf("─") / 2) - 1;

                String pathSegment = matcher.group(1);

                if (depth < 0) {
                    // Handle the root node separately
                    pathSegments[0] = pathSegment;
                    currentDepth = 0;
                } else {
                    currentDepth = depth;
                    pathSegments[currentDepth] = pathSegment;
                }

                paths.add(pathSegments[currentDepth].trim());
            }
        }

        return paths;
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
    public void call(@ShellOption(value = { "--ser", "-s" }, defaultValue = "") String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "") String path,
            @ShellOption(value = { "--iface", "-i" }, defaultValue = "") String iface,
            @ShellOption(value = { "--method", "-m" }) String method,
            @ShellOption(value = { "--sig", "-s" }, defaultValue = "") String sig,
            @ShellOption(value = { "--args", "-a" }, defaultValue = "") String args) {
        if (service.equals("")) {
            service = currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iface.equals("")) {
            iface = currentIface;
            System.out.println("Interface is null, using default interface " + iface);
        }
        currentIface = iface;
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

    @ShellMethod(key = "bs.call", value = "eg: bs.call xyz.openbmc_project.ObjectMapper GetObject sas /xyz/openbmc_project/sensors/power/total_power 1 xyz.openbmc_project.Sensor.Value")
    @ShellMethodAvailability("availabilityCheck")
    public void call(@ShellOption(value = { "--iface", "-i" }, defaultValue = "") String iface,
            @ShellOption(value = { "--method", "-m" }) String method,
            @ShellOption(value = { "--sig", "-s" }, defaultValue = "") String sig,
            @ShellOption(value = { "--args", "-a" }, defaultValue = "") String args) {
        call(currentService, currentPath, iface, method, sig, args);
    }

    @ShellMethod(key = "bs.setproperty", value = "eg: bs.setproperty xyz.openbmc_project.EntityManager /xyz/openbmc_project/inventory/system/chassis/Tacoma_Rack_Controller/aggregated0 xyz.openbmc_project.Configuration.SatelliteController Hostname s Tacoma_Rack_Controller")
    @ShellMethodAvailability("availabilityCheck")
    public void setProperty(@ShellOption(value = { "--ser", "-s" }, defaultValue = "") String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "") String path,
            @ShellOption(value = { "--iface", "-i" }, defaultValue = "") String iFace,
            @ShellOption(value = { "--property", "-pn" }) String property,
            @ShellOption(value = { "--sig", "-si" }) String sig,
            @ShellOption(value = { "--args", "-a" }) String args) {
        if (service.equals("")) {
            service = currentService;
            System.out.println("Service is null, using previous service " + service);
        }
        if (path.equals("")) {
            path = currentPath;
            System.out.println("Path is null, using previous path " + path);
        }
        if (iFace.equals("")) {
            iFace = currentIface;
            System.out.println("Interface is null, using default interface " + iFace);
        }
        currentIface = iFace;
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
    public void property(@ShellOption(value = { "--ser", "-s" }, defaultValue = "") String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "") String path,
            @ShellOption(value = { "--iface", "-i" }, defaultValue = "") String iface,
            @ShellOption(value = { "--prop", "-pn" }, defaultValue = "") String prop) {
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
