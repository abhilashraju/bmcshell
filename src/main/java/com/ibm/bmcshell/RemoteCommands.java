package com.ibm.bmcshell;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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

import com.ibm.bmcshell.DbusCommnads.InterfaceProvider;
import com.ibm.bmcshell.RemoteCommands.ServiceProvider;
import com.ibm.bmcshell.Utils.Util;

@ShellComponent
public class RemoteCommands extends CommonCommands {
    @Component
    public static class ServiceProvider implements ValueProvider {
        public static List<String> serviceNames;

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

    protected RemoteCommands() throws IOException {

    }

    @ShellMethod(key = "ro.ls", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void ls(String path) {
        scmd(String.format("ls -alhS %s", path));
    }

    @ShellMethod(key = "ro.mv", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void mv(String source, String dest) {
        scmd(String.format("mv %s %s", source, dest));
    }

    @ShellMethod(key = "ro.cmd", value = "eg: ro.cmd command")
    @ShellMethodAvailability("availabilityCheck")
    void cmd(String cmd) {
        scmd(cmd);
    }

    @ShellMethod(key = "ro.cat", value = "eg: ro.cmd filepath")
    @ShellMethodAvailability("availabilityCheck")
    void cat(String p) {
        scmd(String.format("cat %s", p));
    }

    @ShellMethod(key = "ro.service.list", value = "eg: ro.service.list")
    @ShellMethodAvailability("availabilityCheck")
    void serviceList() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("systemctl list-units --type=service"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream2, Util.fullMachineName(machine), userName, passwd,
                    String.format("ls -alhS /etc/systemd/system"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ServiceProvider.serviceNames = extractServiceNamesFromSysctl(outputStream.toString());
        var filenames = parseFilenamesFromls(outputStream2.toString());
        filenames.stream().filter(a -> !ServiceProvider.serviceNames.contains(a))
                .forEach(a -> ServiceProvider.serviceNames.add(a));
        System.out.println("Services fetched");
    }

    @ShellMethod(key = "ro.service", value = "eg: ro.service servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "") String s) throws IOException {

        if (ServiceProvider.serviceNames == null) {
            serviceList();
        }

        ServiceProvider.serviceNames.stream().filter(nm -> {
            if (s == null || s.isEmpty())
                return true;
            else
                return nm.contains(s);
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

    @ShellMethod(key = "ro.find", value = "eg: ro.find filename [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void findFile(String filename,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("find %s -iname %s", path, filename));
    }

    @ShellMethod(key = "ro.grep", value = "eg: ro.grep pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void grep(String pattern,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
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
    void makeFile(String path, String data) {
        scmd(String.format("mkdir -p %s", path.substring(0, path.lastIndexOf('/'))));
        scmd(String.format("chmod 777 %s;echo %s > %s", path, data, path));
        scmd(String.format("ls -lh %s", path));
    }

    @ShellMethod(key = "ro.digest", value = "eg: ro.digest path to file")
    @ShellMethodAvailability("availabilityCheck")
    void digest(String path) {
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
