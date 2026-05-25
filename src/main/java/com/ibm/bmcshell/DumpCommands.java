package com.ibm.bmcshell;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ShellComponent
public class DumpCommands extends CommonCommands {
    protected DumpCommands() throws IOException {
    }

    @ShellMethod(key = "dump.bmc.create")
    @ShellMethodAvailability("availabilityCheck")
    public void bmcdump() throws URISyntaxException, IOException {

        post("/redfish/v1/Managers/bmc/LogServices/Dump/Actions/LogService.CollectDiagnosticData",
                "{\"DiagnosticDataType\" : \"Manager\"}", false);
    }

    @ShellMethod(key = "dump.system.create")
    @ShellMethodAvailability("availabilityCheck")
    public void systemdump() throws URISyntaxException, IOException {

        post("/redfish/v1/Systems/system/LogServices/Dump/Actions/LogService.CollectDiagnosticData",
                "{\"DiagnosticDataType\":\"OEM\", \"OEMDiagnosticDataType\":\"System\"}", false);
    }

    @ShellMethod(key = "dump.system.list")
    @ShellMethodAvailability("availabilityCheck")
    public void list_systemdump() throws URISyntaxException, IOException {

        get("/redfish/v1/Systems/system/LogServices/Dump/Entries", "", true);
    }

    @ShellMethod(key = "dump.bmc.list")
    @ShellMethodAvailability("availabilityCheck")
    public void list_bmcdump() throws URISyntaxException, IOException {
        get("/redfish/v1/Managers/bmc/LogServices/Dump/Entries", "", true);
    }

    @ShellMethod(key = "dump.bmc.delete")
    @ShellMethodAvailability("availabilityCheck")
    public void delete_bmcdump(String id) throws URISyntaxException, IOException {
        delete(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/", id));
    }

    public static class DownLoadInfo {
        public static enum Status {
            notStarted, inprogress, done
        }

        @JsonProperty
        public Status status;
        @JsonProperty
        public String key;
        @JsonProperty
        public String url;
        @JsonProperty
        public long size;
        @JsonProperty
        public String checksum;

        // Default constructor for Jackson
        public DownLoadInfo() {
        }

        public DownLoadInfo(String k, String u, Status b) {
            key = k;
            url = u;
            status = b;
            size = 0;
            checksum = "";
        }

        // Getters and setters for Jackson
        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }
    }

    public static class DownloadData {
        @JsonProperty
        public Map<String, DownLoadInfo> downLoadStatus = new HashMap<String, DownLoadInfo>();

        @JsonIgnore
        public AtomicBoolean collatestarted = new AtomicBoolean();

        @JsonProperty
        public String saveName;

        @JsonProperty
        public String metadataFile;

        // Default constructor for Jackson
        public DownloadData() {
            collatestarted = new AtomicBoolean(false);
        }

        public DownloadData(String s) {
            saveName = s;
            metadataFile = s + ".metadata.json";
            collatestarted = new AtomicBoolean(false);
        }

        // Getters and setters for Jackson
        public Map<String, DownLoadInfo> getDownLoadStatus() {
            return downLoadStatus;
        }

        public void setDownLoadStatus(Map<String, DownLoadInfo> status) {
            this.downLoadStatus = status;
        }

        public String getSaveName() {
            return saveName;
        }

        public void setSaveName(String name) {
            this.saveName = name;
        }

        public String getMetadataFile() {
            return metadataFile;
        }

        public void setMetadataFile(String file) {
            this.metadataFile = file;
        }

        // Save metadata to file for resume capability
        public void saveMetadata() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(metadataFile), this);
        }

        // Load metadata from file for resume
        public static DownloadData loadMetadata(String filename) throws IOException {
            String metadataPath = filename + ".metadata.json";
            File metaFile = new File(metadataPath);
            if (!metaFile.exists()) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            DownloadData data = mapper.readValue(metaFile, DownloadData.class);
            // Reinitialize transient fields
            if (data.collatestarted == null) {
                data.collatestarted = new AtomicBoolean(false);
            }
            return data;
        }
    }

    @ShellMethod(key = "dump.bmc.offload", value = "eg: bmc_dump_offload 4 out_filename [--resume]")
    @ShellMethodAvailability("availabilityCheck")
    public void bmc_dump_offload(String id, String filename, boolean resume)
            throws URISyntaxException, IOException, InterruptedException {
        if (filename == null) {
            filename = id;
        }

        // Try to resume from existing metadata
        DownloadData data = null;
        if (resume) {
            data = DownloadData.loadMetadata(filename);
            if (data != null) {
                System.out.println("Resuming download from metadata...");
                long completed = data.downLoadStatus.values().stream()
                        .filter(info -> info.status == DownLoadInfo.Status.done)
                        .count();
                System.out.println(String.format("Found %d/%d completed chunks",
                        completed, data.downLoadStatus.size()));
                downLoadParts(data, 3);
                return;
            } else {
                System.out.println("No metadata found, starting fresh download...");
            }
        }

        var target = String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/attachment", id);
        var auri = new URI(base() + target);
        var response = client.get()
                .uri(auri)
                .header("X-Auth-Token", token)
                .retrieve()
                .toEntity(String.class)
                .block();

        var header = response.getHeaders().get("Transfer-Encoding");
        if (header != null && header.contains("multipart")) {
            System.out.println("Detected chunked download mode");
            // Chunked download mode
            ObjectMapper mapper = new ObjectMapper();
            var tree = mapper.readTree(response.getBody());
            data = new DownloadData(filename);

            System.out.println("Detected chunked download mode");
            for (var node : tree.get("urls")) {
                data.downLoadStatus.put(node.get(0).asText(),
                        new DownLoadInfo(node.get(0).asText(),
                                node.get(1).asText(), DownLoadInfo.Status.notStarted));
            }

            // Save initial metadata
            data.saveMetadata();
            System.out.println(String.format("Starting download of %d chunks...",
                    data.downLoadStatus.size()));
            downLoadParts(data, 3);
            return;
        }

        // Non-chunked download (small file)
        System.out.println("Downloading complete file...");
        get(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/attachment", id), filename, false);
        String absPath = new File(filename).getAbsolutePath();
        Thread script = new Thread(() -> {
            try {
                System.out.println("Extracting dump to " + absPath + "_out");
                extract_dump(absPath);
            } catch (IOException | URISyntaxException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        script.setName("Dump Extractor");
        script.setDaemon(true);
        script.start();
    }

    @ShellMethod(key = "dump.extract", value = "eg: extract_dump out_filename")
    void extract_dump(String absPath) throws IOException, URISyntaxException, InterruptedException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("ebmcdumputil");
        if (in == null) {
            throw new FileNotFoundException("ebmcdumputil not found in resources");
        }
        File tempScript = File.createTempFile("ebmcdumputil", null);
        tempScript.setExecutable(true);
        try (FileOutputStream out = new FileOutputStream(tempScript)) {
            in.transferTo(out);
        }

        ProcessBuilder pb = new ProcessBuilder("bash", tempScript.getAbsolutePath(), "-e", absPath, "-I", "xz", "-L",
                "0");
        // pb.inheritIO();
        Process process = pb.start();
        process.waitFor();
        tempScript.delete();
        File outDir = new File(absPath + "_out");
        if (outDir.exists() && outDir.isDirectory()) {
            Files.walk(outDir.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(path -> System.out.println(path.toAbsolutePath()));
        } else {
            System.out.println("Directory " + outDir.getAbsolutePath() + " does not exist.");
        }
    }

    private void downLoadParts(DownloadData data, int max) {
        data.downLoadStatus.keySet().stream()
                .filter(a -> data.downLoadStatus.get(a).status == DownLoadInfo.Status.notStarted).limit(max)
                .forEach(a -> {
                    var info = data.downLoadStatus.get(a);
                    try {
                        info.status = DownLoadInfo.Status.inprogress;
                        asyncDownload(info.url, info.key, data);
                    } catch (URISyntaxException e) {
                        // throw new RuntimeException(e);
                    }
                });
    }

    void asyncDownload(String target, String filename, DownloadData data) throws URISyntaxException {
        var auri = new URI(base() + target);
        var info = data.downLoadStatus.get(filename);

        // Skip if already downloaded
        File chunkFile = new File(data.saveName + "/" + filename);
        if (chunkFile.exists() && info.status == DownLoadInfo.Status.done) {
            System.out.println("Chunk " + filename + " already downloaded, skipping...");
            try {
                updateAndcheckFinishedStatus(data, filename);
            } catch (Exception e) {
                System.err.println("Error updating status: " + e.getMessage());
            }
            return;
        }

        client.get()
                .uri(auri)
                .header("X-Auth-Token", token)
                .retrieve()
                .bodyToMono(byte[].class)
                .flatMap(bytes -> Mono.fromRunnable(() -> {
                    try {
                        File dir = new File(data.saveName);

                        // If saveName exists as a file, delete it first
                        if (dir.exists() && dir.isFile()) {
                            dir.delete();
                        }

                        // Create directory if it doesn't exist
                        if (!dir.exists()) {
                            if (!dir.mkdirs()) {
                                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
                            }
                        }

                        // Write chunk to file
                        Path chunkPath = Paths.get(data.saveName, filename);
                        java.nio.file.Files.write(chunkPath, bytes);

                        // Update metadata
                        info.size = bytes.length;
                        info.checksum = String.valueOf(bytes.hashCode()); // Simple checksum

                        System.out.println(String.format("Downloaded chunk %s (%d bytes)",
                                filename, bytes.length));

                        updateAndcheckFinishedStatus(data, filename);
                    } catch (Exception e) {
                        System.err.println("Error downloading chunk " + filename + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                })).onErrorResume(resp -> {
                    System.err.println("Network error for chunk " + filename + ", will retry...");
                    info.status = DownLoadInfo.Status.notStarted;
                    try {
                        data.saveMetadata();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    downLoadParts(data, 1);
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private void updateAndcheckFinishedStatus(DownloadData data, String filename) throws Exception {
        var info = data.downLoadStatus.get(filename);
        info.status = DownLoadInfo.Status.done;

        // Save metadata after each chunk completion
        data.saveMetadata();

        long completed = data.downLoadStatus.values().stream()
                .filter(a -> a.status == DownLoadInfo.Status.done)
                .count();
        long inProgress = data.downLoadStatus.values().stream()
                .filter(a -> a.status == DownLoadInfo.Status.inprogress)
                .count();
        long notStarted = data.downLoadStatus.values().stream()
                .filter(a -> a.status == DownLoadInfo.Status.notStarted)
                .count();
        long total = data.downLoadStatus.size();

        if (inProgress == 0 && notStarted == 0) {
            if (!data.collatestarted.getAndSet(true)) {
                System.out.println("\n✓ All chunks downloaded successfully!");
                System.out.println("Combining chunks into final file...");
                collateData(data);

                // Clean up metadata file after successful completion
                File metaFile = new File(data.metadataFile);
                if (metaFile.exists()) {
                    metaFile.delete();
                }
            }
            return;
        }

        // Clear screen and display progress table
        script.script(new File(CommonCommands.shellHomePath + "clear"));
        displayProgressTable(data, completed, inProgress, notStarted, total);

        downLoadParts(data, 1);
    }

    private void displayProgressTable(DownloadData data, long completed, long inProgress,
            long notStarted, long total) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              BMC DUMP CHUNKED DOWNLOAD PROGRESS                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println(String.format("║ Total Chunks: %-4d  Completed: %-4d  Progress: %5.1f%%          ║",
                total, completed, (completed * 100.0 / total)));
        System.out.println(String.format("║ In Progress:  %-4d  Not Started: %-4d                           ║",
                inProgress, notStarted));
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Chunk ID │ Status      │ Size (KB) │ Progress                      ║");
        System.out.println("╠══════════╪═════════════╪═══════════╪═══════════════════════════════╣");

        // Show last 10 completed chunks
        data.downLoadStatus.entrySet().stream()
                .filter(e -> e.getValue().status == DownLoadInfo.Status.done)
                .sorted((a, b) -> Integer.compare(
                        Integer.parseInt(b.getKey()),
                        Integer.parseInt(a.getKey())))
                .limit(10)
                .forEach(entry -> {
                    String chunkId = String.format("%-8s", entry.getKey());
                    String status = "✓ Done";
                    String size = String.format("%9.1f", entry.getValue().size / 1024.0);
                    String progress = "████████████████████ 100%";
                    System.out.println(String.format("║ %s │ %-11s │ %s │ %-29s ║",
                            chunkId, status, size, progress));
                });

        // Show all in-progress chunks
        data.downLoadStatus.entrySet().stream()
                .filter(e -> e.getValue().status == DownLoadInfo.Status.inprogress)
                .sorted((a, b) -> Integer.compare(
                        Integer.parseInt(a.getKey()),
                        Integer.parseInt(b.getKey())))
                .forEach(entry -> {
                    String chunkId = String.format("%-8s", entry.getKey());
                    String status = "⟳ Loading";
                    String size = String.format("%9s", "...");
                    String progress = "██████░░░░░░░░░░░░░░  30%";
                    System.out.println(String.format("║ %s │ %-11s │ %s │ %-29s ║",
                            chunkId, status, size, progress));
                });

        // Show next 5 not-started chunks
        data.downLoadStatus.entrySet().stream()
                .filter(e -> e.getValue().status == DownLoadInfo.Status.notStarted)
                .sorted((a, b) -> Integer.compare(
                        Integer.parseInt(a.getKey()),
                        Integer.parseInt(b.getKey())))
                .limit(5)
                .forEach(entry -> {
                    String chunkId = String.format("%-8s", entry.getKey());
                    String status = "○ Pending";
                    String size = String.format("%9s", "-");
                    String progress = "░░░░░░░░░░░░░░░░░░░░   0%";
                    System.out.println(String.format("║ %s │ %-11s │ %s │ %-29s ║",
                            chunkId, status, size, progress));
                });

        if (notStarted > 5) {
            System.out.println(String.format("║ ... and %d more pending chunks                                     ║",
                    notStarted - 5));
        }

        System.out.println("╚════════════════════════════════════════════════════════════════════╝");

        // Progress bar
        int barWidth = 60;
        int filledWidth = (int) ((completed * barWidth) / total);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barWidth; i++) {
            if (i < filledWidth) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        System.out.println(bar.toString());
        System.out.println();
    }

    private static void collateData(DownloadData data) {
        try (Stream<Path> paths = Files.list(Paths.get(data.saveName))) {
            File saveFile = new File(data.saveName + ".bk");
            System.out.println("Creating combined file: " + saveFile.getAbsolutePath());

            FileOutputStream outputStream = new FileOutputStream(saveFile);
            long totalBytes = 0;

            var sortedPaths = paths.map(a -> {
                var i = Integer.parseInt(a.getFileName().toString());
                return Pair.of(i, a);
            }).sorted((a1, a2) -> a1.getLeft() - a2.getLeft())
                    .collect(Collectors.toList());

            for (var f : sortedPaths) {
                try {
                    FileInputStream inputStream = new FileInputStream(f.getRight().toFile());
                    byte[] bytes = inputStream.readAllBytes();
                    outputStream.write(bytes);
                    totalBytes += bytes.length;
                    inputStream.close();
                    System.out.println(String.format("  Combined chunk %d (%d bytes)",
                            f.getLeft(), bytes.length));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            outputStream.close();

            System.out.println(String.format("\n✓ Successfully combined %d chunks (%d total bytes)",
                    sortedPaths.size(), totalBytes));
            System.out.println("Output file: " + saveFile.getAbsolutePath());

            // Clean up chunk directory
            File chunkDir = new File(data.saveName);
            if (chunkDir.exists() && chunkDir.isDirectory()) {
                Files.walk(chunkDir.toPath())
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + path);
                            }
                        });
                System.out.println("Cleaned up temporary chunk directory");
            }

        } catch (Exception e) {
            System.err.println("Error combining chunks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @ShellMethod(key = "dump.system.offload", value = "eg: system_dump_offload 4 out_filename")
    @ShellMethodAvailability("availabilityCheck")
    public void system_dump_offload(String id, String filename) throws URISyntaxException, IOException {

        // get(String.format("/redfish/v1/Systems/system/LogServices/Dump/Entries/%s/attachment",
        // id), filename, false);
        Thread script = new Thread(() -> {
            try {
                get(String.format("/redfish/v1/Systems/system/LogServices/Dump/Entries/%s/attachment", id), filename,
                        false);

            } catch (URISyntaxException | IOException e) {
            }

        });
        script.setName("Dump Extractor");
        script.setDaemon(true);
        script.start();
    }

}
