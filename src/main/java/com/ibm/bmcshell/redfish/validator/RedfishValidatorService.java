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
import java.util.function.Consumer;

/**
 * Core validation orchestrator that executes conformance checks across Redfish endpoints.
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
        ResourceValidationResult result = new ResourceValidationResult(uri);
        long start = System.currentTimeMillis();

        String fullUrl = baseUrl.endsWith("/") && uri.startsWith("/") ? baseUrl + uri.substring(1) : baseUrl + uri;

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
                return result;
            }

            HttpStatusCode status = response.getStatusCode();
            result.setHttpStatusCode(status.value());
            HttpHeaders headers = response.getHeaders();

            // 1. Protocol and Header Validation
            result.addIssues(RedfishProtocolValidator.validateResponse(uri, status, headers));

            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                result.addIssue(ValidationIssue.fail("PAYLOAD", "", "Empty response body received"));
                return result;
            }

            JsonNode rootNode;
            try {
                rootNode = objectMapper.readTree(body);
            } catch (Exception e) {
                result.addIssue(ValidationIssue.fail("JSON_PARSE", "", "Failed to parse response body as JSON: " + e.getMessage()));
                return result;
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

        return result;
    }

    /**
     * Validates multiple URIs in batch or crawled tree.
     */
    public ValidationReport validateUris(WebClient webClient, String baseUrl, Collection<String> uris, String authToken,
                                         int concurrency, Consumer<ResourceValidationResult> progressCallback) {
        ValidationReport report = new ValidationReport(baseUrl);
        long startTotal = System.currentTimeMillis();

        if (uris == null || uris.isEmpty()) {
            report.setTotalDurationMs(0);
            return report;
        }

        int threads = Math.max(1, Math.min(concurrency, 10));
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<ResourceValidationResult>> futures = new ArrayList<>();
        for (String uri : uris) {
            futures.add(executor.submit(() -> validateEndpoint(webClient, baseUrl, uri, authToken)));
        }

        for (Future<ResourceValidationResult> future : futures) {
            try {
                ResourceValidationResult res = future.get(60, TimeUnit.SECONDS);
                report.addResult(res);
                if (progressCallback != null) {
                    progressCallback.accept(res);
                }
            } catch (Exception e) {
                ResourceValidationResult failRes = new ResourceValidationResult("unknown");
                failRes.addIssue(ValidationIssue.fail("TIMEOUT", "", "Validation task timed out or failed: " + e.getMessage()));
                report.addResult(failRes);
            }
        }

        executor.shutdown();
        report.setTotalDurationMs(System.currentTimeMillis() - startTotal);
        return report;
    }
}
