package com.ibm.bmcshell;

import com.ibm.bmcshell.ssh.SSHShellClient;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.IOException;
@ShellComponent
public class RemoteCommands extends CommonCommands{
    protected RemoteCommands() throws IOException {

    }
    @ShellMethod(key = "ro.ls",value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String path) {
        scmd(String.format("ls %s",path));
    }
    @ShellMethod(key = "ro.mv",value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String source,String dest) {
        scmd(String.format("mv %s %s",source,dest));
    }
}
