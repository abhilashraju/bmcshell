package com.ibm.bmcshell.redfish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RedfishUriCache - Caches and indexes Redfish URIs for fast pattern-based
 * searching.
 * Uses background thread to fetch and index URIs on first access.
 */
@Component
public class RedfishUriCache {

    private final Map<String, Set<String>> uriCache = new ConcurrentHashMap<>();
    private final Set<String> allUris = ConcurrentHashMap.newKeySet();
    private volatile boolean isIndexed = false;
    private volatile boolean isIndexing = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialize the cache by fetching all Redfish URIs in the background
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     */
    public CompletableFuture<Void> initializeCache(WebClient webClient, String baseUrl) {
        return initializeCache(webClient, baseUrl, null);
    }

    /**
     * Initialize the cache by fetching all Redfish URIs in the background with
     * authentication
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     * @param authToken Authentication token (X-Auth-Token header value), can be
     *                  null for unauthenticated access
     */
    public CompletableFuture<Void> initializeCache(WebClient webClient, String baseUrl, String authToken) {
        if (isIndexed || isIndexing) {
            return CompletableFuture.completedFuture(null);
        }

        isIndexing = true;

        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Starting Redfish URI indexing...");
                fetchAllUris(webClient, baseUrl, "/redfish/v1", authToken);
                System.out.println("Indexing complete. Found " + allUris.size() + " URIs.");
                isIndexed = true;

                // Automatically save cache after successful indexing
                try {
                    String cacheFile = getDefaultCacheFilePath();
                    saveToFile(cacheFile);
                    System.out.println("Cache automatically saved to: " + cacheFile);
                } catch (Exception saveError) {
                    System.err.println("Warning: Failed to auto-save cache: " + saveError.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error initializing Redfish URI cache: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isIndexing = false;
            }
        }, executorService);
    }

    /**
     * Recursively fetch all Redfish URIs starting from a root path
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     * @param path      Current URI path to fetch
     * @param authToken Authentication token (can be null)
     */
    private void fetchAllUris(WebClient webClient, String baseUrl, String path, String authToken) {
        fetchAllUrisRecursive(webClient, baseUrl, path, new HashSet<>(), authToken);
    }

    /**
     * Recursively fetch all Redfish URIs with cycle detection
     *
     * @param webClient   WebClient instance for HTTP requests
     * @param baseUrl     Base URL for the Redfish service
     * @param path        Current URI path to fetch
     * @param parentPaths Set of parent URIs in the current recursion path to detect
     *                    cycles
     * @param authToken   Authentication token (can be null)
     */
    private void fetchAllUrisRecursive(WebClient webClient, String baseUrl, String path, Set<String> parentPaths,
            String authToken) {
        // Check for circular reference - if this path appears in parent chain, stop
        // recursion
        if (parentPaths.contains(path)) {
            System.err.println("Circular reference detected: " + path + " already in parent chain");
            return;
        }

        // Add to global visited set (even if we've seen it before, we still record it)
        allUris.add(path);

        // Create new parent set for this recursion branch
        Set<String> currentParents = new HashSet<>(parentPaths);
        currentParents.add(path);

        try {
            // Build request with optional authentication
            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri(baseUrl + path);

            // Add authentication header if token is provided
            if (authToken != null && !authToken.isEmpty()) {
                request = request.header("X-Auth-Token", authToken);
            }

            String response = request
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);
                extractUrisFromNode(rootNode, webClient, baseUrl, currentParents, authToken);
            }
        } catch (Exception e) {
            // Continue indexing even if this URI fails - log error and move on
            System.err.println(
                    "Warning: Failed to fetch " + path + " - " + e.getMessage() + " (continuing with other URIs)");
        }
    }

    /**
     * Extract URIs from a JSON node recursively
     *
     * @param node        JSON node to extract URIs from
     * @param webClient   WebClient instance for HTTP requests
     * @param baseUrl     Base URL for the Redfish service
     * @param parentPaths Set of parent URIs in the current recursion path
     * @param authToken   Authentication token (can be null)
     */
    private void extractUrisFromNode(JsonNode node, WebClient webClient, String baseUrl, Set<String> parentPaths,
            String authToken) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();

                // Check for @odata.id fields
                if (entry.getKey().equals("@odata.id") && value.isTextual()) {
                    String uri = value.asText();
                    // Only check for circular reference in current path, not global visited
                    if (!parentPaths.contains(uri)) {
                        // Depth-first: recursively fetch immediately (synchronous)
                        fetchAllUrisRecursive(webClient, baseUrl, uri, parentPaths, authToken);
                    }
                }

                // Check for Members array
                if (entry.getKey().equals("Members") && value.isArray()) {
                    value.forEach(member -> {
                        if (member.has("@odata.id")) {
                            String uri = member.get("@odata.id").asText();
                            // Only check for circular reference in current path, not global visited
                            if (!parentPaths.contains(uri)) {
                                // Depth-first: recursively fetch immediately (synchronous)
                                fetchAllUrisRecursive(webClient, baseUrl, uri, parentPaths, authToken);
                            }
                        }
                    });
                }

                // Recursively process nested objects and arrays
                extractUrisFromNode(value, webClient, baseUrl, parentPaths, authToken);
            });
        } else if (node.isArray()) {
            node.forEach(element -> extractUrisFromNode(element, webClient, baseUrl, parentPaths, authToken));
        }
    }

    /**
     * Search for URIs matching a pattern
     * 
     * @param pattern Regex pattern to match against URIs
     * @return List of matching URIs
     */
    public List<String> searchByPattern(String pattern) {
        // Allow searching even during indexing to show partial results
        if (allUris.isEmpty()) {
            return Collections.emptyList();
        }

        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return allUris.stream()
                .filter(uri -> compiledPattern.matcher(uri).find())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Search for URIs containing a specific substring
     * 
     * @param substring Substring to search for
     * @return List of matching URIs
     */
    public List<String> searchBySubstring(String substring) {
        // Allow searching even during indexing to show partial results
        if (allUris.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerSubstring = substring.toLowerCase();
        return allUris.stream()
                .filter(uri -> uri.toLowerCase().contains(lowerSubstring))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all cached URIs
     * 
     * @return Set of all URIs
     */
    public Set<String> getAllUris() {
        return new HashSet<>(allUris);
    }

    /**
     * Check if cache is ready
     * 
     * @return true if cache is indexed and ready
     */
    public boolean isReady() {
        return isIndexed;
    }

    /**
     * Check if cache is currently indexing
     * 
     * @return true if indexing is in progress
     */
    public boolean isIndexing() {
        return isIndexing;
    }

    /**
     * Get cache statistics
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("isIndexed", isIndexed);
        stats.put("isIndexing", isIndexing);
        stats.put("totalUris", allUris.size());
        return stats;
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        allUris.clear();
        uriCache.clear();
        isIndexed = false;
    }

    /**
     * Save the URI cache to a file
     * 
     * @param filePath Path to save the cache file
     * @return true if save was successful, false otherwise
     */
    public boolean saveToFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            // Create parent directories if they don't exist
            Files.createDirectories(path.getParent());

            // Create a data structure to serialize
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("version", "1.0");
            cacheData.put("timestamp", System.currentTimeMillis());
            cacheData.put("uris", new ArrayList<>(allUris));
            cacheData.put("totalUris", allUris.size());

            // Write to file using ObjectMapper
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), cacheData);

            System.out.println("URI cache saved to: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save URI cache: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load the URI cache from a file
     * 
     * @param filePath Path to load the cache file from
     * @return true if load was successful, false otherwise
     */
    public boolean loadFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("Cache file not found: " + filePath);
                return false;
            }

            // Read from file using ObjectMapper
            @SuppressWarnings("unchecked")
            Map<String, Object> cacheData = objectMapper.readValue(path.toFile(), Map.class);

            // Extract URIs
            @SuppressWarnings("unchecked")
            List<String> uris = (List<String>) cacheData.get("uris");

            if (uris == null) {
                System.err.println("Invalid cache file format");
                return false;
            }

            // Clear existing cache and load new data
            allUris.clear();
            allUris.addAll(uris);
            isIndexed = true;

            System.out.println("URI cache loaded from: " + filePath);
            System.out.println("Loaded " + allUris.size() + " URIs");

            // Display cache metadata if available
            if (cacheData.containsKey("timestamp")) {
                long timestamp = ((Number) cacheData.get("timestamp")).longValue();
                System.out.println("Cache created: " + new Date(timestamp));
            }

            return true;
        } catch (IOException e) {
            System.err.println("Failed to load URI cache: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the default cache file path (machine-independent, common for all
     * machines)
     *
     * @return Default cache file path
     */
    public static String getDefaultCacheFilePath() {
        String userHome = System.getProperty("user.home");
        return userHome + "/.bmcshell/cache/redfish_uris_common.json";
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
