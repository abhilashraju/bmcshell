package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.Utils.Util;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Function;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
import static com.ibm.bmcshell.ssh.SSHShellClient.runShell;

@ShellComponent
public class VMICommands extends CommonCommands {

    private final DbusCommnads dbusCommnads;

    protected VMICommands(DbusCommnads buscommand) throws IOException {
        dbusCommnads = buscommand;
    }

    @ShellMethod(key = "vmi")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi() throws URISyntaxException, IOException {

        execute(new Util.EndPoints("Systems/hypervisor", "Get"), "", false, "", false);

    }

    @ShellMethod(key = "vmi_eth_interfaces")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_eth_interfaces() throws URISyntaxException, IOException {

        execute(new Util.EndPoints("Systems/hypervisor/EthernetInterfaces", "Get"), "", false, "", false);

    }

    @ShellMethod(key = "vmi_eth_interface")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_eth_interface(String iface, @ShellOption(value = { "--data", "-d" }, defaultValue = "") String d,
            @ShellOption(value = { "--file", "-f" }, defaultValue = "") String f,
            @ShellOption(value = { "--patch", "-p" }, defaultValue = "false") boolean p)
            throws URISyntaxException, IOException {

        if (!f.isEmpty()) {
            var stream = new FileInputStream(new File(f));
            d = new String(stream.readAllBytes());
            stream.close();
        }
        execute(new Util.EndPoints("Systems/hypervisor/EthernetInterfaces/" + iface, "Get"), d, p, "", false);

    }

    @ShellMethod(key = "vmi.ssh")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_ssh() {
        runShell(String.format("%s.aus.stglabs.ibm.com", machine), userName, passwd, 2201);
        System.out.println("Exited Shell");
        displayCurrent();

    }

    @ShellMethod(key = "vmi.bios_introspect")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_introspect() {
        runCommand(String.format("%s.aus.stglabs.ibm.com", machine), userName, passwd, String.format(
                "busctl introspect xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager --verbose"));
    }

    @ShellMethod(key = "vmi.bios_table")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_table() {
        runCommand(String.format("%s.aus.stglabs.ibm.com", machine), userName, passwd, String.format(
                "busctl call xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager org.freedesktop.DBus.Properties Get ss xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable --verbose"));
    }

    @ShellMethod(key = "vmi.bios_table_property", value = "eg; vmi.bios_table_property -s vmi_if1_ipv6_ipaddr,vmi_if1_ipv4_ipaddr")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_table_property(@ShellOption(value = { "--search", "-s" }, defaultValue = "") String search) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        redirector(outputStream, () -> dbusCommnads.property("xyz.openbmc_project.BIOSConfigManager",
                "/xyz/openbmc_project/bios_config/manager", "xyz.openbmc_project.BIOSConfig.Manager", "BaseBIOSTable"));
        if (search.isEmpty()) {
            System.out.println(outputStream.toString());
            return;
        }
        Arrays.stream(search.split(",")).forEach(a -> {
            printSelected(a, outputStream.toString());
        });

    }

    private static void printSelected(String search, String capturedOutput) {
        var arry = capturedOutput.split("\n");
        int i = 0;
        while (i < arry.length) {
            if (arry[i].contains(search)) {
                break;
            }
            i++;
        }
        if (i < arry.length) {
            Arrays.stream(arry).skip(i).takeWhile((a) -> !a.contains("DICT_ENTRY"))
                    .forEach((a) -> System.out.println(a));
        }
    }

}
