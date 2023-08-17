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
    @ShellMethod(key="bs.introspect")
    @ShellMethodAvailability("availabilityCheck")
    public void introspect(String service,String path){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl introspect %s %s",service,path));

    }
    @ShellMethod(key="bs.search")
    @ShellMethodAvailability("availabilityCheck")
    public void search(String service){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl list | grep %s",service));

    }
    @ShellMethod(key="bs.tree")
    @ShellMethodAvailability("availabilityCheck")
    public void tree(String service){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl tree %s",service));
    }
    @ShellMethod(key="bs.bios_introspect")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_introspect(){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl introspect xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager"));
    }
    @ShellMethod(key="bs.bios_table")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_table(){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl call xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager org.freedesktop.DBus.Properties Get ss xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable"));
    }
    @ShellMethod(key="bs.bios_table_property")
    @ShellMethodAvailability("availabilityCheck")
    public void bios_table_property(){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl get-property xyz.openbmc_project.BIOSConfigManager /xyz/openbmc_project/bios_config/manager xyz.openbmc_project.BIOSConfig.Manager BaseBIOSTable --verbose"));
    }




}
