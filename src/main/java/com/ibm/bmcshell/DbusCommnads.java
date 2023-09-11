package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

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
    @ShellMethod(key="bs.subtree")
    @ShellMethodAvailability("availabilityCheck")
    public void subtree(String obj,int depth,String iFace){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper %s %d %s GetSubTree --verbose",obj,depth,iFace));
    }

    @ShellMethod(key="bs.property",value = "eg: bs.property xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable true")
    @ShellMethodAvailability("availabilityCheck")
    public void property(String service,String path,String iface, String prop,boolean verbose){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl get-property %s %s %s %s %s",service,path,iface,prop,verbose?"--verbos":""));

    }




}
