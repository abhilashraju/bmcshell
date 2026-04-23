package com.ibm.bmcshell.redfish;

import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.Utils.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RedfishUriCommands - Shell commands for searching and filtering Redfish URIs
 */
@ShellComponent
public class RedfishUriCommands extends CommonCommands {

    @Autowired
    private RedfishUriCache uriCache;

    protected RedfishUriCommands() throws IOException {
    }

    @ShellMethod(key = "rf.index_uris", value = "Index all Redfish URIs for fast searching (runs in background)")
    @ShellMethodAvailability("availabilityCheck")
    public String indexUris() throws Exception {
        if (uriCache.isReady()) {
            return "URI cache is already indexed with " + uriCache.getAllUris().size() + " URIs";
        }

        if (uriCache.isIndexing()) {
            return "URI indexing is already in progress...";
        }

        // Get authentication token
        String authToken = getToken();

        // Create WebClient with SSL configuration
        WebClient webClient = Util.createWebClient();
        String baseUrl = base();

        uriCache.initializeCache(webClient, baseUrl, authToken)
                .thenRun(() -> System.out.println("✓ Redfish URI indexing completed. Total URIs: " +
                        uriCache.getAllUris().size()));

        return "Started indexing Redfish URIs in background...";
    }

    @ShellMethod(key = "rf.search_uris", value = "Search Redfish URIs by pattern. eg: rf.search_uris --pattern 'Systems.*Processors'")
    @ShellMethodAvailability("availabilityCheck")
    public String searchUris(
            @ShellOption(value = { "--pattern", "-p" }) String pattern,
            @ShellOption(value = { "--limit", "-l" }, defaultValue = "50") int limit) {

        if (!uriCache.isReady() && !uriCache.isIndexing()) {
            return "URI cache not ready. Run 'rf.index_uris' first.";
        }

        List<String> results = uriCache.searchByPattern(pattern);

        if (results.isEmpty()) {
            String msg = "No URIs found matching pattern: " + pattern;
            if (uriCache.isIndexing()) {
                msg += "\n(Note: Indexing still in progress - results may be incomplete)";
            }
            return msg;
        }

        // Convert URIs to EndPoints for selection with 's' command
        List<Util.EndPoints> endpoints = new ArrayList<>();
        for (String uri : results) {
            endpoints.add(new Util.EndPoints(uri.replace("/redfish/v1/", ""), uri));
        }
        endPoints.push(endpoints);

        StringBuilder output = new StringBuilder();
        if (uriCache.isIndexing()) {
            output.append("⟳ Indexing in progress - showing partial results\n\n");
        }
        output.append(String.format("Found %d URIs matching '%s':\n\n", results.size(), pattern));

        int count = 0;
        for (String uri : results) {
            if (count >= limit) {
                output.append(String.format("\n... and %d more (use --limit to see more)\n",
                        results.size() - limit));
                break;
            }
            output.append(String.format("[%d] %s\n", count, uri));
            count++;
        }

        output.append("\nUse 's <number>' to select a URI\n");

        return output.toString();
    }

    @ShellMethod(key = "rf.find_uris", value = "Find Redfish URIs containing substring. eg: rf.find_uris --text 'Processor'")
    @ShellMethodAvailability("availabilityCheck")
    public String findUris(
            @ShellOption(value = { "--text", "-t" }) String substring,
            @ShellOption(value = { "--limit", "-l" }, defaultValue = "50") int limit) {

        if (!uriCache.isReady() && !uriCache.isIndexing()) {
            return "URI cache not ready. Run 'rf.index_uris' first.";
        }

        List<String> results = uriCache.searchBySubstring(substring);

        if (results.isEmpty()) {
            String msg = "No URIs found containing: " + substring;
            if (uriCache.isIndexing()) {
                msg += "\n(Note: Indexing still in progress - results may be incomplete)";
            }
            return msg;
        }

        // Convert URIs to EndPoints for selection with 's' command
        List<Util.EndPoints> endpoints = new ArrayList<>();
        for (String uri : results) {
            endpoints.add(new Util.EndPoints(uri.replace("/redfish/v1/", ""), uri));
        }
        endPoints.push(endpoints);

        StringBuilder output = new StringBuilder();
        if (uriCache.isIndexing()) {
            output.append("⟳ Indexing in progress - showing partial results\n\n");
        }
        output.append(String.format("Found %d URIs containing '%s':\n\n", results.size(), substring));

        int count = 0;
        for (String uri : results) {
            if (count >= limit) {
                output.append(String.format("\n... and %d more (use --limit to see more)\n",
                        results.size() - limit));
                break;
            }
            output.append(String.format("[%d] %s\n", count, uri));
            count++;
        }

        output.append("\nUse 's <number>' to select a URI\n");

        return output.toString();
    }

    @ShellMethod(key = "rf.list_all_uris", value = "List all cached Redfish URIs")
    @ShellMethodAvailability("availabilityCheck")
    public String listAllUris(
            @ShellOption(value = { "--limit", "-l" }, defaultValue = "100") int limit) {

        if (!uriCache.isReady() && !uriCache.isIndexing()) {
            return "URI cache not ready. Run 'rf.index_uris' first.";
        }

        List<String> allUris = uriCache.getAllUris().stream()
                .sorted()
                .toList();

        // Convert URIs to EndPoints for selection with 's' command
        List<Util.EndPoints> endpoints = new ArrayList<>();
        for (String uri : allUris) {
            endpoints.add(new Util.EndPoints(uri.replace("/redfish/v1/", ""), uri));
        }
        endPoints.push(endpoints);

        StringBuilder output = new StringBuilder();
        if (uriCache.isIndexing()) {
            output.append("⟳ Indexing in progress - showing partial results\n\n");
        }
        output.append(String.format("Total cached URIs: %d\n\n", allUris.size()));

        int count = 0;
        for (String uri : allUris) {
            if (count >= limit) {
                output.append(String.format("\n... and %d more (use --limit to see more)\n",
                        allUris.size() - limit));
                break;
            }
            output.append(String.format("[%d] %s\n", count, uri));
            count++;
        }

        output.append("\nUse 's <number>' to select a URI\n");

        return output.toString();
    }

    @ShellMethod(key = "rf.cache_stats", value = "Show Redfish URI cache statistics")
    @ShellMethodAvailability("availabilityCheck")
    public String cacheStats() {
        Map<String, Object> stats = uriCache.getStats();

        StringBuilder output = new StringBuilder();
        output.append("Redfish URI Cache Statistics:\n");
        output.append("─────────────────────────────\n");
        output.append(String.format("Status: %s\n",
                (Boolean) stats.get("isIndexed") ? "✓ Indexed"
                        : (Boolean) stats.get("isIndexing") ? "⟳ Indexing..." : "✗ Not Indexed"));
        output.append(String.format("Total URIs: %d\n", stats.get("totalUris")));

        if (!(Boolean) stats.get("isIndexed") && !(Boolean) stats.get("isIndexing")) {
            output.append("\nRun 'rf.index_uris' to start indexing.\n");
        }

        return output.toString();
    }

    @ShellMethod(key = "rf.clear_cache", value = "Clear the Redfish URI cache")
    @ShellMethodAvailability("availabilityCheck")
    public String clearCache() {
        int uriCount = uriCache.getAllUris().size();
        uriCache.clearCache();
        return String.format("Cleared cache containing %d URIs", uriCount);
    }

    @ShellMethod(key = "rf.get_by_pattern", value = "Get Redfish resource by pattern. eg: rf.get_by_pattern --pattern 'Systems.*Memory'")
    @ShellMethodAvailability("availabilityCheck")
    public void getByPattern(
            @ShellOption(value = { "--pattern", "-p" }) String pattern,
            @ShellOption(value = { "--index", "-i" }, defaultValue = "0") int index)
            throws URISyntaxException, IOException {

        if (!uriCache.isReady() && !uriCache.isIndexing()) {
            System.out.println("URI cache not ready. Run 'rf.index_uris' first.");
            return;
        }

        List<String> results = uriCache.searchByPattern(pattern);

        if (results.isEmpty()) {
            System.out.println("No URIs found matching pattern: " + pattern);
            return;
        }

        if (index >= results.size()) {
            System.out.println(String.format("Index %d out of range. Found %d URIs.", index, results.size()));
            return;
        }

        String uri = results.get(index);
        System.out.println("Fetching: " + uri + "\n");

        // Use the existing get method from CommonCommands
        get(uri.replace("/redfish/v1/", ""), "", false);
    }

    @ShellMethod(key = "rf.save_cache", value = "Save URI cache to file. eg: rf.save_cache --file /path/to/cache.json")
    @ShellMethodAvailability("availabilityCheck")
    public String saveCache(
            @ShellOption(value = { "--file", "-f" }, defaultValue = "") String filePath) {

        // Allow saving even during indexing (partial save)
        if (!uriCache.isReady() && !uriCache.isIndexing()) {
            return "URI cache not ready. Nothing to save.";
        }

        // Use default path if not specified
        if (filePath.isEmpty()) {
            filePath = RedfishUriCache.getDefaultCacheFilePath();
        }

        boolean success = uriCache.saveToFile(filePath);
        if (success) {
            String status = uriCache.isIndexing() ? " (partial - indexing in progress)" : "";
            return String.format("✓ Saved %d URIs to: %s%s", uriCache.getAllUris().size(), filePath, status);
        } else {
            return "✗ Failed to save cache";
        }
    }

    @ShellMethod(key = "rf.load_cache", value = "Load URI cache from file. eg: rf.load_cache --file /path/to/cache.json")
    @ShellMethodAvailability("availabilityCheck")
    public String loadCache(
            @ShellOption(value = { "--file", "-f" }, defaultValue = "") String filePath) {

        if (uriCache.isIndexing()) {
            return "Cannot load cache while indexing is in progress";
        }

        // Use default path if not specified
        if (filePath.isEmpty()) {
            filePath = RedfishUriCache.getDefaultCacheFilePath();
        }

        boolean success = uriCache.loadFromFile(filePath);
        if (success) {
            return String.format("✓ Loaded %d URIs from: %s", uriCache.getAllUris().size(), filePath);
        } else {
            return "✗ Failed to load cache from: " + filePath;
        }
    }
}
