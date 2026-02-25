package com.ibm.bmcshell;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.stream.Stream;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.Utils.Util;
import com.ibm.bmcshell.ssh.SSHShellClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import java.nio.file.Files;
import java.nio.file.Paths;

@ShellComponent
public class InstallCommands extends CommonCommands {
    protected InstallCommands() throws IOException {
    }

    @ShellMethod(key = "install.mount", value = "eg: install.mount")
    void mount() {

        scmd("mkdir -p /tmp/persist/usr ; mkdir -p /tmp/persist/work/usr;mount -t overlay  -o lowerdir=/usr,upperdir=/tmp/persist/usr,workdir=/tmp/persist/work/usr overlay /usr");

    }

    @ShellMethod(key = "install.flash", value = "eg: install.flash . To flash images")
    void flash() throws InterruptedException {
        scmd("mv /tmp/obmc-phosphor-image-p10bmc.ext4.mmc.tar /tmp/images");
        sleep(1);
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

    @ShellMethod(key = "install.opkg", value = "eg: install.opkg.install")
    void opkgInstall() throws InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    String.format("ls /tmp/*.ipk"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String output = outputStream.toString();
        var lines = output.split("\n");
        Stream.of(lines).filter(a->a.endsWith(".ipk")).forEach(a -> {
            System.out.println(a);
            a.trim();
            String[] paths = a.split("/");
            String pkgname = paths[paths.length - 1];
            pkgname = pkgname.split("_")[0];

            String command = String.format(
                    "opkg remove --force-remove %s;opkg install --force-reinstall --force-depends %s",
                    pkgname, a);
            System.out.println(command);
            scmd(command);
            scmd(String.format("rm %s", a));
        });
        
    }


    @ShellMethod(key = "install.scp", value = "eg: install.scp filepath/filename. Will copy the file content to the /tmp/ folder in the remote machine")
    @ShellMethodAvailability("availabilityCheck")
    void scp(@ShellOption(value = { "--path", "-p"}, defaultValue = "/",valueProvider = FileCompleter.class) String path, String dest) {
        System.out.println(passwd);
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
        cmdBuilder.append(":/tmp");

        system(cmdBuilder.toString());
        var subpaths = dest.split("/");
        scmd(String.format("mkdir -p %s", dest.substring(0, dest.lastIndexOf('/'))));

        scmd(String.format("chmod 777 /tmp/%s; mv /tmp/%s %s", subpaths[subpaths.length - 1],
                subpaths[subpaths.length - 1], dest));
    }

    @ShellMethod(key = "install.check-bootside", value = "Check the current boot side")
    @ShellMethodAvailability("availabilityCheck")
    void checkBootside() {
        System.out.println("Checking current boot side...");
        scmd("fw_printenv bootside");
    }
    @ShellMethod(key = "install.set-bootside", value = "Check the current boot side")
    @ShellMethodAvailability("availabilityCheck")
    void setBootside(String bootside) {
        System.out.println("Setting current boot side...");
        scmd(String.format("fw_setenv bootside %s", bootside));
    }
    @ShellMethod(key = "install.update-firmware", value = "Update firmware with specified label. eg: install.update-firmware --label b")
    @ShellMethodAvailability("availabilityCheck")
    void updateFirmware(@ShellOption(value = { "--label", "-l" }) String label) throws IOException {
        
        String imagePath = "/tmp/images";
        
        // List available versions in /tmp/images
        System.out.println("Listing available versions in " + imagePath + "...");
        scmd("ls " + imagePath);
        
        // Prompt user to select version
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter version from above: ");
        String version = scanner.nextLine().trim();
        
        System.out.println("Loading firmware update script from resources...");
        
        // Read the script from resources
        InputStream scriptStream = getClass().getClassLoader().getResourceAsStream("update_firmware.sh");
        if (scriptStream == null) {
            throw new IOException("Could not find update_firmware.sh in resources");
        }
        
        String scriptContent = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
        scriptStream.close();
        
        // Write script to local temporary file
        String localScriptPath = "/tmp/update_firmware.sh";
        File scriptFile = new File(localScriptPath);
        FileOutputStream fout = new FileOutputStream(scriptFile);
        fout.write(scriptContent.getBytes(StandardCharsets.UTF_8));
        fout.close();
        
        System.out.println("Script loaded from resources");
        System.out.println("Copying script to remote machine...");
        
        // Copy script to remote machine
        String remoteScriptPath = "/tmp/update_firmware.sh";
        scp(localScriptPath, remoteScriptPath);
        
        System.out.println("Making script executable...");
        scmd("chmod +x " + remoteScriptPath);
        
        System.out.println("Executing firmware update script...");
        scmd(String.format("%s %s %s %s", remoteScriptPath, label, imagePath, version));
        
        System.out.println("Firmware update completed!");
        setBootside(label);
    }

}