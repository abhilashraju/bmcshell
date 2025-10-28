package com.ibm.bmcshell;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.stream.Stream;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

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

    @ShellMethod(key = "opkg.install", value = "eg: opkg.install")
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
        Stream.of(lines).forEach(a -> {
            System.out.println(a);
            a.trim();
            String[] paths = a.split("/");
            String pkgname = paths[paths.length - 1];
            pkgname = pkgname.split("_")[0];

            String command = String.format(
                    "opkg remove %s; opkg install --force-depends %s",
                    pkgname, a);
            System.out.println(command);
            scmd(command);
            scmd(String.format("rm %s", a));
        });
        // Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter image id from above : ");
        // String imageid = scanner.nextLine();
        // String[] paths = imageid.split("/");
        // String pkgname = paths[paths.length - 1];
        // pkgname = pkgname.split("_")[0];

        // String command = String.format(
        //         "opkg remove %s; opkg install --force-depends %s",
        //         pkgname, imageid);
        // System.out.println(command);
        // scmd(command);
        // scmd(String.format("rm %s", imageid));
    }

    @ShellMethod(key = "opkg.copy", value = "eg: opkg.copy path_to_ipk_file")
    void opkgCopy(String path) throws InterruptedException {
        String fileName = path.split("/")[path.split("/").length - 1];
        scp(path, String.format("/tmp/%s", fileName));
    }

    @ShellMethod(key = "uploadimage", value = "eg: uploadimage imagepath . To flash images")
    void upload(String imagepath) {
        String url = String.format("https://%s.aus.stglabs.ibm.com/redfish/v1/UpdateService/update", machine);
        String token = getToken();
        String filePath = imagepath; // path argument is the file to upload
        try {
            File file = new File(filePath);
            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .build();

            String response = webClient.post()
                    .uri("")
                    .header("X-Auth-Token", token)
                    .header("Content-Type", "application/octet-stream")
                    .bodyValue(Files.newInputStream(file.toPath()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(Throwable::printStackTrace)
                    .block();
            System.out.println("Response: " + response);
            return;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ShellMethod(key = "scp", value = "eg: scp filepath/filename. Will copy the file content to the /tmp/ folder in the remote machine")
    @ShellMethodAvailability("availabilityCheck")
    void scp(String path, String dest) {
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

}
