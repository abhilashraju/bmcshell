package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;
@ShellComponent
public class DbusCommnads extends CommonCommands{
    protected DbusCommnads() throws IOException {
    }
    @ShellMethod(key="bs.introspect",value = "eg: bs.introspect xyz.openbmc_project.Network.Hypervisor /xyz/openbmc_project/network/hypervisor/eth0/ipv4")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(String service,String path){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl introspect %s %s",service,path));

    }
    @ShellMethod(key="bs.search",value = "eg: bs.search network")
    @ShellMethodAvailability("availabilityCheck")
    public void search(String service){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl list | grep %s",service));

    }
    @ShellMethod(key="bs.tree",value = "eg: bs.tree xyz.openbmc_project.Network.Hypervisor")
    @ShellMethodAvailability("availabilityCheck")
    public void tree(String service){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl tree %s",service));
    }

    @ShellMethod(key="bs.managed_objects")
    @ShellMethodAvailability("availabilityCheck")
    public void managed_objects(String service,String path){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl call %s %s org.freedesktop.DBus.ObjectManager GetManagedObjects --verbose",service,path));
    }
    @ShellMethod(key="bs.subtree" ,value = "eg: bs.subtree /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void subtree(String obj,int depth,String iFace){
        execMapperCall(iFace, "GetSubTree", obj, depth);
    }
    @ShellMethod(key="bs.subtreepaths" ,value = "eg: bs.subtreepaths /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void GetSubTreePaths(String obj,int depth,String iFace){
        execMapperCall(iFace,"GetSubTreePaths" , obj, depth);
    }
    @ShellMethod(key="bs.associatedsubtreepaths" ,value = "eg: bs.associatedsubtree /xyz/openbmc_project/inventory 0 xyz.openbmc_project.Inventory.Item.Cable")
    @ShellMethodAvailability("availabilityCheck")
    public void GetAssociatedSubTreePaths(String obj,int depth,String iFace){
        execMapperCall(iFace,"GetAssociatedSubTreePaths" , obj, depth);
    }
    @ShellMethod(key="bs.object" ,value = "eg: bs.object /xyz/openbmc_project/sensors/power/total_power xyz.openbmc_project.Sensor.Value")
    @ShellMethodAvailability("availabilityCheck")
    public void getObject(String obj,String iFace){
        var format="busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas %s %d";
        var infaces = iFace.split(",");
        var commd = String.format(format,obj, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd + " --verbose";
        scmd(commd);
    }

    @ShellMethod(key="bs.call" ,value = "eg: bs.call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper GetObject sas /xyz/openbmc_project/sensors/power/total_power 1 xyz.openbmc_project.Sensor.Value")
    @ShellMethodAvailability("availabilityCheck")
    public void call(String service,String path,String iFace,String method,String sig, String args){
        var format="busctl call %s %s %s %s %s %s";

        var commd = String.format(format,service,path,iFace,method,sig, args);
        commd = commd + " --verbose";
        scmd(commd);
    }

    private void execMapperCall(String iFace, String method, String obj, int depth) {
        var format="busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper %s sias %s %d %d";
        var infaces = iFace.split(",");
        var commd = String.format(format,method, obj, depth, infaces.length);
        for (var iF : infaces) {
            commd = commd + " " + iF;
        }
        commd = commd + " --verbose";
        scmd(commd);
    }

    @ShellMethod(key="bs.property",value = "eg: bs.property xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable true")
    @ShellMethodAvailability("availabilityCheck")
    public void property(String service,String path,String iface, String prop,boolean verbose){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl get-property %s %s %s %s %s",service,path,iface,prop,verbose?"--verbos":""));

    }
    @ShellMethod(key="bs.monitor",value = "eg: bs.monitor output-filename")
    @ShellMethodAvailability("availabilityCheck")
    public void monitor(String filename) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            redirector(outputStream,()->scmd("busctl monitor"));
        }catch (Exception e){

        }
        new FileOutputStream(new File(filename)).write(outputStream.toByteArray());
        System.out.println("Content is available in "+filename);
    }




}
