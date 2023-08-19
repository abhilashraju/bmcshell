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

    @ShellMethod(key="bs.manged_objects")
    @ShellMethodAvailability("availabilityCheck")
    public void managed_objects(String service,String path){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl call %s %s org.freedesktop.DBus.ObjectManager GetManagedObjects --verbose",service,path));
    }
    @ShellMethod(key="bs.subtree")
    @ShellMethodAvailability("availabilityCheck")
    public void subtree(String obj,int depth,String iFace){
        runCommand(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,String.format("busctl call xyz.openbmc_project.ObjectMapper /xyz/openbmc_project/object_mapper xyz.openbmc_project.ObjectMapper %s %d %s GetSubTree --verbose",obj,depth,iFace));
    }




}
