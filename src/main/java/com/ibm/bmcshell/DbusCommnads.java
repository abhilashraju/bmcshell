package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;

@ShellComponent
public class DbusCommnads extends CommonCommands {
    String currentService;
    String currentPath;

    String currentIface;
   
    protected DbusCommnads() throws IOException {
    }
   
    @ShellMethod(key = "bs.introspect", value = "eg: bs.introspect xyz.openbmc_project.Network.Hypervisor /xyz/openbmc_project/network/hypervisor/eth0/ipv4")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(@ShellOption(value = { "--ser", "-s" }, defaultValue = "") String service,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "") String path) {
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
    public void search(String service) {
        runCommand(Util.fullMachineName(machine), userName, passwd, String.format("busctl list | grep %s", service));

    }

    @ShellMethod(key = "bs.tree", value = "eg: bs.tree xyz.openbmc_project.Network.Hypervisor")
    @ShellMethodAvailability("availabilityCheck")
    public void tree(String service) {
        if (!service.equals("*")) {
            currentService = service;
        }
        runCommand(Util.fullMachineName(machine), userName, passwd,
                String.format("busctl tree %s", service.equals("*") ? "" : service));
    }

    @ShellMethod(key = "bs.managed_objects", value = "eg: bs.managed_objects xyz.openbmc_project.Network /xyz/openbmc_project/network")
    @ShellMethodAvailability("availabilityCheck")
    public void managed_objects(String service, String path) {
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
        if (args != null){

            var str=Arrays.stream(args.split("\\|")).map(a->"\""+a+"\" ").reduce((a,b)->a+b).orElse("");
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
        String commd=" busctl get-property xyz.openbmc_project.Inventory.Manager /xyz/openbmc_project/inventory/system xyz.openbmc_project.Inventory.Decorator.Asset SerialNumber";
        scmd(commd);
    }
   

}
