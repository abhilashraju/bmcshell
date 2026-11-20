package com.ibm.bmcshell.redfish.validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates validation results across all tested URIs and produces reports.
 */
public class ValidationReport {

    private final String targetHost;
    private final String startTime;
    private long totalDurationMs;
    private final List<ResourceValidationResult> results = new ArrayList<>();

    public ValidationReport(String targetHost) {
        this.targetHost = targetHost;
        this.startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void addResult(ResourceValidationResult result) {
        if (result != null) {
            results.add(result);
        }
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public List<ResourceValidationResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public int getTotalCount() {
        return results.size();
    }

    public long getPassedCount() {
        return results.stream().filter(ResourceValidationResult::isPassed).count();
    }

    public long getFailedCount() {
        return results.stream().filter(ResourceValidationResult::isFailed).count();
    }

    public long getWarningCount() {
        return results.stream().filter(ResourceValidationResult::hasWarnings).count();
    }

    public long getSkippedCount() {
        return results.stream().filter(ResourceValidationResult::isSkipped).count();
    }

    public long getTotalFailures() {
        return results.stream().mapToLong(ResourceValidationResult::getFailCount).sum();
    }

    public long getTotalWarnings() {
        return results.stream().mapToLong(ResourceValidationResult::getWarnCount).sum();
    }

    /**
     * Formats terminal summary with colors.
     */
    public String toConsoleSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================================================\n");
        sb.append("                  REDFISH SERVICE CONFORMANCE VALIDATION REPORT                 \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Target BMC : %s\n", targetHost));
        sb.append(String.format("Timestamp  : %s\n", startTime));
        sb.append(String.format("Duration   : %.2f seconds\n", totalDurationMs / 1000.0));
        sb.append("────────────────────────────────────────────────────────────────────────────────\n");
        sb.append(String.format("Total Endpoints Validated : %d\n", getTotalCount()));
        sb.append(String.format("✓ PASSED Resources        : %d\n", getPassedCount()));
        sb.append(String.format("✗ FAILED Resources        : %d (Total Failures: %d)\n", getFailedCount(), getTotalFailures()));
        sb.append(String.format("⚠ WARNING Resources       : %d (Total Warnings: %d)\n", getWarningCount(), getTotalWarnings()));
        sb.append(String.format("⊘ SKIPPED Resources       : %d\n", getSkippedCount()));
        sb.append("================================================================================\n\n");

        if (getFailedCount() > 0 || getWarningCount() > 0) {
            sb.append("DETAILED ISSUES BY RESOURCE:\n");
            sb.append("────────────────────────────────────────────────────────────────────────────────\n");

            for (ResourceValidationResult res : results) {
                if (res.isPassed() && res.getIssues().isEmpty()) continue;

                String statusSymbol = res.isFailed() ? "✗ [FAIL]" : (res.hasWarnings() ? "⚠ [WARN]" : "✓ [PASS]");
                sb.append(String.format("%s %s (%s) [%dms]\n",
                        statusSymbol, res.getUri(), res.getResourceType(), res.getExecutionTimeMs()));

                for (ValidationIssue issue : res.getIssues()) {
                    String levelTag = issue.level() == ValidationLevel.FAIL ? "  [FAIL]" :
                            (issue.level() == ValidationLevel.WARN ? "  [WARN]" : "  [SKIP]");
                    sb.append(String.format("  %s %-18s : %s", levelTag, "[" + issue.ruleCategory() + "]", issue.message()));
                    if (issue.propertyPath() != null && !issue.propertyPath().isEmpty()) {
                        sb.append(" (at ").append(issue.propertyPath()).append(")");
                    }
                    if (issue.expected() != null || issue.actual() != null) {
                        sb.append(String.format(" [Expected: %s, Actual: %s]", issue.expected(), issue.actual()));
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        } else if (getTotalCount() > 0) {
            sb.append("✓ All endpoints fully conform to Redfish specifications!\n\n");
        }

        return sb.toString();
    }

    /**
     * Generates a clean standalone HTML report.
     */
    public String saveHtmlReport(String outputPath) throws IOException {
        if (outputPath == null || outputPath.isEmpty()) {
            outputPath = "./redfish_validator_report.html";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n");
        html.append("<title>Redfish Conformance Validation Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 30px; background: #f8fafc; color: #1e293b; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }\n");
        html.append("h1 { margin-top: 0; color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 12px; }\n");
        html.append(".summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin: 24px 0; }\n");
        html.append(".card { padding: 16px; border-radius: 6px; text-align: center; }\n");
        html.append(".card-total { background: #f1f5f9; border: 1px solid #cbd5e1; }\n");
        html.append(".card-pass { background: #dcfce7; border: 1px solid #86efac; color: #166534; }\n");
        html.append(".card-fail { background: #fee2e2; border: 1px solid #fca5a5; color: #991b1b; }\n");
        html.append(".card-warn { background: #fef9c3; border: 1px solid #fde047; color: #854d0e; }\n");
        html.append(".card-number { font-size: 28px; font-weight: bold; margin-top: 4px; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 14px; }\n");
        html.append("th, td { padding: 10px 14px; border: 1px solid #e2e8f0; text-align: left; }\n");
        html.append("th { background: #f8fafc; color: #475569; }\n");
        html.append(".badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-weight: bold; font-size: 12px; }\n");
        html.append(".badge-pass { background: #22c55e; color: white; }\n");
        html.append(".badge-fail { background: #ef4444; color: white; }\n");
        html.append(".badge-warn { background: #eab308; color: white; }\n");
        html.append(".badge-skip { background: #94a3b8; color: white; }\n");
        html.append(".issues-list { margin: 8px 0; padding-left: 20px; font-size: 13px; color: #334155; }\n");
        html.append("</style>\n</head>\n<body>\n<div class=\"container\">\n");
        html.append("<h1>Redfish Service Conformance Report</h1>\n");
        html.append("<p><strong>Target Host:</strong> ").append(targetHost).append(" &nbsp;|&nbsp; <strong>Date:</strong> ").append(startTime).append(" &nbsp;|&nbsp; <strong>Duration:</strong> ").append(String.format("%.2f s", totalDurationMs / 1000.0)).append("</p>\n");

        html.append("<div class=\"summary-cards\">\n");
        html.append("<div class=\"card card-total\"><div>Total Endpoints</div><div class=\"card-number\">").append(getTotalCount()).append("</div></div>\n");
        html.append("<div class=\"card card-pass\"><div>Passed</div><div class=\"card-number\">").append(getPassedCount()).append("</div></div>\n");
        html.append("<div class=\"card card-fail\"><div>Failed</div><div class=\"card-number\">").append(getFailedCount()).append("</div></div>\n");
        html.append("<div class=\"card card-warn\"><div>Warnings</div><div class=\"card-number\">").append(getWarningCount()).append("</div></div>\n");
        html.append("</div>\n");

        html.append("<h2>Validation Results</h2>\n");
        html.append("<table>\n<thead><tr><th>Status</th><th>URI</th><th>Type</th><th>HTTP</th><th>Time</th><th>Issues</th></tr></thead>\n<tbody>\n");

        for (ResourceValidationResult r : results) {
            String badgeClass = r.isPassed() ? "badge-pass" : (r.isFailed() ? "badge-fail" : (r.hasWarnings() ? "badge-warn" : "badge-skip"));
            String statusName = r.getOverallStatus().name();
            html.append("<tr>\n");
            html.append("<td><span class=\"badge ").append(badgeClass).append("\">").append(statusName).append("</span></td>\n");
            html.append("<td><strong>").append(r.getUri()).append("</strong></td>\n");
            html.append("<td><code>").append(r.getResourceType()).append("</code></td>\n");
            html.append("<td>").append(r.getHttpStatusCode()).append("</td>\n");
            html.append("<td>").append(r.getExecutionTimeMs()).append("ms</td>\n");
            html.append("<td>\n");
            if (r.getIssues().isEmpty()) {
                html.append("<span style=\"color:#22c55e;\">✓ No issues</span>");
            } else {
                html.append("<ul class=\"issues-list\">\n");
                for (ValidationIssue i : r.getIssues()) {
                    html.append("<li><strong>[").append(i.ruleCategory()).append("]</strong> ");
                    if (i.propertyPath() != null && !i.propertyPath().isEmpty()) {
                        html.append("<code>").append(i.propertyPath()).append("</code>: ");
                    }
                    html.append(i.message());
                    if (i.expected() != null || i.actual() != null) {
                        html.append(" (<em>Expected:</em> ").append(i.expected()).append(", <em>Actual:</em> ").append(i.actual()).append(")");
                    }
                    html.append("</li>\n");
                }
                html.append("</ul>\n");
            }
            html.append("</td>\n</tr>\n");
        }

        html.append("</tbody>\n</table>\n</div>\n</body>\n</html>\n");

        Path path = Paths.get(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, html.toString());
        return path.toAbsolutePath().toString();
    }
}
