package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@ShellComponent
public class SystemCommands extends CommonCommands {
    protected SystemCommands() throws IOException {
    }

    @ShellMethod(key = "sys.ls")
    protected void list(@ShellOption(value = { "--args", "-a" }, defaultValue = "") String args)
            throws IOException, InterruptedException {
        system(String.format("ls -%s .", args));
    }

    @ShellMethod(key = "sys.cat")
    protected void cat(String file) throws IOException, InterruptedException {
        system(String.format("cat %s", file));
    }

    @ShellMethod(key = "sys.pwd")
    protected void pwd() throws IOException, InterruptedException {
        system("pwd");
    }

    @ShellMethod(key = "sys.ping")
    protected void ping(String machine) throws IOException, InterruptedException {
        system(String.format("ping %s", machine));
    }

    @ShellMethod(key = "sys.scpjar")
    protected void scpjar() throws IOException, InterruptedException {
        scpfile("target/bmcshell-0.0.1-SNAPSHOT.jar");
    }

    @ShellMethod(key = "sys.scpfile")
    protected void scpfile(String file) throws IOException, InterruptedException {
        system(String.format("scp %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/", file));
    }

    @ShellMethod(key = "sys.scpdir")
    protected void scpDir(String file) throws IOException, InterruptedException {
        system(String.format("scp -r %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/", file));
    }

    @ShellMethod(key = "sys.deployweb")
    protected void scpweb(String file) throws IOException, InterruptedException {
        system(String.format("scp -r %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/bmcshelllibrary/",
                file));
    }

    @ShellMethod(key = "sys.wget")
    protected void wget(String url) throws IOException, InterruptedException {
        system(String.format("wget %s", url));
    }

    @ShellMethod(key = "sys.cmd", value = "eg: sy.cmd command")
    void cmd(String cmd) {
        system(cmd);
    }

    @ShellMethod(key = "ts.send")
    protected void send(String commands, @ShellOption(value = { "--id", "-i" }, defaultValue = "-1") String id) {
        System.out.println("Sending to " + id);
        try {
            // Get the resource as a stream
            InputStream resourceAsStream = getClass().getResourceAsStream("/tsend.sh");

            // Create a temporary file
            Path tempFile = Files.createTempFile("tsend", ".sh");

            // Copy the resource to the temporary file
            Files.copy(resourceAsStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Make the temporary file executable
            tempFile.toFile().setExecutable(true);

            // Specify the path to the shell script
            String scriptPath = tempFile.toString();

            // Specify the arguments
            String arg1 = id;
            String arg2 = commands;

            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath, arg1, arg2);

            // Start the process
            Process process = processBuilder.start();

            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Print the exit code
            System.out.println("Script executed with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
