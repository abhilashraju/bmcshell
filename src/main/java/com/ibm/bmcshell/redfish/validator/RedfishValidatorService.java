package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Core validation orchestrator that executes conformance checks across Redfish endpoints
 * with high-performance concurrent and parallel processing.
 */
@Service
public class RedfishValidatorService {

    @Autowired
    private RedfishSchemaManager schemaManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates a single Redfish URI endpoint.
     */
    public ResourceValidationResult validateEndpoint(WebClient webClient, String baseUrl, String uri, String authToken) {
        return validateEndpointWithNode(webClient, baseUrl, uri, authToken).result();
    }

    /**
     * Internal container holding both validation result and parsed payload node for crawling.
     */
    public record ValidationResultWithNode(ResourceValidationResult result, JsonNode payload) {}

    /**
     * Validates a single Redfish URI endpoint and returns the parsed JSON payload.
     */
    public ValidationResultWithNode validateEndpointWithNode(WebClient webClient, String baseUrl, String uri, String authToken) {
        ResourceValidationResult result = new ResourceValidationResult(uri);
        long start = System.currentTimeMillis();

        String fullUrl = baseUrl.endsWith("/") && uri.startsWith("/") ? baseUrl + uri.substring(1) : baseUrl + uri;
        JsonNode rootNode = null;

        try {
            var requestSpec = webClient.get().uri(fullUrl);
            if (authToken != null && !authToken.trim().isEmpty()) {
                requestSpec.header("X-Auth-Token", authToken);
            }

            ResponseEntity<String> response = requestSpec.retrieve()
                    .toEntity(String.class)
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            result.setExecutionTimeMs(elapsed);

            if (response == null) {
                result.setHttpStatusCode(0);
                result.addIssue(ValidationIssue.fail("HTTP_PROTOCOL", "", "Empty response entity received from server"));
                return new ValidationResultWithNode(result, null);
            }

            HttpStatusCode status = response.getStatusCode();
            result.setHttpStatusCode(status.value());
            HttpHeaders headers = response.getHeaders();

            // 1. Protocol and Header Validation
            result.addIssues(RedfishProtocolValidator.validateResponse(uri, status, headers));

            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                result.addIssue(ValidationIssue.fail("PAYLOAD", "", "Empty response body received"));
                return new ValidationResultWithNode(result, null);
            }

            try {
                rootNode = objectMapper.readTree(body);
            } catch (Exception e) {
                result.addIssue(ValidationIssue.fail("JSON_PARSE", "", "Failed to parse response body as JSON: " + e.getMessage()));
                return new ValidationResultWithNode(result, null);
            }

            // 2. OData Core Structure Validation
            result.addIssues(RedfishODataValidator.validateODataStructure(uri, rootNode));

            // Extract @odata.type
            String odataType = rootNode.has("@odata.type") ? rootNode.get("@odata.type").asText() : "";
            result.setResourceType(odataType);

            // 3. Schema Conformance Validation
            if (!odataType.isEmpty()) {
                Optional<JsonSchema> schemaOpt = schemaManager.getSchemaForOdataType(odataType, webClient);
                if (schemaOpt.isPresent()) {
                    result.addIssues(RedfishJsonSchemaValidator.validatePayload(rootNode, schemaOpt.get()));
                } else {
                    result.addIssue(ValidationIssue.warn("SCHEMA_LOOKUP", "@odata.type",
                            "No JSON Schema found for " + odataType + " (tried DMTF repository and local cache)"));
                }
            }

            // 4. Action Validation
            result.addIssues(RedfishActionValidator.validateActions(rootNode));

            // 5. Message Registry Validation
            result.addIssues(RedfishMessageRegistryValidator.validateMessageRegistries(rootNode, schemaManager, webClient));

            // 6. DateTime & Type Format Validation
            result.addIssues(RedfishDateTimeAndTypeValidator.validateDateTimesAndTypes(rootNode));

        } catch (WebClientResponseException e) {
            result.setExecutionTimeMs(System.currentTimeMillis() - start);
            result.setHttpStatusCode(e.getStatusCode().value());
            result.addIssue(ValidationIssue.fail("HTTP_ERROR", "", "HTTP Error " + e.getStatusCode().value() + ": " + e.getStatusText()));
        } catch (Exception e) {
            result.setExecutionTimeMs(System.currentTimeMillis() - start);
            result.addIssue(ValidationIssue.fail("COMMUNICATION_ERROR", "", "Connection or processing failure: " + e.getMessage()));
        }

        return new ValidationResultWithNode(result, rootNode);
    }

    /**
     * Validates multiple URIs in parallel using an ExecutorCompletionService for real-time
     * non-blocking result streaming as each worker finishes.
     */
    public ValidationReport validateUris(WebClient webClient, String baseUrl, Collection<String> uris, String authToken,
                                         int concurrency, Consumer<ResourceValidationResult> progressCallback) {
        ValidationReport report = new ValidationReport(baseUrl);
        long startTotal = System.currentTimeMillis();

        if (uris == null || uris.isEmpty()) {
            report.setTotalDurationMs(0);
            return report;
        }

        int threads = Math.max(1, Math.min(concurrency, 32));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CompletionService<ResourceValidationResult> completionService = new ExecutorCompletionService<>(executor);

        int totalSubmitted = 0;
        for (String uri : uris) {
            completionService.submit(() -> validateEndpoint(webClient, baseUrl, uri, authToken));
            totalSubmitted++;
        }

        for (int i = 0; i < totalSubmitted; i++) {
            try {
                Future<ResourceValidationResult> completedFuture = completionService.take();
                ResourceValidationResult res = completedFuture.get();
                report.addResult(res);
                if (progressCallback != null) {
                    progressCallback.accept(res);
                }
            } catch (Exception e) {
                ResourceValidationResult failRes = new ResourceValidationResult("unknown");
                failRes.addIssue(ValidationIssue.fail("EXECUTION_ERROR", "", "Parallel worker failed: " + e.getMessage()));
                report.addResult(failRes);
            }
        }

        executor.shutdown();
        report.setTotalDurationMs(System.currentTimeMillis() - startTotal);
        return report;
    }

    /**
     * Dynamically crawls and validates a Redfish resource tree in parallel starting from rootUri.
     */
    public ValidationReport validateTreeParallel(WebClient webClient, String baseUrl, String rootUri, String authToken,
                                                 int concurrency, Set<String> exclusions, Consumer<ResourceValidationResult> progressCallback) {
        ValidationReport report = new ValidationReport(baseUrl);
        long startTotal = System.currentTimeMillis();

        int threads = Math.max(1, Math.min(concurrency, 32));
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Set<String> visited = ConcurrentHashMap.newKeySet();
        AtomicInteger activeTasks = new AtomicInteger(0);
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        String startUri = (rootUri == null || rootUri.isEmpty()) ? "/redfish/v1" : rootUri;
        visited.add(startUri);
        queue.add(startUri);

        // Coordination latch to know when all discovery and validation tasks complete
        CountDownLatch doneLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        String uri = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (uri == null) {
                            if (activeTasks.get() == 0 && queue.isEmpty()) {
                                doneLatch.countDown();
                                break;
                            }
                            continue;
                        }

                        activeTasks.incrementAndGet();

                        ValidationResultWithNode resWithNode = validateEndpointWithNode(webClient, baseUrl, uri, authToken);
                        ResourceValidationResult res = resWithNode.result();
                        report.addResult(res);

                        if (progressCallback != null) {
                            progressCallback.accept(res);
                        }

                        // Discover and enqueue child URIs
                        if (resWithNode.payload() != null) {
                            Set<String> childUris = extractChildUris(resWithNode.payload());
                            for (String child : childUris) {
                                boolean excluded = exclusions != null && exclusions.stream().anyMatch(child::contains);
                                if (!excluded && visited.add(child)) {
                                    queue.add(child);
                                }
                            }
                        }

                        activeTasks.decrementAndGet();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        activeTasks.decrementAndGet();
                    }
                }
            });
        }

        try {
            doneLatch.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdownNow();
        report.setTotalDurationMs(System.currentTimeMillis() - startTotal);
        return report;
    }

    /**
     * Recursively extracts all Redfish child URIs (@odata.id references) from a JSON node.
     */
    public Set<String> extractChildUris(JsonNode node) {
        Set<String> uris = new HashSet<>();
        findOdataIds(node, uris);
        return uris;
    }

    private void findOdataIds(JsonNode node, Set<String> results) {
        if (node == null) return;
        if (node.isObject()) {
            if (node.has("@odata.id")) {
                String id = node.get("@odata.id").asText();
                if (id.startsWith("/redfish/v1") && !id.contains("$metadata") && !id.contains("#")) {
                    results.add(id);
                }
            }
            node.elements().forEachRemaining(child -> findOdataIds(child, results));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> findOdataIds(child, results));
        }
    }
}
