package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages loading, caching, downloading, and compiling DMTF and OEM Redfish JSON schemas
 * and Message Registries with cycle-safe and non-blocking reference resolution.
 */
@Component
public class RedfishSchemaManager {

    private static final String DEFAULT_SCHEMA_DIR = System.getProperty("user.home") + "/.bmcshell/schemas/";
    private static final String DMTF_SCHEMA_BASE_URL = "https://redfish.dmtf.org/schemas/v1/";
    private static final String DMTF_SCHEMA_INDEX_URL = "https://redfish.dmtf.org/redfish/schema_index";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory;
    private final SchemaValidatorsConfig validatorsConfig;

    // Cache of compiled JsonSchema instances keyed by schema filename (e.g. "ComputerSystem.v1_15_0.json")
    private final Map<String, JsonSchema> compiledSchemaCache = new ConcurrentHashMap<>();

    // Cache of parsed Message Registries keyed by registry prefix (e.g. "Base", "ResourceEvent")
    private final Map<String, JsonNode> messageRegistries = new ConcurrentHashMap<>();

    private String schemaDirectory = DEFAULT_SCHEMA_DIR;

    private static final Pattern ODATA_TYPE_PATTERN = Pattern.compile("^#([A-Za-z0-9_]+)(?:\\.(v[0-9]+_[0-9]+_[0-9]+))?\\.([A-Za-z0-9_]+)$");

    public RedfishSchemaManager() {
        initSchemaDir();

        // 1. Configure schema validator options
        this.validatorsConfig = new SchemaValidatorsConfig();
        this.validatorsConfig.setPreloadJsonSchema(false); // CRITICAL: prevents infinite recursive preload loops on cyclical Redfish $refs
        this.validatorsConfig.setHandleNullableField(true);

        // 2. Register custom Redfish meta-schema keywords to prevent UnknownKeyword warnings
        JsonMetaSchema v7 = JsonMetaSchema.getV7();
        JsonMetaSchema customMeta = JsonMetaSchema.builder(v7.getIri(), v7)
                .addKeyword(new NonValidationKeyword("urisDeprecated"))
                .addKeyword(new NonValidationKeyword("readonly"))
                .addKeyword(new NonValidationKeyword("longDescription"))
                .addKeyword(new NonValidationKeyword("versionAdded"))
                .addKeyword(new NonValidationKeyword("owningEntity"))
                .addKeyword(new NonValidationKeyword("degradedPermitted"))
                .addKeyword(new NonValidationKeyword("excerptCopy"))
                .addKeyword(new NonValidationKeyword("excerpt"))
                .addKeyword(new NonValidationKeyword("subProperties"))
                .addKeyword(new NonValidationKeyword("units"))
                .addKeyword(new NonValidationKeyword("parameterInfo"))
                .addKeyword(new NonValidationKeyword("deprecated"))
                .addKeyword(new NonValidationKeyword("degraded"))
                .build();

        // 3. Custom SchemaLoader to resolve remote $ref (http://redfish.dmtf.org/schemas/v1/...) from local cache
        SchemaLoader localDiskSchemaLoader = new SchemaLoader() {
            @Override
            public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                if (absoluteIri == null) return RedfishSchemaManager::emptyObjectStream;
                String uriStr = absoluteIri.toString();
                String fileName = uriStr.substring(uriStr.lastIndexOf('/') + 1);
                if (fileName.contains("#")) {
                    fileName = fileName.substring(0, fileName.indexOf('#'));
                }

                if (fileName.endsWith(".json")) {
                    Path localPath = Paths.get(schemaDirectory, fileName);
                    if (Files.exists(localPath)) {
                        return () -> new FileInputStream(localPath.toFile());
                    }

                    // Try fetching from DMTF if not present locally
                    try {
                        WebClient wc = WebClient.create();
                        String content = wc.get()
                                .uri(DMTF_SCHEMA_BASE_URL + fileName)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();

                        if (content != null && content.contains("$schema")) {
                            Files.createDirectories(localPath.getParent());
                            Files.writeString(localPath, content);
                            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                            return () -> new ByteArrayInputStream(bytes);
                        }
                    } catch (Exception ignored) {}
                }

                return RedfishSchemaManager::emptyObjectStream;
            }
        };

        this.schemaFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                .addMetaSchema(customMeta)
                .schemaLoaders(loaders -> loaders.add(localDiskSchemaLoader))
                .build();
    }

    private static InputStream emptyObjectStream() {
        return new ByteArrayInputStream("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}".getBytes(StandardCharsets.UTF_8));
    }

    public SchemaValidatorsConfig getValidatorsConfig() {
        return validatorsConfig;
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
                return schemaFactory.getSchema(content, validatorsConfig);
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
                    return schemaFactory.getSchema(content, validatorsConfig);
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
