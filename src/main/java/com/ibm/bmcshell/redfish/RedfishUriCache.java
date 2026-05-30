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
    private volatile int totalUrisToProcess = 0;
    private volatile int processedUrisCount = 0;
    private volatile long indexingStartTime = 0;
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
        return initializeCache(webClient, baseUrl, authToken, false);
    }

    /**
     * Initialize the cache by fetching all Redfish URIs in the background with
     * authentication, with option for incremental indexing
     *
     * @param webClient   WebClient instance for HTTP requests
     * @param baseUrl     Base URL for the Redfish service
     * @param authToken   Authentication token (X-Auth-Token header value), can be
     *                    null for unauthenticated access
     * @param incremental If true, continue indexing from existing cached URIs
     *                    instead of starting from scratch
     */
    public CompletableFuture<Void> initializeCache(WebClient webClient, String baseUrl, String authToken,
            boolean incremental) {
        if (isIndexed || isIndexing) {
            return CompletableFuture.completedFuture(null);
        }

        isIndexing = true;

        return CompletableFuture.runAsync(() -> {
            try {
                int initialUriCount = allUris.size();
                indexingStartTime = System.currentTimeMillis();
                processedUrisCount = 0;

                if (incremental && !allUris.isEmpty()) {
                    System.out.println("Starting incremental indexing from " + initialUriCount + " cached URIs...");
                    // Create a sorted list of URIs in reverse order (deepest/newest first)
                    List<String> cachedUrisList = new ArrayList<>(allUris);
                    // Sort by URI depth (number of slashes) and then alphabetically, in reverse
                    cachedUrisList.sort((a, b) -> {
                        int depthA = a.length() - a.replace("/", "").length();
                        int depthB = b.length() - b.replace("/", "").length();
                        if (depthA != depthB) {
                            return Integer.compare(depthB, depthA); // Deeper URIs first
                        }
                        return b.compareTo(a); // Reverse alphabetical for same depth
                    });

                    totalUrisToProcess = cachedUrisList.size();

                    // Fetch immediate children only (not deep recursion) for each cached URI
                    // Process in reverse order (deepest/newest first)
                    for (String uri : cachedUrisList) {
                        processedUrisCount++;
                        try {
                            // Fetch only immediate children, not deep recursion
                            fetchImmediateChildren(webClient, baseUrl, uri, authToken);
                        } catch (Exception e) {
                            // Silently continue on errors
                        }
                    }
                } else {
                    System.out.println("Starting Redfish URI indexing from base URI...");
                    totalUrisToProcess = 1; // Starting from base URI
                    fetchAllUris(webClient, baseUrl, "/redfish/v1", authToken);
                }

                int newUrisFound = allUris.size() - initialUriCount;
                System.out.println("Indexing complete. Total URIs: " + allUris.size() +
                        (incremental ? " (+" + newUrisFound + " new)" : ""));
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
     * Fetch all Redfish URIs starting from a root path using breadth-first search
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     * @param path      Current URI path to fetch
     * @param authToken Authentication token (can be null)
     */
    private void fetchAllUris(WebClient webClient, String baseUrl, String path, String authToken) {
        fetchAllUrisBreadthFirst(webClient, baseUrl, path, authToken);
    }

    /**
     * Fetch only immediate children of a URI (non-recursive)
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     * @param path      URI path to fetch children from
     * @param authToken Authentication token (can be null)
     */
    private void fetchImmediateChildren(WebClient webClient, String baseUrl, String path, String authToken) {
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
                // Extract only immediate child URIs, don't recurse
                extractImmediateUris(rootNode);
            }
        } catch (Exception e) {
            // Continue indexing even if this URI fails
            System.err.println("Warning: Failed to fetch " + path + " - " + e.getMessage());
        }
    }

    /**
     * Extract immediate child URIs from a JSON node (non-recursive)
     *
     * @param node JSON node to extract URIs from
     */
    private void extractImmediateUris(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();

                // Check for @odata.id fields
                if (entry.getKey().equals("@odata.id") && value.isTextual()) {
                    String uri = value.asText();
                    allUris.add(uri);
                }

                // Check for Members array
                if (entry.getKey().equals("Members") && value.isArray()) {
                    value.forEach(member -> {
                        if (member.has("@odata.id")) {
                            String uri = member.get("@odata.id").asText();
                            allUris.add(uri);
                        }
                    });
                }
            });
        }
    }

    /**
     * Fetch all Redfish URIs using breadth-first search with cycle detection
     *
     * @param webClient WebClient instance for HTTP requests
     * @param baseUrl   Base URL for the Redfish service
     * @param startPath Starting URI path to fetch
     * @param authToken Authentication token (can be null)
     */
    private void fetchAllUrisBreadthFirst(WebClient webClient, String baseUrl, String startPath, String authToken) {
        // Queue for BFS traversal
        Queue<String> queue = new LinkedList<>();
        // Set to track visited URIs to avoid cycles
        Set<String> visited = new HashSet<>();

        // Initialize with starting path
        queue.offer(startPath);
        visited.add(startPath);
        allUris.add(startPath);

        // BFS traversal
        while (!queue.isEmpty()) {
            String currentPath = queue.poll();

            // Show progress for URIs discovered during indexing
            if (isIndexing && allUris.size() % 5 == 0) {
                System.out.println("  → Discovered " + allUris.size() + " URIs so far...");
            }

            try {
                // Build request with optional authentication
                WebClient.RequestHeadersSpec<?> request = webClient.get()
                        .uri(baseUrl + currentPath);

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
                    // Extract child URIs and add to queue
                    List<String> childUris = extractUrisFromNodeBFS(rootNode);

                    for (String uri : childUris) {
                        if (!visited.contains(uri)) {
                            visited.add(uri);
                            allUris.add(uri);
                            queue.offer(uri);
                        }
                    }
                }
            } catch (Exception e) {
                // Continue indexing even if this URI fails - log error and move on
                System.err.println(
                        "Warning: Failed to fetch " + currentPath + " - " + e.getMessage()
                                + " (continuing with other URIs)");
            }
        }
    }

    /**
     * Extract URIs from a JSON node for BFS traversal (non-recursive)
     *
     * @param node JSON node to extract URIs from
     * @return List of URIs found in the node
     */
    private List<String> extractUrisFromNodeBFS(JsonNode node) {
        List<String> uris = new ArrayList<>();
        extractUrisFromNodeBFSHelper(node, uris);
        return uris;
    }

    /**
     * Helper method to extract URIs from a JSON node (non-recursive traversal)
     *
     * @param node JSON node to extract URIs from
     * @param uris List to collect URIs
     */
    private void extractUrisFromNodeBFSHelper(JsonNode node, List<String> uris) {
        // Use a queue to traverse the JSON structure iteratively
        Queue<JsonNode> nodeQueue = new LinkedList<>();
        nodeQueue.offer(node);

        while (!nodeQueue.isEmpty()) {
            JsonNode currentNode = nodeQueue.poll();

            if (currentNode.isObject()) {
                currentNode.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();

                    // Check for @odata.id fields
                    if (entry.getKey().equals("@odata.id") && value.isTextual()) {
                        String uri = value.asText();
                        uris.add(uri);
                    }

                    // Check for Members array
                    if (entry.getKey().equals("Members") && value.isArray()) {
                        value.forEach(member -> {
                            if (member.has("@odata.id")) {
                                String uri = member.get("@odata.id").asText();
                                uris.add(uri);
                            }
                        });
                    }

                    // Add nested objects and arrays to queue for processing
                    if (value.isObject() || value.isArray()) {
                        nodeQueue.offer(value);
                    }
                });
            } else if (currentNode.isArray()) {
                currentNode.forEach(element -> {
                    if (element.isObject() || element.isArray()) {
                        nodeQueue.offer(element);
                    }
                });
            }
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
        stats.put("processedUris", processedUrisCount);
        stats.put("totalToProcess", totalUrisToProcess);

        if (isIndexing && indexingStartTime > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - indexingStartTime) / 1000;
            stats.put("elapsedSeconds", elapsedSeconds);

            if (processedUrisCount > 0 && totalUrisToProcess > 0) {
                double percentComplete = (processedUrisCount * 100.0) / totalUrisToProcess;
                stats.put("percentComplete", percentComplete);

                // Estimate time remaining
                if (percentComplete > 0) {
                    long estimatedTotalSeconds = (long) (elapsedSeconds / (percentComplete / 100.0));
                    long remainingSeconds = estimatedTotalSeconds - elapsedSeconds;
                    stats.put("estimatedRemainingSeconds", remainingSeconds);
                }
            }
        }

        return stats;
    }

    /**
     * Get detailed indexing progress information
     *
     * @return Formatted string with progress details
     */
    public String getIndexingProgress() {
        if (!isIndexing) {
            if (isIndexed) {
                return "Indexing complete. Total URIs: " + allUris.size();
            } else {
                return "Indexing not started. Run 'rf.index_uris' to begin.";
            }
        }

        StringBuilder progress = new StringBuilder();
        progress.append("Indexing in progress...\n");
        progress.append("─────────────────────────────\n");

        if (totalUrisToProcess > 0) {
            double percentComplete = (processedUrisCount * 100.0) / totalUrisToProcess;
            progress.append(String.format("Processed: %d/%d URIs (%.1f%%)\n",
                    processedUrisCount, totalUrisToProcess, percentComplete));
        }

        progress.append(String.format("Total URIs discovered: %d\n", allUris.size()));

        if (indexingStartTime > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - indexingStartTime) / 1000;
            progress.append(String.format("Elapsed time: %d seconds\n", elapsedSeconds));

            if (processedUrisCount > 0 && totalUrisToProcess > 0) {
                double percentComplete = (processedUrisCount * 100.0) / totalUrisToProcess;
                if (percentComplete > 0) {
                    long estimatedTotalSeconds = (long) (elapsedSeconds / (percentComplete / 100.0));
                    long remainingSeconds = estimatedTotalSeconds - elapsedSeconds;
                    progress.append(String.format("Estimated time remaining: %d seconds\n",
                            Math.max(0, remainingSeconds)));
                }
            }
        }

        return progress.toString();
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
     * Clear the indexed flag to allow re-indexing
     */
    public void clearIndexedFlag() {
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
