package com.ibm.bmcshell.redfish;

import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.Utils.Util;
import com.ibm.bmcshell.redfish.validator.RedfishSchemaManager;
import com.ibm.bmcshell.redfish.validator.RedfishValidatorService;
import com.ibm.bmcshell.redfish.validator.ResourceValidationResult;
import com.ibm.bmcshell.redfish.validator.ValidationIssue;
import com.ibm.bmcshell.redfish.validator.ValidationLevel;
import com.ibm.bmcshell.redfish.validator.ValidationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Shell commands for DMTF Redfish Conformance Validation.
 */
@ShellComponent
public class RedfishValidatorCommands extends CommonCommands {

    @Autowired
    private RedfishValidatorService validatorService;

    @Autowired
    private RedfishSchemaManager schemaManager;

    @Autowired
    private RedfishUriCache uriCache;

    private ValidationReport lastReport;

    public RedfishValidatorCommands() throws IOException {
    }

    @ShellMethod(key = "rf.validate", value = "Validate a single Redfish URI against DMTF specifications and JSON schemas. eg: rf.validate --uri /redfish/v1/Systems/system")
    @ShellMethodAvailability("availabilityCheck")
    public String validate(
            @ShellOption(value = { "--uri", "-u" }, defaultValue = "/redfish/v1") String uri,
            @ShellOption(value = { "--html-report", "-r" }, defaultValue = "") String htmlReportPath) throws Exception {

        String baseUrl = base();
        String authToken = getToken();
        WebClient webClient = Util.createWebClient();

        System.out.println("Validating Redfish endpoint: " + uri + " on " + machine + " ...");

        ResourceValidationResult result = validatorService.validateEndpoint(webClient, baseUrl, uri, authToken);

        ValidationReport report = new ValidationReport(machine);
        report.addResult(result);
        report.setTotalDurationMs(result.getExecutionTimeMs());
        this.lastReport = report;

        StringBuilder output = new StringBuilder(report.toConsoleSummary());

        if (!htmlReportPath.isEmpty()) {
            String savedPath = report.saveHtmlReport(htmlReportPath);
            output.append("\n✓ HTML report saved to: ").append(savedPath).append("\n");
        }

        return output.toString();
    }

    @ShellMethod(key = "rf.validate_all", value = "Validate all discovered/cached Redfish URIs. eg: rf.validate_all --concurrency 4 --html-report ./report.html")
    @ShellMethodAvailability("availabilityCheck")
    public String validateAll(
            @ShellOption(value = { "--exclude", "-e" }, defaultValue = "") String excludePattern,
            @ShellOption(value = { "--concurrency", "-c" }, defaultValue = "4") int concurrency,
            @ShellOption(value = { "--html-report", "-r" }, defaultValue = "./redfish_validation_report.html") String htmlReportPath) throws Exception {

        // Check if URI cache has endpoints, otherwise index first
        if (!uriCache.isReady() && uriCache.getAllUris().isEmpty()) {
            System.out.println("URI cache empty. Performing quick indexing of Redfish tree first...");
            String baseUrl = base();
            String authToken = getToken();
            WebClient webClient = Util.createWebClient();
            uriCache.initializeCache(webClient, baseUrl, authToken, false).get();
        }

        Set<String> allUris = new TreeSet<>(uriCache.getAllUris());
        if (allUris.isEmpty()) {
            allUris.add("/redfish/v1");
        }

        // Apply exclusion filter if provided
        List<String> targetUris = allUris.stream()
                .filter(u -> excludePattern.isEmpty() || !u.matches(".*" + excludePattern + ".*"))
                .toList();

        System.out.println(String.format("Starting Redfish validation on %d endpoints (concurrency: %d)...", targetUris.size(), concurrency));

        String baseUrl = base();
        String authToken = getToken();
        WebClient webClient = Util.createWebClient();

        ValidationReport report = validatorService.validateUris(webClient, baseUrl, targetUris, authToken, concurrency,
                res -> {
                    String status = res.isPassed() ? "✓" : (res.isFailed() ? "✗" : "⚠");
                    System.out.println(String.format("[%s] %-50s (%d ms) [%d fail, %d warn]",
                            status, res.getUri(), res.getExecutionTimeMs(), res.getFailCount(), res.getWarnCount()));
                });

        this.lastReport = report;

        StringBuilder output = new StringBuilder(report.toConsoleSummary());

        if (!htmlReportPath.isEmpty()) {
            String savedPath = report.saveHtmlReport(htmlReportPath);
            output.append("\n✓ Full HTML report saved to: ").append(savedPath).append("\n");
        }

        return output.toString();
    }

    @ShellMethod(key = "rf.download_schemas", value = "Download and sync all DMTF standard JSON Schemas to local cache")
    @ShellMethodAvailability("availabilityCheck")
    public String downloadSchemas(
            @ShellOption(value = { "--dir", "-d" }, defaultValue = "") String directory,
            @ShellOption(value = { "--force", "-f" }, defaultValue = "false") boolean force) {

        if (!directory.isEmpty()) {
            schemaManager.setSchemaDirectory(directory);
        }

        System.out.println("Syncing DMTF Redfish JSON Schemas to local storage: " + schemaManager.getSchemaDirectory());

        int downloaded = schemaManager.downloadAllDmtfSchemas(System.out::println);
        return String.format("✓ Download completed. Total cached schemas in %s: %d",
                schemaManager.getSchemaDirectory(), schemaManager.getCachedSchemaCount());
    }

    @ShellMethod(key = "rf.validator_status", value = "Show Redfish validator configuration and schema cache status")
    @ShellMethodAvailability("availabilityCheck")
    public String validatorStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Redfish Conformance Validator Status:\n");
        sb.append("─────────────────────────────────────\n");
        sb.append(String.format("Target BMC          : %s\n", machine));
        sb.append(String.format("Schema Cache Dir    : %s\n", schemaManager.getSchemaDirectory()));
        sb.append(String.format("Cached JSON Schemas : %d\n", schemaManager.getCachedSchemaCount()));

        if (lastReport != null) {
            sb.append("\nLast Validation Run Summary:\n");
            sb.append(String.format("  Total URIs Tested : %d\n", lastReport.getTotalCount()));
            sb.append(String.format("  Passed            : %d\n", lastReport.getPassedCount()));
            sb.append(String.format("  Failed            : %d\n", lastReport.getFailedCount()));
            sb.append(String.format("  Warnings          : %d\n", lastReport.getWarningCount()));
        } else {
            sb.append("\nNo validation runs executed yet in this session.\n");
            sb.append("Run 'rf.validate' or 'rf.validate_all' to begin.\n");
        }

        return sb.toString();
    }
}
