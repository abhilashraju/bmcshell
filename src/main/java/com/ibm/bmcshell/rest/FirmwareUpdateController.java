package com.ibm.bmcshell.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import reactor.core.publisher.Mono;

/**
 * REST controller that acts as a firmware update server for the
 * firmware_updater OpenBMC daemon.
 *
 * <p>
 * The fw_updater daemon is configured with:
 * 
 * <pre>
 *   "serverHost": "&lt;bmcshell-host&gt;",
 *   "serverPort": "8443",
 *   "cataloguePath": "/firmware/catalogue.json"
 * </pre>
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET  /firmware/catalogue.json} — JSON array of available firmware
 * entries</li>
 * <li>{@code GET  /firmware/images/{filename}} — stream a firmware binary</li>
 * <li>{@code POST /firmware/images} — upload a new firmware image
 * (multipart)</li>
 * <li>{@code DELETE /firmware/images/{filename}} — remove an image from the
 * store</li>
 * </ul>
 *
 * <p>
 * The firmware store directory is configured via {@code firmware.store.dir}
 * in {@code application.properties}. Each image file placed there is
 * automatically included in the catalogue.
 *
 * <p>
 * Catalogue entry fields match the {@code FirmwareEntry} struct in
 * {@code fw_updater_config.hpp}:
 * 
 * <pre>
 *   version, imageUrl, checksum, releaseDate, description
 * </pre>
 * 
 * The version is derived from the filename: a file named
 * {@code firmware-2.15.0.bin} → version {@code "2.15.0"}.
 */
@RestController
@RequestMapping("/firmware")
public class FirmwareUpdateController {

    /** Filesystem directory where firmware binaries are stored. */
    @Value("${firmware.store.dir:./firmware-store}")
    private String storeDir;

    /** Base HTTPS URL of this bmcshell instance (used to build imageUrl). */
    @Value("${firmware.server.baseUrl:https://gfwr528.rchland.ibm.com:8443}")
    private String serverBaseUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Catalogue ─────────────────────────────────────────────────────────

    /**
     * Returns the firmware catalogue as a JSON array.
     *
     * <p>
     * Each object in the array has the fields expected by
     * {@code FirmwareEntry} in fw_updater_config.hpp:
     * {@code version}, {@code imageUrl}, {@code checksum},
     * {@code releaseDate}, {@code description}.
     *
     * @return JSON array of firmware entries, or an empty array if the
     *         store directory does not exist yet.
     */
    @GetMapping(value = "/catalogue.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> getCatalogue() {
        try {
            String json = buildCatalogueJson();
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}"));
        }
    }

    // ── Binary download ───────────────────────────────────────────────────

    /**
     * Streams a firmware binary to the caller.
     *
     * <p>
     * The fw_updater daemon fetches the {@code imageUrl} returned in
     * the catalogue entry here.
     *
     * @param filename Filename as returned in the catalogue {@code imageUrl}.
     */
    @GetMapping("/images/{filename:.+}")
    public Mono<ResponseEntity<Resource>> downloadImage(@PathVariable String filename) {
        Path filePath = resolveStorePath(filename);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        Resource resource = new FileSystemResource(filePath);
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource));
    }

    // ── Upload ────────────────────────────────────────────────────────────

    /**
     * Upload a firmware binary into the store.
     *
     * <p>
     * Optionally accepts {@code version}, {@code description}, and
     * {@code releaseDate} query parameters. When {@code version} is omitted
     * it is parsed from the filename (e.g. {@code firmware-2.15.0.bin} →
     * {@code "2.15.0"}).
     *
     * @param file        Multipart binary data.
     * @param version     Optional semantic version override.
     * @param description Optional human-readable description.
     * @param releaseDate Optional ISO-8601 release date (defaults to today).
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "releaseDate", required = false) String releaseDate)
            throws IOException {

        if (file.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("{\"error\":\"Empty file\"}"));
        }

        ensureStoreExists();

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body("{\"error\":\"Missing filename\"}"));
        }

        // toAbsolutePath() ensures transferTo() does not resolve the path
        // relative to Tomcat's internal working directory.
        Path dest = resolveStorePath(filename).toAbsolutePath();
        file.transferTo(dest);

        String resolvedVersion = (version != null && !version.isBlank())
                ? version
                : parseVersionFromFilename(filename);
        String resolvedDate = (releaseDate != null && !releaseDate.isBlank())
                ? releaseDate
                : DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 10);
        String resolvedDesc = (description != null && !description.isBlank())
                ? description
                : "Uploaded via bmcshell";

        // Write a per-image metadata sidecar so catalogue can show rich info.
        writeMetadata(filename, resolvedVersion, resolvedDate, resolvedDesc);

        String imageUrl = serverBaseUrl + "/firmware/images/" + filename;
        ObjectNode response = mapper.createObjectNode();
        response.put("status", "uploaded");
        response.put("filename", filename);
        response.put("version", resolvedVersion);
        response.put("imageUrl", imageUrl);

        return Mono.just(ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.toString()));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /**
     * Remove a firmware image (and its metadata sidecar) from the store.
     *
     * @param filename Filename of the image to remove.
     */
    @DeleteMapping("/images/{filename:.+}")
    public Mono<ResponseEntity<String>> deleteImage(@PathVariable String filename) throws IOException {
        Path filePath = resolveStorePath(filename);
        if (!Files.exists(filePath)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        Files.deleteIfExists(filePath);
        Files.deleteIfExists(resolveStorePath(filename + ".meta.json"));

        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"deleted\",\"filename\":\"" + filename + "\"}"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Path resolveStorePath(String filename) {
        // Prevent path traversal — only allow plain filenames.
        String safe = Paths.get(filename).getFileName().toString();
        return Paths.get(storeDir).resolve(safe);
    }

    private void ensureStoreExists() throws IOException {
        Files.createDirectories(Paths.get(storeDir));
    }

    /**
     * Build the JSON catalogue array by scanning the store directory.
     *
     * <p>
     * For every {@code .bin} (or non-{@code .meta.json}) file found a
     * catalogue entry is created. If a {@code <filename>.meta.json} sidecar
     * exists the metadata from it is used; otherwise fields are derived from
     * the filename.
     */
    private String buildCatalogueJson() throws IOException {
        ArrayNode catalogue = mapper.createArrayNode();
        Path store = Paths.get(storeDir);

        if (!Files.exists(store)) {
            return catalogue.toString();
        }

        List<Path> imageFiles = new ArrayList<>();
        try (var stream = Files.list(store)) {
            stream.filter(p -> !p.getFileName().toString().endsWith(".meta.json"))
                    .filter(p -> !Files.isDirectory(p))
                    .sorted()
                    .forEach(imageFiles::add);
        }

        for (Path imagePath : imageFiles) {
            String filename = imagePath.getFileName().toString();
            ObjectNode entry = mapper.createObjectNode();

            // Load metadata sidecar if present.
            Path metaPath = resolveStorePath(filename + ".meta.json");
            if (Files.exists(metaPath)) {
                ObjectNode meta = (ObjectNode) mapper.readTree(metaPath.toFile());
                entry.put("version", meta.path("version").asText(parseVersionFromFilename(filename)));
                entry.put("releaseDate", meta.path("releaseDate").asText(""));
                entry.put("description", meta.path("description").asText(""));
            } else {
                entry.put("version", parseVersionFromFilename(filename));
                entry.put("releaseDate", "");
                entry.put("description", "");
            }

            // Checksum (SHA-256) — compute on the fly if the file is small
            // enough; for large images skip to avoid blocking.
            entry.put("checksum", computeChecksum(imagePath));
            entry.put("imageUrl", serverBaseUrl + "/firmware/images/" + filename);

            catalogue.add(entry);
        }

        return catalogue.toPrettyString();
    }

    /**
     * Parse a semantic version from a filename.
     *
     * <p>
     * Examples:
     * <ul>
     * <li>{@code firmware-2.15.0.bin} → {@code "2.15.0"}</li>
     * <li>{@code obmc-2.14.bin} → {@code "2.14"}</li>
     * <li>{@code myimage.bin} → {@code "0.0.0"} (fallback)</li>
     * </ul>
     */
    static String parseVersionFromFilename(String filename) {
        // Extract the portion that matches \d+(\.\d+)+ from the filename.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)+)").matcher(filename);
        return m.find() ? m.group(1) : "0.0.0";
    }

    /** Write a JSON sidecar with metadata for the uploaded image. */
    private void writeMetadata(String filename, String version,
            String releaseDate, String description) throws IOException {
        ensureStoreExists();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("version", version);
        meta.put("releaseDate", releaseDate);
        meta.put("description", description);
        mapper.writeValue(resolveStorePath(filename + ".meta.json").toFile(), meta);
    }

    /**
     * Compute a SHA-256 hex digest for the file.
     * Returns an empty string on any error (non-fatal for catalogue serving).
     */
    private String computeChecksum(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
