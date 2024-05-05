package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
@ShellComponent
public class SystemCommands extends CommonCommands{
    protected SystemCommands() throws IOException {
    }
    @ShellMethod(key = "sys.ls")
    protected void list(@ShellOption(value = {"--args", "-a"},defaultValue="") String args) throws IOException, InterruptedException {
        system(String.format("ls -%s .",args));
    }
    @ShellMethod(key = "sys.cat")
    protected void cat(String file) throws IOException, InterruptedException {
        system(String.format("cat %s",file));
    }
    @ShellMethod(key = "sys.pwd")
    protected void pwd() throws IOException, InterruptedException {
        system("pwd");
    }
    @ShellMethod(key = "sys.ping")
    protected void ping(String machine) throws IOException, InterruptedException {
        system(String.format("ping %s",machine));
    }
    @ShellMethod(key = "sys.scpjar")
    protected void scpjar() throws IOException, InterruptedException {
        scpfile("target/bmcshell-0.0.1-SNAPSHOT.jar");
    }
    @ShellMethod(key = "sys.scpfile")
    protected void scpfile(String file) throws IOException, InterruptedException {
        system(String.format("scp %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/",file));
    }
    @ShellMethod(key = "sys.wget")
    protected void wget(String url) throws IOException, InterruptedException {
        system(String.format("wget %s",url));
    }

}
