package com.ibm.bmcshell;

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
        delete(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/",id ));
    }

    public static class DownLoadInfo {
        static enum Status {
            notStarted, inprogress, done
        };

        Status status;
        String key;
        String url;

        DownLoadInfo(String k, String u, Status b) {
            key = k;
            url = u;
            status = b;
        }
    }

    public static class DownloadData {
        Map<String, DownLoadInfo> downLoadStatus = new HashMap<String, DownLoadInfo>();
        AtomicBoolean collatestarted = new AtomicBoolean();
        String saveName;

        DownloadData(String s) {
            saveName = s;
            collatestarted.set(false);
        }

    }

    @ShellMethod(key = "dump.bmc.offload", value = "eg: bmc_dump_offload 4 out_filename")
    @ShellMethodAvailability("availabilityCheck")
    public void bmc_dump_offload(String id, String filename) throws URISyntaxException, IOException, InterruptedException {
        if (filename == null) {
            filename = id;
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
        if (header != null) {
            ObjectMapper mapper = new ObjectMapper();
            var tree = mapper.readTree(response.getBody());
            DownloadData data = new DownloadData(filename);

            for (var node : tree.get("urls")) {
                // Process each item in the array
                data.downLoadStatus.put(node.get(0).asText(),
                        new DownLoadInfo(node.get(0).asText(),
                                node.get(1).asText(), DownLoadInfo.Status.notStarted));

            }
            downLoadParts(data, 3);
            return;
        }

        // System.out.println(response.getBody());
        get(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%s/attachment", id), filename, false);
        String absPath = new File(filename).getAbsolutePath();
        extract_dump(absPath);
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
        
        ProcessBuilder pb = new ProcessBuilder("bash", tempScript.getAbsolutePath(), "-e", absPath,"-I","xz","-L","3");
        pb.inheritIO();
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
        client.get()
                .uri(auri)
                .header("X-Auth-Token", token)
                .retrieve()
                .bodyToMono(byte[].class)
                .flatMap(bytes -> Mono.fromRunnable(() -> {
                    try {
                        File dir = new File(data.saveName);
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        java.nio.file.Files.write(Paths.get(data.saveName + "/" + filename), bytes);
                        updateAndcheckFinishedStatus(data, filename);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).onErrorResume(resp -> {
                    data.downLoadStatus.get(filename).status = DownLoadInfo.Status.notStarted;
                    downLoadParts(data, 1);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    private void updateAndcheckFinishedStatus(DownloadData data, String filename) throws Exception {
        var info = data.downLoadStatus.get(filename);
        info.status = DownLoadInfo.Status.done;
        if (data.downLoadStatus.values().stream()
                .filter(a -> (a.status == DownLoadInfo.Status.inprogress
                        || a.status == DownLoadInfo.Status.notStarted))
                .count() == 0) {
            if (!data.collatestarted.getAndSet(true)) {
                System.out.println("Finish Downloading all parts");
                collateData(data);
            }

            return;
        }
        script.script(new File(libPath + "clear"));
        System.out.println();
        data.downLoadStatus.forEach((a, b) -> {
            if (b.status != DownLoadInfo.Status.done) {
                System.out.print(a + " -> " + b.status + " ");
            }

        });
        downLoadParts(data, 1);
    }

    private static void collateData(DownloadData data) {
        try (Stream<Path> paths = Files.list(Paths.get(data.saveName))) {
            File saveFile = new File(data.saveName + ".bk");
            FileOutputStream outputStream = new FileOutputStream(saveFile);
            paths.map(a -> {
                var i = Integer.parseInt(a.getFileName().toString());
                return Pair.of(i, a);
            }).sorted((a1, a2) -> a1.getLeft() - a2.getLeft())
                    .forEach(f -> {
                        try {
                            FileInputStream inputStream = new FileInputStream(f.getRight().toFile());
                            outputStream.write(inputStream.readAllBytes());
                            inputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    });
            outputStream.close();

        } catch (Exception e) {
            File dir = new File(data.saveName);
            dir.delete();
            e.printStackTrace();
        }

    }

    @ShellMethod(key = "dump.system.offload", value = "eg: system_dump_offload 4 out_filename")
    @ShellMethodAvailability("availabilityCheck")
    public void system_dump_offload(String id, String filename) throws URISyntaxException, IOException {
        if (filename == null) {
            filename = id;
        }
        get(String.format("/redfish/v1/Systems/system/LogServices/Dump/Entries/%s/attachment", id), filename, false);
    }

}
