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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
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

    @ShellMethod(key = { "install.firmware_update", "fu" }, value = "Push a firmware image via Redfish UpdateService and monitor the task. eg: fu --file ~/obmc-phosphor-image.tar [--no-wait] [--timeout 1800] [--manual [--bootside a|b]]")
    @ShellMethodAvailability("availabilityCheck")
    void firmwareUpdate(
            @ShellOption(value = { "--file", "-f" }, valueProvider = FileCompleter.class) String filePath,
            @ShellOption(value = "--no-wait", defaultValue = "false", help = "Do not monitor the update task") boolean noWait,
            @ShellOption(value = "--timeout", defaultValue = "1800", help = "Monitoring timeout in seconds") int timeoutSec,
            @ShellOption(value = "--manual", defaultValue = "false", help = "Copy via SFTP, untar on the BMC and flash with update-firmware (no Redfish)") boolean manual,
            @ShellOption(value = "--bootside", defaultValue = ShellOption.NULL, help = "Boot side to flash in --manual mode: a or b (default: opposite of current)") String bootside)
            throws URISyntaxException, IOException {
        File file = new File(filePath);
        if (!file.isFile()) {
            System.err.println("❌ Error: File not found: " + filePath);
            return;
        }
        if (manual) {
            manualUpdate(file, bootside);
            return;
        }
        var uri = new URI(base() + "/redfish/v1/UpdateService/update");
        ensureApplyTimeImmediate();
        final long total = file.length();
        System.out.println("Uploading " + file.getName() + " (" + formatBytes(total) + ") to " + uri + " ...");
        final java.util.concurrent.atomic.AtomicLong sent = new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicInteger lastPercent = new java.util.concurrent.atomic.AtomicInteger(-1);
        // Read the file in 64 KB chunks; backpressure keeps this close to bytes on the wire
        Flux<DataBuffer> fileData = DataBufferUtils
                .read(file.toPath(), new DefaultDataBufferFactory(), 64 * 1024)
                .doOnNext(buf -> {
                    long done = sent.addAndGet(buf.readableByteCount());
                    int percent = total == 0 ? 100 : (int) (done * 100 / total);
                    if (percent != lastPercent.getAndSet(percent)) {
                        System.out.print(String.format("\rUpload progress: %3d%% (%s / %s)", percent,
                                formatBytes(done), formatBytes(total)));
                        System.out.flush();
                    }
                });
        try {
            var response = Util.createUploadWebClient().post()
                    .uri(uri)
                    .header("X-Auth-Token", token)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(total)
                    .body(BodyInserters.fromDataBuffers(fileData))
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            System.out.println("\nUpload finished, waiting for BMC response.");
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
            System.err.println("❌ Firmware update failed: " + ex);
            resetToken();
        }
    }

    @ShellMethod(key = "install.fromFile", value = "Push a firmware tar that is already on the BMC via Redfish, using curl on the BMC with this shell's X-Auth-Token. eg: install.fromFile --path /tmp/foo.tar [--no-wait] [--timeout 1800]")
    @ShellMethodAvailability("availabilityCheck")
    void installFromFile(
            @ShellOption(value = { "--path", "-p" }, help = "Path of the image file on the BMC, e.g. /tmp/foo.tar") String bmcPath,
            @ShellOption(value = "--no-wait", defaultValue = "false", help = "Do not monitor the update task") boolean noWait,
            @ShellOption(value = "--timeout", defaultValue = "1800", help = "Monitoring timeout in seconds") int timeoutSec)
            throws URISyntaxException {
        // The path ends up in a remote shell command, so allow only safe characters
        if (!bmcPath.matches("/[A-Za-z0-9._/-]+")) {
            System.err.println("❌ Error: --path must be an absolute path using letters, digits, '.', '_', '-' and '/': "
                    + bmcPath);
            return;
        }
        if (token == null || token.isEmpty()) {
            getToken();
        }
        ensureApplyTimeImmediate();

        // curl runs on the BMC and talks to bmcweb over loopback; -T streams the file as the POST body
        String cmd = String.format(
                "test -f '%1$s' || { echo 'FILE_NOT_FOUND'; exit 1; }; "
                        + "curl -sk --max-time 600 -X POST -H 'X-Auth-Token: %2$s' "
                        + "-H 'Content-Type: application/octet-stream' -T '%1$s' "
                        + "-w '\\nHTTP_STATUS=%%{http_code}\\n' https://127.0.0.1:%3$d/redfish/v1/UpdateService/update",
                bmcPath, token, Util.targetport);
        System.out.println("Running curl on the BMC to push " + bmcPath + " ...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runCommandShort(out, Util.fullMachineName(machine), userName, passwd, cmd);
        String result = out.toString();

        if (result.contains("FILE_NOT_FOUND")) {
            System.err.println("❌ File not found on the BMC: " + bmcPath);
            return;
        }
        int idx = result.lastIndexOf("HTTP_STATUS=");
        if (idx < 0) {
            System.err.println("❌ No response from curl on the BMC. Output:\n" + result);
            return;
        }
        String body = result.substring(0, idx).trim();
        String status = result.substring(idx + "HTTP_STATUS=".length()).trim();
        defaultOutputConsumer().accept(body);
        if (!status.startsWith("2")) {
            System.err.println("❌ Firmware update failed: HTTP " + status);
            return;
        }
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
    }

    /**
     * Same pre-step the web UI performs: HTTP push uses the stored ApplyTime, so
     * make sure it is "Immediate" before uploading. Failures are non-fatal.
     */
    private void ensureApplyTimeImmediate() {
        try {
            String json = client.get()
                    .uri(new URI(base() + "/redfish/v1/UpdateService"))
                    .header("X-Auth-Token", token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            var applyTime = new ObjectMapper().readTree(json)
                    .path("HttpPushUriOptions").path("HttpPushUriApplyTime").path("ApplyTime");
            if (applyTime.isMissingNode() || "Immediate".equals(applyTime.asText())) {
                return;
            }
            System.out.println("ApplyTime is '" + applyTime.asText() + "', setting to 'Immediate'...");
            client.patch()
                    .uri(new URI(base() + "/redfish/v1/UpdateService"))
                    .header("X-Auth-Token", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"HttpPushUriOptions\":{\"HttpPushUriApplyTime\":{\"ApplyTime\":\"Immediate\"}}}")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            System.err.println("⚠ Could not set ApplyTime to Immediate (continuing): " + ex);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double v = bytes;
        String[] units = { "KB", "MB", "GB" };
        int i = -1;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, units[i]);
    }

    /**
     * Polls a Redfish Task until it reaches a terminal state or the timeout
     * expires. Connection errors (BMC rebooting) are retried silently without
     * re-authenticating; a new token is requested only on 401. A 404 means the
     * task is gone, which happens when the BMC reboots to apply the update.
     */
    private void monitorTask(String taskUri, int timeoutSec) throws URISyntaxException {
        final java.util.Set<String> terminal = java.util.Set.of("Completed", "Exception", "Killed", "Cancelled");
        final int maxReauth = 3;
        var mapper = new ObjectMapper();
        var uri = new URI(base() + taskUri);
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        String lastLine = "";
        boolean connectionLost = false;
        int reauthCount = 0;
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
                if (connectionLost) {
                    System.out.println("BMC is reachable again.");
                    connectionLost = false;
                }
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
            } catch (WebClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 401) {
                    if (++reauthCount > maxReauth) {
                        System.err.println("❌ Authentication keeps failing; giving up.");
                        return;
                    }
                    System.out.println("Session rejected (401), re-authenticating...");
                    resetToken();
                } else if (status == 404) {
                    System.out.println("Task " + taskUri + " no longer exists"
                            + (connectionLost || lastLine.contains("100%")
                                    ? " (the BMC most likely rebooted to apply the update)."
                                    : "."));
                    System.out.println("Verify the new version with: get UpdateService/FirmwareInventory");
                    return;
                } else {
                    System.err.println("❌ Task poll failed: HTTP " + status + " " + ex.getStatusText());
                    return;
                }
            } catch (Exception ex) {
                // Connection errors: the BMC is probably rebooting. Just retry; no new token needed.
                if (!connectionLost) {
                    System.out.println("Lost connection to BMC (" + ex.getClass().getSimpleName()
                            + "), waiting for it to come back...");
                    connectionLost = true;
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

    /** Prints a single-line upload progress with throughput and ETA. */
    private static class UploadProgress implements com.jcraft.jsch.SftpProgressMonitor {
        private long total;
        private long done;
        private long startNanos;
        private int lastPercent = -1;

        @Override
        public void init(int op, String src, String dest, long max) {
            total = max;
            startNanos = System.nanoTime();
        }

        @Override
        public boolean count(long count) {
            done += count;
            int percent = total <= 0 ? 100 : (int) (done * 100 / total);
            if (percent != lastPercent) {
                lastPercent = percent;
                double secs = Math.max((System.nanoTime() - startNanos) / 1e9, 0.001);
                double rate = done / secs;
                long eta = rate > 0 ? (long) ((total - done) / rate) : 0;
                System.out.print(String.format("\rUpload progress: %3d%% (%s / %s) %s/s ETA %ds   ", percent,
                        formatBytes(done), formatBytes(total), formatBytes((long) rate), eta));
                System.out.flush();
            }
            return true;
        }

        @Override
        public void end() {
            System.out.println();
        }
    }

    @ShellMethod(key = "install.scp", value = "eg: install.scp filepath/filename. Will copy the file to /tmp/ (same file name) in the remote machine")
    @ShellMethodAvailability("availabilityCheck")
    void scp(@ShellOption(value = { "--path",
            "-p" }, defaultValue = "/", valueProvider = FileCompleter.class) String path) {
        File localFile = new File(path);
        String remoteTmp = "/tmp/" + localFile.getName();
        if (!localFile.isFile()) {
            System.err.println("❌ Error: File not found: " + path);
            return;
        }
        System.out.println("Copying " + localFile.getName() + " (" + formatBytes(localFile.length()) + ") to "
                + remoteTmp + " ...");
        try {
            var session = SSHShellClient.getSession(Util.fullMachineName(machine), userName, passwd,
                    SSHShellClient.port);
            if (session == null) {
                System.err.println("❌ Failed to establish SSH session");
                return;
            }
            var sftp = (com.jcraft.jsch.ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            try {
                sftp.put(localFile.getAbsolutePath(), remoteTmp, new UploadProgress());
            } finally {
                sftp.disconnect();
            }
        } catch (Exception ex) {
            System.err.println("\n❌ Copy failed: " + ex);
        }
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

    /**
     * Copies the tar to the BMC with progress, extracts it into
     * /tmp/images/<id> the way the updater would (id = first 8 hex chars of the
     * SHA-512 of the MANIFEST version string), then flashes it.
     */
    private void manualUpdate(File file, String bootside) throws IOException {
        if (!file.getName().matches("[A-Za-z0-9._-]+")) {
            System.err.println("❌ Error: --manual needs a file name made of letters, digits, '.', '_' or '-': "
                    + file.getName());
            return;
        }
        if (bootside != null && !bootside.trim().matches("(?i)[ab]")) {
            System.err.println("❌ Error: Invalid boot side '" + bootside + "'. Must be 'a' or 'b'.");
            return;
        }
        scp(file.getAbsolutePath());

        String script = "W=/tmp/fw_extract; rm -rf $W; mkdir -p $W && tar -xf /tmp/" + file.getName()
                + " -C $W && V=$(sed -n \"s/^version=//p\" $W/MANIFEST) && [ -n \"$V\" ] && "
                + "ID=$(printf \"%s\" \"$V\" | sha512sum | cut -c1-8) && rm -rf /tmp/images/$ID && "
                + "mkdir -p /tmp/images/$ID && mv $W/* /tmp/images/$ID/ && echo FW_ID=$ID; rm -rf $W";
        String cmd = (userName.equals("root") ? "sh -c '" : "sudo sh -c '") + script + "'";
        System.out.println("Extracting image on the BMC...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runCommandShort(out, Util.fullMachineName(machine), userName, passwd, cmd);
        String id = null;
        for (String line : out.toString().split("\n")) {
            if (line.startsWith("FW_ID=")) {
                id = line.substring("FW_ID=".length()).trim();
            }
        }
        if (id == null || !id.matches("[0-9a-f]{8}")) {
            System.err.println("❌ Extraction failed. Output:\n" + out);
            return;
        }
        System.out.println("Extracted to /tmp/images/" + id);
        scmd("rm -f /tmp/" + file.getName());
        updateFirmware(bootside, id);
    }

    @ShellMethod(key = "update-firmware", value = "Update firmware automatically. eg: install.update-firmware [bootside]. Optional bootside parameter: 'a' or 'b' to specify target boot side for flashing")
    @ShellMethodAvailability("availabilityCheck")
    void updateFirmware(@ShellOption(defaultValue = ShellOption.NULL) String bootside) throws IOException {
        updateFirmware(bootside, null);
    }

    /** @param version image directory under /tmp/images; null = first one listed */
    private void updateFirmware(String bootside, String version) throws IOException {

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

        // Use the given version, or the first available one
        if (version == null) {
            version = getFirstAvailableVersion(imagePath);
        }

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
        scp(localScriptPath);

        System.out.println("Making script executable...");
        scmd("chmod +x " + remoteScriptPath);

        System.out.println("Executing firmware update script...");
        scmd(String.format("%s %s %s %s", remoteScriptPath, label, imagePath, version));

        System.out.println("Firmware update completed!");
        setBootside(label);
    }

}