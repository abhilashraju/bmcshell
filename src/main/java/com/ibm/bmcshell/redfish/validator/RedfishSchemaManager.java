package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages loading, caching, downloading, and compiling DMTF and OEM Redfish JSON schemas
 * and Message Registries.
 */
@Component
public class RedfishSchemaManager {

    private static final String DEFAULT_SCHEMA_DIR = System.getProperty("user.home") + "/.bmcshell/schemas/";
    private static final String DMTF_SCHEMA_BASE_URL = "https://redfish.dmtf.org/schemas/v1/";
    private static final String DMTF_SCHEMA_INDEX_URL = "https://redfish.dmtf.org/redfish/schema_index";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    // Cache of compiled JsonSchema instances keyed by schema filename (e.g. "ComputerSystem.v1_15_0.json")
    private final Map<String, JsonSchema> compiledSchemaCache = new ConcurrentHashMap<>();

    // Cache of parsed Message Registries keyed by registry prefix (e.g. "Base", "ResourceEvent")
    private final Map<String, JsonNode> messageRegistries = new ConcurrentHashMap<>();

    private String schemaDirectory = DEFAULT_SCHEMA_DIR;

    private static final Pattern ODATA_TYPE_PATTERN = Pattern.compile("^#([A-Za-z0-9_]+)(?:\\.(v[0-9]+_[0-9]+_[0-9]+))?\\.([A-Za-z0-9_]+)$");

    public RedfishSchemaManager() {
        initSchemaDir();
    }

    private void initSchemaDir() {
        try {
            Path p = Paths.get(schemaDirectory);
            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            // fallback to local directory
            schemaDirectory = "./schemas/";
            try {
                Files.createDirectories(Paths.get(schemaDirectory));
            } catch (Exception ignored) {}
        }
    }

    public String getSchemaDirectory() {
        return schemaDirectory;
    }

    public void setSchemaDirectory(String schemaDirectory) {
        this.schemaDirectory = schemaDirectory.endsWith("/") ? schemaDirectory : schemaDirectory + "/";
        initSchemaDir();
        compiledSchemaCache.clear();
    }

    /**
     * Resolve and return a compiled JsonSchema for the given @odata.type string.
     * e.g. "#ComputerSystem.v1_15_0.ComputerSystem" -> resolves "ComputerSystem.v1_15_0.json" (or "ComputerSystem.json")
     */
    public Optional<JsonSchema> getSchemaForOdataType(String odataType, WebClient webClient) {
        if (odataType == null || odataType.trim().isEmpty()) {
            return Optional.empty();
        }

        List<String> candidateFileNames = getCandidateSchemaFileNames(odataType);
        for (String fileName : candidateFileNames) {
            JsonSchema schema = compiledSchemaCache.computeIfAbsent(fileName, fn -> loadOrFetchSchema(fn, webClient));
            if (schema != null) {
                return Optional.of(schema);
            }
        }

        return Optional.empty();
    }

    /**
     * Determines the prioritized candidate schema filenames for an @odata.type string.
     */
    public List<String> getCandidateSchemaFileNames(String odataType) {
        List<String> list = new ArrayList<>();
        Matcher m = ODATA_TYPE_PATTERN.matcher(odataType.trim());
        if (m.matches()) {
            String schemaName = m.group(1);
            String version = m.group(2); // e.g. "v1_15_0" or null
            String typeName = m.group(3);

            if (version != null && !version.isEmpty()) {
                list.add(schemaName + "." + version + ".json");
            }
            list.add(schemaName + ".json");
            if (!typeName.equals(schemaName)) {
                if (version != null && !version.isEmpty()) {
                    list.add(typeName + "." + version + ".json");
                }
                list.add(typeName + ".json");
            }
        } else {
            // If odataType does not match standard pattern, extract simple name
            String clean = odataType.replace("#", "").split("\\.")[0];
            if (!clean.isEmpty()) {
                list.add(clean + ".json");
            }
        }
        return list;
    }

    private JsonSchema loadOrFetchSchema(String schemaFileName, WebClient webClient) {
        Path localPath = Paths.get(schemaDirectory, schemaFileName);

        // 1. Try reading from local schema directory
        if (Files.exists(localPath)) {
            try {
                String content = Files.readString(localPath);
                return schemaFactory.getSchema(content);
            } catch (Exception e) {
                System.err.println("[RedfishSchemaManager] Error compiling local schema " + schemaFileName + ": " + e.getMessage());
            }
        }

        // 2. Try fetching from DMTF repository if webClient is provided
        if (webClient != null) {
            try {
                String url = DMTF_SCHEMA_BASE_URL + schemaFileName;
                String content = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (content != null && !content.isEmpty() && content.contains("$schema")) {
                    Files.createDirectories(localPath.getParent());
                    Files.writeString(localPath, content);
                    return schemaFactory.getSchema(content);
                }
            } catch (Exception ignored) {
                // Not found on DMTF or offline
            }
        }

        return null;
    }

    /**
     * Download and cache all DMTF JSON schemas and registries to local disk.
     */
    public int downloadAllDmtfSchemas(java.util.function.Consumer<String> progressLog) {
        try {
            if (progressLog != null) progressLog.accept("Fetching DMTF Schema Index from " + DMTF_SCHEMA_INDEX_URL + "...");
            Document doc = Jsoup.connect(DMTF_SCHEMA_INDEX_URL).timeout(15000).get();

            List<String> jsonLinks = new ArrayList<>();
            Elements rows = doc.select("table tbody tr");
            for (Element row : rows) {
                Elements links = row.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("href");
                    if (href.endsWith(".json")) {
                        jsonLinks.add(href);
                    }
                }
            }

            if (progressLog != null) progressLog.accept(String.format("Found %d JSON schemas. Starting download...", jsonLinks.size()));

            WebClient wc = WebClient.create();
            int downloaded = 0;

            for (String link : jsonLinks) {
                String fullUrl = link.startsWith("http") ? link : "https://redfish.dmtf.org" + link;
                String fileName = link.substring(link.lastIndexOf('/') + 1);
                Path destPath = Paths.get(schemaDirectory, fileName);

                if (Files.exists(destPath)) {
                    continue; // already cached
                }

                try {
                    String content = wc.get().uri(fullUrl).retrieve().bodyToMono(String.class).block();
                    if (content != null && !content.isEmpty()) {
                        Files.createDirectories(destPath.getParent());
                        Files.writeString(destPath, content);
                        downloaded++;
                        if (progressLog != null && downloaded % 20 == 0) {
                            progressLog.accept(String.format("Downloaded %d schemas...", downloaded));
                        }
                    }
                } catch (Exception e) {
                    // skip failing individual files
                }
            }

            if (progressLog != null) progressLog.accept(String.format("✓ Schema sync complete. %d new schemas downloaded to %s", downloaded, schemaDirectory));
            return downloaded;

        } catch (Exception e) {
            if (progressLog != null) progressLog.accept("✗ Failed to download DMTF schemas: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Counts how many local JSON schema files exist in cache.
     */
    public int getCachedSchemaCount() {
        try {
            Path p = Paths.get(schemaDirectory);
            if (!Files.exists(p)) return 0;
            try (var stream = Files.list(p)) {
                return (int) stream.filter(f -> f.toString().endsWith(".json")).count();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Resolves a message definition from a loaded or DMTF message registry.
     */
    public Optional<JsonNode> getMessageDefinition(String registryPrefix, String messageKey, WebClient webClient) {
        JsonNode registry = messageRegistries.computeIfAbsent(registryPrefix, prefix -> {
            // Try loading from local file e.g. Base.1.12.0.json or Base.json
            Path localPath = Paths.get(schemaDirectory, prefix + ".json");
            if (Files.exists(localPath)) {
                try {
                    return objectMapper.readTree(localPath.toFile());
                } catch (Exception ignored) {}
            }
            // Try fetching from DMTF
            if (webClient != null) {
                try {
                    String content = webClient.get()
                            .uri(DMTF_SCHEMA_BASE_URL + prefix + ".json")
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    if (content != null) {
                        JsonNode node = objectMapper.readTree(content);
                        Files.writeString(localPath, content);
                        return node;
                    }
                } catch (Exception ignored) {}
            }
            return null;
        });

        if (registry != null && registry.has("Messages") && registry.get("Messages").has(messageKey)) {
            return Optional.of(registry.get("Messages").get(messageKey));
        }

        return Optional.empty();
    }
}
