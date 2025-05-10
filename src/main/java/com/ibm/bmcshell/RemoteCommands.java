package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class RemoteCommands extends CommonCommands {
    protected RemoteCommands() throws IOException {

    }

    @ShellMethod(key = "ro.ls", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void ls(String path) {
        scmd(String.format("ls -a %s", path));
    }

    @ShellMethod(key = "ro.mv", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String source, String dest) {
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
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "")  String s) {
        if (s == null || s.isEmpty()) {
            scmd(String.format("systemctl list-units --type=service"));
            return ;
        }
        scmd(String.format("systemctl list-units --type=service |grep %s", s));
    }
    @ShellMethod(key = "ro.service_show", value = "eg: ro.service_show servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_show(String s) {
        scmd(String.format("systemctl show %s", s));
    }
    @ShellMethod(key = "ro.service_status", value = "eg: ro.service_status servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_status(String s) {
        scmd(String.format("systemctl status %s", s));
    }
    @ShellMethod(key = "ro.service_start", value = "eg: ro.service_start servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_start(String s) {
        scmd(String.format("systemctl start %s", s));
    }

    @ShellMethod(key = "ro.service_stop", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_stop(String s) {
        scmd(String.format("systemctl stop %s", s));
    }

    @ShellMethod(key = "ro.service_log", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_log(String s) {
        scmd(String.format("journalctl -u %s", s));
    }
}
