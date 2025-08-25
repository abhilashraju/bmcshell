package com.ibm.bmcshell;

import java.io.FileInputStream;
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
    void service(@ShellOption(value = { "--service", "-s" }, defaultValue = "")  String s) {
        if (s == null || s.isEmpty()) {
            scmd(String.format("systemctl list-units --type=service"));
            return ;
        }
        scmd(String.format("systemctl list-units --type=service |grep %s", s));
    }
    @ShellMethod(key = "ro.service.show", value = "eg: ro.service_show servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_show(String s,
                      @ShellOption(value = { "--reg", "-r" }, defaultValue = ".") String reg) {
        
        scmd(String.format("systemctl show %s |grep %s", s,reg));
        
    }
    @ShellMethod(key = "ro.service.status", value = "eg: ro.service_status servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_status(String s) {
        scmd(String.format("systemctl status %s", s));
    }
    @ShellMethod(key = "ro.service.start", value = "eg: ro.service_start servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_start(String s) {
        scmd(String.format("systemctl start %s", s));
    }

    @ShellMethod(key = "ro.service.stop", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_stop(String s) {
        scmd(String.format("systemctl stop %s", s));
    }
    @ShellMethod(key = "ro.service.restart", value = "eg: ro.service_stop servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_restart(String s) {
        scmd(String.format("systemctl restart %s", s));
    }

    @ShellMethod(key = "ro.service.log", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_log(String s) {
        scmd(String.format("journalctl -u %s", s));
    }
    @ShellMethod(key = "ro.service.cat", value = "eg: ro.service.cat servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_cat(String s) {
        scmd(String.format("systemctl cat %s", s));
    }

    @ShellMethod(key = "ro.service.enable", value = "eg: ro.service_log servicename")
    @ShellMethodAvailability("availabilityCheck")
    void service_enable(String s) {
        scmd(String.format("systemctl enable %s", s));
    }
    
    @ShellMethod(key = "ro.find", value = "eg: ro.find filename [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void findFile(String filename ,
                  @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("find %s -iname %s", path, filename));
    }
    @ShellMethod(key = "ro.grep", value = "eg: ro.grep pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void grep(String pattern ,
                  @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("grep -nr %s %s", pattern, path));
    }
    @ShellMethod(key = "ro.running", value = "eg: ro.running pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void running(String pattern ,
                  @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("ps | grep %s", pattern));
    }
    @ShellMethod(key = "ro.makefile", value = "eg: ro.makefile path content")
    @ShellMethodAvailability("availabilityCheck")
    void makeFile(String path ,String data) {
        scmd(String.format("mkdir -p %s", path.substring(0, path.lastIndexOf('/'))));
        scmd(String.format("chmod 777 %s;echo %s > %s",path,data, path));
        scmd(String.format("ls -lh %s",path));
    }
    @ShellMethod(key = "ro.digest", value = "eg: ro.digest path to file")
    @ShellMethodAvailability("availabilityCheck")
    void digest(String path) {
         scmd(String.format("openssl dgst -sha256 %s",path));
    }
    @ShellMethod(key = "ro.ping", value = "eg: ro.ping ip")
    @ShellMethodAvailability("availabilityCheck")
    void ping(String ip) {
        scmd(String.format("ping -c 1 %s",ip));
    }
    
}
