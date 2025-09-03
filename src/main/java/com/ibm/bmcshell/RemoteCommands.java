package com.ibm.bmcshell;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
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

    @ShellMethod(key = "ro.service", value = "eg: ro.service servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "") String s) {
        
        if (ServiceProvider.serviceNames == null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            redirector(outputStream, () -> {
                try {
                    runCommandShort(Util.fullMachineName(machine), userName, passwd,
                            String.format("systemctl list-units --type=service"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            ServiceProvider.serviceNames = extractServiceNamesFromSysctl(outputStream.toString());
        }

        ServiceProvider.serviceNames.stream().filter(nm -> {
            if (s == null || s.isEmpty())
                return true;
            else
                return nm.contains(s);
        }).forEach(nm -> System.out.println(nm));

    }

    @ShellMethod(key = "ro.service.show", value = "eg: ro.service_show servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_show(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s,
            @ShellOption(value = { "--reg", "-r" }, defaultValue = ".") String reg) {

        scmd(String.format("systemctl show %s |grep %s", s, reg));

    }

    @ShellMethod(key = "ro.service.status", value = "eg: ro.service_status servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_status(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl status %s", s));
    }

    @ShellMethod(key = "ro.service.start", value = "eg: ro.service_start servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_start(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl start %s", s));
    }

    @ShellMethod(key = "ro.service.stop", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_stop(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl stop %s", s));
    }

    @ShellMethod(key = "ro.service.restart", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_restart(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl restart %s", s));
    }

    @ShellMethod(key = "ro.service.log", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_log(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("journalctl -u %s", s));
    }

    @ShellMethod(key = "ro.service.cat", value = "eg: ro.service.cat servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_cat(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl cat %s", s));
    }

    @ShellMethod(key = "ro.service.enable", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_enable(@ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceProvider.class) String s) {
        scmd(String.format("systemctl enable %s", s));
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

    public static List<String> extractFilenamesFromLs(String lsOutput) {
        List<String> fileNames = new ArrayList<>();
        // Split the entire output into individual lines
        String[] lines = lsOutput.split("\\r?\\n");

        // Iterate over each line
        for (String line : lines) {
            // Trim leading/trailing whitespace
            line = line.trim();
            // Skip any empty lines
            if (line.isEmpty()) {
                continue;
            }

            // A more robust way to handle the filename extraction is with a regex that
            // captures everything after the timestamp or link indicator.
            // This pattern handles standard files/directories, as well as symbolic links.
            // It looks for a sequence of non-whitespace characters, followed by a variable
            // number of
            // whitespace-separated fields, and then captures the last non-whitespace group.
            Pattern p = Pattern.compile(".*?\\s+(.*)"); // Regular expression
            Matcher m = p.matcher(line);

            if (m.matches()) {
                String potentialFilename = m.group(1);
                // Handle symbolic links: The filename is before the "->"
                if (potentialFilename.contains(" -> ")) {
                    potentialFilename = potentialFilename.substring(0, potentialFilename.indexOf(" -> "));
                }
                fileNames.add(potentialFilename);
            }
        }
        return fileNames;
    }
}
