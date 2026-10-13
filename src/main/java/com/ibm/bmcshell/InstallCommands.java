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
import org.springframework.core.io.FileSystemResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.net.URI;
import reactor.core.publisher.Mono;
import java.nio.file.Files;
import java.nio.file.Paths;

@ShellComponent
public class InstallCommands extends CommonCommands {
    protected InstallCommands() throws IOException {
    }

    @ShellMethod(key = "install.mount", value = "eg: install.mount")
    void mount() {

        scmd("sudo sh -c 'mkdir -p /tmp/persist/usr && mkdir -p /tmp/persist/work/usr && mount -t overlay -o lowerdir=/usr,upperdir=/tmp/persist/usr,workdir=/tmp/persist/work/usr overlay /usr'");

    }

    /**
     * Fetches the first available firmware version from the specified image path.
     *
     * @param imagePath The path to the directory containing firmware images
     * @return The first available firmware version/image ID
     * @throws IOException If no images are found or if there's an error listing
     *                     images
     */
    private String getFirstAvailableVersion(String imagePath) throws IOException {
        System.out.println("Listing available versions in " + imagePath + "...");
        ByteArrayOutputStream lsOutputStream = new ByteArrayOutputStream();
        try {
            runCommandShort(lsOutputStream, Util.fullMachineName(machine), userName, passwd, "ls " + imagePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list images", e);
        }

        String lsOutput = lsOutputStream.toString().trim();
        System.out.println("Available images:\n" + lsOutput);

        // Get the first image from the list
        String[] images = lsOutput.split("\n");
        if (images.length == 0 || images[0].trim().isEmpty()) {
            throw new IOException("No images found in " + imagePath);
        }

        String version = images[0].trim();
        System.out.println("Selected image: " + version);
        return version;
    }

    @ShellMethod(key = "install.flash", value = "eg: install.flash [--tag <imageTag>] . To flash images")
    @ShellMethodAvailability("availabilityCheck")
    void flash(@ShellOption(value = { "--tag", "-t" }, defaultValue = ShellOption.NULL, help = "Image ID / version tag to flash") String tag)
            throws InterruptedException, IOException {
        String imagePath = "/tmp/images";
        String imageid;

        if (tag != null && !tag.trim().isEmpty()) {
            imageid = tag.trim();
            System.out.println("Using specified image tag: " + imageid);
        } else {
            // Get the first available version
            imageid = getFirstAvailableVersion(imagePath);
        }

        String command = String.format(
                "busctl set-property xyz.openbmc_project.Software.BMC.Updater /xyz/openbmc_project/software/%s xyz.openbmc_project.Software.Activation RequestedActivation s xyz.openbmc_project.Software.Activation.RequestedActivations.Active",
                imageid);
        System.out.println(command);
        scmd(command);
    }

    @ShellMethod(key = "install.image", value = "Install/flash an image by tag/ID. eg: install.image --tag <imageTag>")
    @ShellMethodAvailability("availabilityCheck")
    void installImage(
            @ShellOption(value = { "--tag", "-t" }, help = "Image ID / version tag to install") String tag)
            throws InterruptedException, IOException {
        if (tag == null || tag.trim().isEmpty()) {
            System.err.println("❌ Error: Image tag cannot be empty.");
            System.err.println("Usage: install.image --tag <imageTag>");
            return;
        }

        String imageid = tag.trim();
        System.out.println("Activating image with tag: " + imageid);

        String command = String.format(
                "busctl set-property xyz.openbmc_project.Software.BMC.Updater /xyz/openbmc_project/software/%s xyz.openbmc_project.Software.Activation RequestedActivation s xyz.openbmc_project.Software.Activation.RequestedActivations.Active",
                imageid);
        System.out.println(command);
        scmd(command);
    }

    @ShellMethod(key = { "install.firmware_update", "fu" }, value = "Push a firmware image via Redfish UpdateService and monitor the task. eg: fu --file ~/obmc-phosphor-image.tar [--no-wait] [--timeout 1800]")
    @ShellMethodAvailability("availabilityCheck")
    void firmwareUpdate(
            @ShellOption(value = { "--file", "-f" }, valueProvider = FileCompleter.class) String filePath,
            @ShellOption(value = "--no-wait", defaultValue = "false", help = "Do not monitor the update task") boolean noWait,
            @ShellOption(value = "--timeout", defaultValue = "1800", help = "Monitoring timeout in seconds") int timeoutSec)
            throws URISyntaxException {
        File file = new File(filePath);
        if (!file.isFile()) {
            System.err.println("❌ Error: File not found: " + filePath);
            return;
        }
        var uri = new URI(base() + "/redfish/v1/UpdateService/update");
        System.out.println("Uploading " + file.getName() + " to " + uri + " ...");
        try {
            var response = client.post()
                    .uri(uri)
                    .header("X-Auth-Token", token)
                    .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(BodyInserters.fromResource(new FileSystemResource(file)))
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            String body = response.getBody();
            defaultOutputConsumer().accept(body);
            if (noWait) {
                return;
            }
            String taskUri = null;
            try {
                taskUri = new ObjectMapper().readTree(body).path("@odata.id").asText(null);
            } catch (Exception ignored) {
                // body is not JSON; no task to monitor
            }
            if (taskUri == null || taskUri.isEmpty()) {
                System.out.println("No task returned by the BMC; nothing to monitor.");
                return;
            }
            monitorTask(taskUri, timeoutSec);
        } catch (WebClientResponseException ex) {
            System.err.println("❌ Firmware update failed: " + ex.getStatusText());
            defaultOutputConsumer().accept(ex.getResponseBodyAsString());
            resetToken();
        } catch (Exception ex) {
            System.err.println("❌ Firmware update failed: " + ex.getMessage());
            resetToken();
        }
    }

    /**
     * Polls a Redfish Task until it reaches a terminal state or the timeout
     * expires. Tolerates BMC reboots (connection errors / expired token) by
     * re-authenticating and retrying.
     */
    private void monitorTask(String taskUri, int timeoutSec) throws URISyntaxException {
        final java.util.Set<String> terminal = java.util.Set.of("Completed", "Exception", "Killed", "Cancelled");
        var mapper = new ObjectMapper();
        var uri = new URI(base() + taskUri);
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        String lastLine = "";
        System.out.println("Monitoring " + taskUri + " (timeout " + timeoutSec + "s)...");
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Monitoring interrupted.");
                return;
            }
            try {
                String json = client.get()
                        .uri(uri)
                        .header("X-Auth-Token", token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                var task = mapper.readTree(json);
                String state = task.path("TaskState").asText("Unknown");
                String status = task.path("TaskStatus").asText("");
                String percent = task.has("PercentComplete") ? task.path("PercentComplete").asText() + "%" : "";
                String line = String.format("TaskState=%s %s %s", state, percent, status).trim();
                if (!line.equals(lastLine)) {
                    System.out.println(line);
                    lastLine = line;
                }
                if (terminal.contains(state)) {
                    task.path("Messages").forEach(m -> System.out.println("  " + m.path("Message").asText()));
                    if (state.equals("Completed")) {
                        System.out.println("✅ Firmware update completed. Verify with: get UpdateService/FirmwareInventory");
                    } else {
                        System.err.println("❌ Firmware update ended with state: " + state);
                    }
                    return;
                }
            } catch (Exception ex) {
                // BMC may be rebooting or the token may have expired
                System.out.println("Waiting for BMC (" + ex.getClass().getSimpleName() + ")...");
                try {
                    resetToken();
                } catch (Exception ignored) {
                    // retry on next iteration
                }
            }
        }
        System.err.println("⚠ Timed out after " + timeoutSec + "s waiting for task " + taskUri);
    }

    @ShellMethod(key = "install.opkg", value = "eg: install.opkg.install")
    void opkgInstall() throws InterruptedException {
        // Clean any failing prerm scripts first so opkg install/reinstall doesn't abort
        String command = "sudo rm -f /var/lib/opkg/info/*.prerm && sudo opkg install --force-reinstall --force-depends --force-overwrite /tmp/*.ipk && sudo rm -f /tmp/*.ipk";
        System.out.println(command);
        scmd(command);
    }

    @ShellMethod(key = "install.scp", value = "eg: install.scp filepath/filename. Will copy the file content to the /tmp/ folder in the remote machine")
    @ShellMethodAvailability("availabilityCheck")
    void scp(@ShellOption(value = { "--path",
            "-p" }, defaultValue = "/", valueProvider = FileCompleter.class) String path, String dest) {
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

    @ShellMethod(key = "check-bootside", value = "Check the current boot side")
    @ShellMethodAvailability("availabilityCheck")
    void checkBootside() {
        System.out.println("Checking current boot side...");
        scmd("fw_printenv bootside");
    }

    @ShellMethod(key = "set-bootside", value = "Check the current boot side")
    @ShellMethodAvailability("availabilityCheck")
    void setBootside(String bootside) {
        System.out.println("Setting current boot side...");
        scmd(String.format("fw_setenv bootside %s", bootside));
    }

    @ShellMethod(key = "update-firmware", value = "Update firmware automatically. eg: install.update-firmware [bootside]. Optional bootside parameter: 'a' or 'b' to specify target boot side for flashing")
    @ShellMethodAvailability("availabilityCheck")
    void updateFirmware(@ShellOption(defaultValue = ShellOption.NULL) String bootside) throws IOException {

        String imagePath = "/tmp/images";
        String label;

        // If bootside is provided as argument, use it directly
        if (bootside != null && !bootside.isEmpty()) {
            bootside = bootside.trim().toLowerCase();

            // Validate the provided bootside
            if (!bootside.equals("a") && !bootside.equals("b")) {
                System.err.println("❌ Error: Invalid boot side '" + bootside + "'. Must be 'a' or 'b'.");
                System.err.println("Usage: update-firmware [a|b]");
                return;
            }

            label = bootside;
            System.out.println("Using provided boot side for flashing: " + label);
        } else {
            // Auto-detect current boot side
            System.out.println("Checking current boot side...");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String currentBootSide = null;

            try {
                runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                        "fw_printenv bootside 2>&1");
                String bootSideOutput = outputStream.toString().trim();
                System.out.println("Boot side output: " + bootSideOutput);

                // Check if there was an error (lock file error or other issues)
                if (bootSideOutput.contains("Error") || bootSideOutput.contains("error") ||
                        !bootSideOutput.contains("=")) {
                    System.err.println("\nStandard Error:");
                    System.err.println(bootSideOutput);
                    currentBootSide = null;
                } else {
                    // Parse boot side (expected format: "bootside=a" or "bootside=b")
                    if (bootSideOutput.contains("=")) {
                        currentBootSide = bootSideOutput.split("=")[1].trim().toLowerCase();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to check boot side: " + e.getMessage());
                currentBootSide = null;
            }

            // If auto-detection failed, return with error
            if (currentBootSide == null) {
                System.err.println("\n❌ Error: Unable to automatically determine current boot side.");
                System.err.println("Please run the command with an explicit boot side parameter:");
                System.err.println("  update-firmware a  (to flash boot side 'a')");
                System.err.println("  update-firmware b  (to flash boot side 'b')");
                return;
            }

            // Determine target boot side (opposite of current)
            label = currentBootSide.equals("a") ? "b" : "a";
            System.out.println("Current boot side: " + currentBootSide);
            System.out.println("Target boot side for update: " + label);
        }

        // Get the first available version using the reusable method
        String version = getFirstAvailableVersion(imagePath);

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