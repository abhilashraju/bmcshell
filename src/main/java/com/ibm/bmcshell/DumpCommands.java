package com.ibm.bmcshell;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ShellComponent
public class DumpCommands extends CommonCommands {
    // Track active downloads
    private volatile DownloadData activeDownload = null;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

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

    @ShellMethod(key = "dump.bmc.info", value = "Get BMC dump entry info by ID. eg: dump.bmc.info 158")
    @ShellMethodAvailability("availabilityCheck")
    public void get_bmcdump_info(String id) throws URISyntaxException, IOException {
        get(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s", id), "", true);
    }

    @ShellMethod(key = "dump.system.info", value = "Get system dump entry info by ID. eg: dump.system.info 158")
    @ShellMethodAvailability("availabilityCheck")
    public void get_systemdump_info(String id) throws URISyntaxException, IOException {
        get(String.format("/redfish/v1/Systems/system/LogServices/Dump/Entries/%s", id), "", true);
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

        @JsonProperty
        public long startTimeMillis;

        @JsonProperty
        public int chunkSizeMB;

        @JsonProperty
        public int concurrency;

        // Default constructor for Jackson
        public DownloadData() {
            collatestarted = new AtomicBoolean(false);
            startTimeMillis = System.currentTimeMillis();
        }

        public DownloadData(String s, int chunkSize, int concur) {
            saveName = s;
            metadataFile = s + ".metadata.json";
            collatestarted = new AtomicBoolean(false);
            startTimeMillis = System.currentTimeMillis();
            chunkSizeMB = chunkSize;
            concurrency = concur;
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

        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        public void setStartTimeMillis(long time) {
            this.startTimeMillis = time;
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

    @ShellMethod(key = "dump.bmc.offload", value = "eg: bmc_dump_offload 4 --filename out_filename --chunkSizeMB 1 --resume --concurrency 20")
    @ShellMethodAvailability("availabilityCheck")
    public void bmc_dump_offload(
            String id,
            @org.springframework.shell.standard.ShellOption(defaultValue = "") String filename,
            @org.springframework.shell.standard.ShellOption(defaultValue = "1") int chunkSizeMB,
            @org.springframework.shell.standard.ShellOption(defaultValue = "false") boolean resume,
            @org.springframework.shell.standard.ShellOption(defaultValue = "20") int concurrency)
            throws URISyntaxException, IOException, InterruptedException {
        long offloadStartTime = System.currentTimeMillis();

        // Default filename to id if not specified
        if (filename == null || filename.isEmpty()) {
            filename = id;
        }

        // Reset stop flag for new download
        stopRequested.set(false);

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
                downLoadParts(data, concurrency);
                return;
            } else {
                System.out.println("No metadata found, starting fresh download...");
            }
        }

        // Fetch dump entry info to check AdditionalDataSizeBytes
        String infoJson = makeGetRequest(
                String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s", id), "");
        ObjectMapper infoMapper = new ObjectMapper();
        long additionalDataSizeBytes = 0;
        try {
            var infoTree = infoMapper.readTree(infoJson);
            if (infoTree.has("AdditionalDataSizeBytes")) {
                additionalDataSizeBytes = infoTree.get("AdditionalDataSizeBytes").asLong();
            }
        } catch (Exception e) {
            System.err.println("Warning: could not parse dump info, proceeding with chunked path: " + e.getMessage());
            additionalDataSizeBytes = Long.MAX_VALUE;
        }
        System.out.println(String.format("Dump AdditionalDataSizeBytes: %d (%.2f MB)",
                additionalDataSizeBytes, additionalDataSizeBytes / (1024.0 * 1024)));

        long chunkSizeBytes = (long) chunkSizeMB * 1024 * 1024;

        // Use single-file offload when chunkSize >= dumpSize (entire dump fits in one chunk)
        if (chunkSizeBytes >= additionalDataSizeBytes) {
            System.out.println("Chunk size >= dump size — using single-file offload path.");
            System.out.println("Downloading complete file...");
            long downloadStartTime = System.currentTimeMillis();
            get(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/attachment", id), filename, false);
            long downloadEndTime = System.currentTimeMillis();
            System.out.println(String.format("\n✓ Dump offload completed in %.2f seconds",
                    (downloadEndTime - downloadStartTime) / 1000.0));
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
            return;
        }

        // chunkSize < dumpSize — client-side chunked offload path.
        // Generate chunk URLs locally using AdditionalDataSizeBytes, mirroring
        // the server-side generateChunkUrls() logic in bmcweb log_services.hpp.
        System.out.println("Dump size is > chunk size — using chunked offload path.");
        data = new DownloadData(filename, chunkSizeMB, concurrency);

        long fullChunks = additionalDataSizeBytes / chunkSizeBytes;
        long remainder  = additionalDataSizeBytes % chunkSizeBytes;

        System.out.println(String.format(
                "Generating chunk URLs: fileSize=%d, chunkSizeMB=%d, fullChunks=%d, remainder=%d",
                additionalDataSizeBytes, chunkSizeMB, fullChunks, remainder));

        for (long i = 0; i < fullChunks; i++) {
            long offset = i * chunkSizeBytes;
            String url = String.format(
                    "/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/%d/%d/attachment/",
                    id, offset, chunkSizeBytes);
            String key = String.valueOf(i);
            data.downLoadStatus.put(key, new DownLoadInfo(key, url, DownLoadInfo.Status.notStarted));
        }

        if (remainder > 0) {
            long offset = fullChunks * chunkSizeBytes;
            String url = String.format(
                    "/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/%d/%d/attachment/",
                    id, offset, remainder);
            String key = String.valueOf(fullChunks);
            data.downLoadStatus.put(key, new DownLoadInfo(key, url, DownLoadInfo.Status.notStarted));
        }

        // Set as active download
        activeDownload = data;

        // Save initial metadata
        data.saveMetadata();
        System.out.println(String.format("Starting download of %d chunks with concurrency %d...",
                data.downLoadStatus.size(), concurrency));
        downLoadParts(data, concurrency);
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
        // Check if stop was requested
        if (stopRequested.get()) {
            System.out.println("\n⚠ Download stopped by user");
            return;
        }

        data.downLoadStatus.keySet().stream()
                .filter(a -> data.downLoadStatus.get(a).status == DownLoadInfo.Status.notStarted).limit(max)
                .forEach(a -> {
                    // Check stop flag before starting each download
                    if (stopRequested.get()) {
                        return;
                    }
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

                long offloadEndTime = System.currentTimeMillis();
                long totalOffloadDuration = offloadEndTime - data.startTimeMillis;

                // Calculate total bytes
                long totalBytes = data.downLoadStatus.values().stream()
                        .mapToLong(dlInfo -> dlInfo.size)
                        .sum();

                // Display summary table
                System.out.println("\n╔════════════════════════════════════════════════════════════════════╗");
                System.out.println("║              DUMP OFFLOAD COMPLETED SUCCESSFULLY                   ║");
                System.out.println("╠════════════════════════════════════════════════════════════════════╣");
                System.out.println(String.format("║ Total Chunks:      %-47d ║", data.downLoadStatus.size()));
                System.out.println(String.format("║ Chunk Size:        %-44s MB ║",
                        data.chunkSizeMB == 0 ? "Single chunk" : String.valueOf(data.chunkSizeMB)));
                System.out.println(String.format("║ Concurrency:       %-47d ║", data.concurrency));
                System.out.println(String.format("║ Total Size:        %-40s MB ║",
                        String.format("%.2f", totalBytes / (1024.0 * 1024.0))));
                System.out.println(String.format("║ Total Time:        %-40s sec ║",
                        String.format("%.2f", totalOffloadDuration / 1000.0)));
                System.out.println(String.format("║ Throughput:        %-40s MB/s ║",
                        String.format("%.2f", (totalBytes / (1024.0 * 1024.0)) / (totalOffloadDuration / 1000.0))));
                System.out.println("╚════════════════════════════════════════════════════════════════════╝");

                // Clean up metadata file after successful completion
                File metaFile = new File(data.metadataFile);
                if (metaFile.exists()) {
                    metaFile.delete();
                }

                // Clear active download
                activeDownload = null;
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

    // -------------------------------------------------------------------------
    // dump.timeout.test — verifies the per-chunk idle timer in bmcweb
    //
    // Three sub-tests run in sequence:
    //   1. NORMAL  — full-speed download must complete successfully
    //   2. SLOW    — rate-limited WebClient (1 byte/s effective) triggers
    //                server-side timeout mid-transfer
    //   3. STALLED — raw SSLSocket that stops reading after headers; server
    //                must hard-close the connection within ~15s
    // -------------------------------------------------------------------------
    @ShellMethod(key = "dump.timeout.test",
            value = "Test per-chunk idle timer. eg: dump.timeout.test 4 [--skipSanity]")
    @ShellMethodAvailability("availabilityCheck")
    public void dumpTimeoutTest(
            String id,
            @org.springframework.shell.standard.ShellOption(defaultValue = "false")
            boolean skipSanity) throws Exception {

        String attachmentPath = String.format(
                "/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/attachment", id);

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        bmcweb per-chunk idle timer — integration test      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        if (skipSanity) {
            System.out.println("  (--skipSanity: tests 1 and 3 skipped)");
        }

        // ── TEST 1: Normal full-speed download (optional) ───────────────────
        boolean test1Pass = true; // treated as N/A when skipped
        if (!skipSanity) {
            System.out.println("\n[TEST 1] Normal full-speed download...");
            test1Pass = false;
            try {
                Instant start = Instant.now();
                byte[] body = client.get()
                        .uri(new URI(base() + attachmentPath))
                        .header("X-Auth-Token", token)
                        .header("Accept", "application/octet-stream")
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block(Duration.ofMinutes(5));
                long elapsed = Duration.between(start, Instant.now()).toMillis();
                if (body != null && body.length > 0) {
                    System.out.printf("  ✓ Received %d bytes in %.1fs%n",
                            body.length, elapsed / 1000.0);
                    test1Pass = true;
                } else {
                    System.out.println("  ✗ Empty body received");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Failed: " + e.getMessage());
            }
            printResult("TEST 1 — Normal download", test1Pass);
        }

        // ── TEST 2: Read one chunk, sleep 30s, try to read again ─────────────
        // The server's chunk timer is 15s. If we stop reading for 30s the timer
        // must have fired and hard-closed the connection. The second read() will
        // then return -1 (EOF) or throw IOException (connection reset).
        System.out.println("\n[TEST 2] Stalled client: read one chunk, sleep 30s, verify server closed...");
        boolean test2Pass = false;
        try {
            URI baseUri = new URI(base());
            String host = baseUri.getHost();
            int port = baseUri.getPort() > 0 ? baseUri.getPort() : 443;

            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }}, null);

            SSLSocket sock = (SSLSocket) sslCtx.getSocketFactory()
                    .createSocket(host, port);
            sock.setSoTimeout(60_000);

            String req = "GET " + attachmentPath + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "X-Auth-Token: " + token + "\r\n"
                    + "Accept: application/octet-stream\r\n"
                    + "Connection: keep-alive\r\n\r\n";
            sock.getOutputStream().write(req.getBytes());
            sock.getOutputStream().flush();

            InputStream in = sock.getInputStream();

            // Read the response headers.
            int b, consecutive = 0, headerBytes = 0;
            while ((b = in.read()) != -1) {
                headerBytes++;
                if (b == '\r' || b == '\n') { consecutive++; } else { consecutive = 0; }
                if (consecutive == 4) break;
            }
            System.out.printf("  Read %d header bytes%n", headerBytes);

            // Read exactly one chunk of body data.
            byte[] chunk = new byte[65536];
            int chunkRead = in.read(chunk);
            System.out.printf("  Read first chunk: %d bytes%n", chunkRead);

            // Now stop reading for 30s — twice the server's 15s chunk timer.
            System.out.println("  Sleeping 30s (server timer is 15s)...");
            Thread.sleep(30_000);

            // Try to read again. The server must have closed the connection by now.
            // Either read() returns -1 (clean close) or throws IOException (RST).
            boolean serverClosedAfterSleep = false;
            try {
                sock.setSoTimeout(5_000); // 5s — if this succeeds the timer didn't fire
                int r = in.read(chunk);
                if (r < 0) {
                    System.out.println("  ✓ read() returned -1 (EOF) — server closed cleanly");
                    serverClosedAfterSleep = true;
                } else {
                    System.out.printf("  ✗ read() returned %d bytes — server still sending (timer did not fire)%n", r);
                }
            } catch (IOException e) {
                System.out.printf("  ✓ read() threw %s — server hard-closed (RST/SSL alert)%n",
                        e.getClass().getSimpleName());
                serverClosedAfterSleep = true;
            }

            sock.close();
            test2Pass = serverClosedAfterSleep;
        } catch (Exception e) {
            System.out.println("  ✗ Test setup failed: " + e.getMessage());
            e.printStackTrace();
        }
        printResult("TEST 2 — Stalled client hard-close", test2Pass);

        // ── TEST 3: Verify normal download still works after stall test ──────
        // Ensures the server is still healthy and serving new connections after
        // the hard-close. A broken server would fail this.
        boolean test3Pass = true; // treated as N/A when skipped
        if (!skipSanity) {
            System.out.println("\n[TEST 3] Server still healthy after stall (downloads again)...");
            test3Pass = false;
            try {
                byte[] body = client.get()
                        .uri(new URI(base() + attachmentPath))
                        .header("X-Auth-Token", token)
                        .header("Accept", "application/octet-stream")
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block(Duration.ofMinutes(5));
                if (body != null && body.length > 0) {
                    System.out.printf("  ✓ Received %d bytes — server healthy%n", body.length);
                    test3Pass = true;
                } else {
                    System.out.println("  ✗ Empty body — server may be degraded");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Failed: " + e.getMessage());
            }
            printResult("TEST 3 — Server health after stall", test3Pass);
        }

        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.printf( "║ TEST 1 (normal download)     : %-29s║%n",
                skipSanity ? "-- SKIPPED --" : (test1Pass ? "✓ PASS" : "✗ FAIL"));
        System.out.printf( "║ TEST 2 (stalled hard-close)  : %-29s║%n", test2Pass ? "✓ PASS" : "✗ FAIL");
        System.out.printf( "║ TEST 3 (server health)       : %-29s║%n",
                skipSanity ? "-- SKIPPED --" : (test3Pass ? "✓ PASS" : "✗ FAIL"));
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    private static void printResult(String name, boolean pass) {
        if (pass) {
            System.out.printf("  ╰─ %-40s ✓ PASS%n", name);
        } else {
            System.out.printf("  ╰─ %-40s ✗ FAIL%n", name);
        }
    }

    @ShellMethod(key = "dump.offload.stop", value = "Stop the current dump offload operation")
    @ShellMethodAvailability("availabilityCheck")
    public void stop_dump_offload() throws IOException {
        if (activeDownload == null) {
            System.out.println("No active dump offload to stop");
            return;
        }

        System.out.println("Stopping dump offload...");
        stopRequested.set(true);

        // Save current state to metadata for potential resume
        if (activeDownload != null) {
            try {
                activeDownload.saveMetadata();
                System.out.println("✓ Download state saved. You can resume with --resume flag");
                System.out.println("  Metadata file: " + activeDownload.metadataFile);

                long completed = activeDownload.downLoadStatus.values().stream()
                        .filter(info -> info.status == DownLoadInfo.Status.done)
                        .count();
                long total = activeDownload.downLoadStatus.size();
                System.out.println(String.format("  Progress: %d/%d chunks completed (%.1f%%)",
                        completed, total, (completed * 100.0 / total)));
            } catch (IOException e) {
                System.err.println("Warning: Failed to save metadata: " + e.getMessage());
            }
        }

        activeDownload = null;
    }

}
