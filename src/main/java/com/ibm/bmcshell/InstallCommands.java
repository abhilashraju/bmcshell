package com.ibm.bmcshell;

import com.ibm.bmcshell.Utils.Util;
import com.ibm.bmcshell.ssh.SSHShellClient;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

@ShellComponent
public class InstallCommands extends CommonCommands {
    protected InstallCommands() throws IOException {
    }

    @ShellMethod(key = "update_version", value = "eg update_version To update version of an image")
    public void updateImageVersion(String imagepath, String name) throws IOException {
        system(String.format("tar -xvf %s MANIFEST", imagepath));
        // Modify the MANIFEST with a valid version, ex:
        var f = new FileInputStream(new File("MANIFEST"));
        var towrite = Arrays.stream(new String(f.readAllBytes()).split("\n")).map(a -> {
            System.out.println(a);
            if (a.contains("version") || a.contains("ExtendedVersion")) {
                var b = a.split("=");
                return b[0] + "=" + name;
            }

            return a;
        }).reduce((a, b) -> a + "\n" + b).orElse("empty");
        System.out.println(towrite);
        var fout = new FileOutputStream(new File("MANIFEST"));
        fout.write(towrite.getBytes(StandardCharsets.UTF_8));
        // Add the MANIFEST back into the tarball:
        system(String.format("tar --append --file=%s MANIFEST", imagepath));

    }

    @ShellMethod(key = "install", value = "eg: install service-exe-name . To install service")
    void install(String exe) {
        scmd("mkdir -p /tmp/usr/bin");
        scmd(String.format("mv /tmp/%s /tmp/usr/bin", exe));
        scmd("mkdir -p /tmp/work ;mount -t overlay  -o lowerdir=/usr,upperdir=/tmp/usr,workdir=/tmp/work overlay /usr");
        // scmd("mkdir -p /tmp/persist/usr ; mkdir -p /tmp/persist/work/usr;mount -t
        // overlay -o
        // lowerdir=/usr,upperdir=/tmp/persist/usr,workdir=/tmp/persist/work/usr overlay
        // /usr");

    }

    @ShellMethod(key = "mount", value = "eg: mount")
    void mount() {

        scmd("mkdir -p /tmp/persist/usr ; mkdir -p /tmp/persist/work/usr;mount -t overlay  -o lowerdir=/usr,upperdir=/tmp/persist/usr,workdir=/tmp/persist/work/usr overlay /usr");

    }

    @ShellMethod(key = "flash", value = "eg: flash . To flash images")
    void flash() throws InterruptedException {

        scmd("ls /tmp/images");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter image id from above : ");
        String imageid = scanner.nextLine();
        String command = String.format(
                "busctl set-property xyz.openbmc_project.Software.BMC.Updater /xyz/openbmc_project/software/%s xyz.openbmc_project.Software.Activation RequestedActivation s xyz.openbmc_project.Software.Activation.RequestedActivations.Active",
                imageid);
        System.out.println(command);
        scmd(command);
    }

    @ShellMethod(key = "uploadimage", value = "eg: uploadimage imagepath . To flash images")
    void upload(String imagepath) {
        scp(imagepath);
        var subpaths = imagepath.split("/");
        scmd(String.format("mv /tmp/%s /tmp/images", subpaths[subpaths.length - 1]));
    }

    @ShellMethod(key = "scp", value = "eg: scp filepath/filename. Will copy the file content to the /tmp/ folder in the remote machine")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String path) {
        StringBuilder cmdBuilder = new StringBuilder();

        cmdBuilder.append("scp ");
        cmdBuilder.append("-o StrictHostKeyChecking=no ");
        cmdBuilder.append("-o UserKnownHostsFile=/dev/null ");
        cmdBuilder.append(String.format("-P %d ", SSHShellClient.port));
        cmdBuilder.append(path);
        cmdBuilder.append(" ");
        cmdBuilder.append(userName);
        cmdBuilder.append("@");
        cmdBuilder.append(Util.fullMachineName(machine));
        cmdBuilder.append(":/tmp/");

        system(cmdBuilder.toString());
    }
}
