package com.ibm.bmcshell.redfish.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete validation result for a single Redfish URI endpoint.
 */
public class ResourceValidationResult {

    private final String uri;
    private int httpStatusCode;
    private String resourceType; // from @odata.type
    private long executionTimeMs;
    private final List<ValidationIssue> issues = new ArrayList<>();
    private ValidationLevel overallStatus = ValidationLevel.PASS;

    public ResourceValidationResult(String uri) {
        this.uri = uri;
    }

    public void addIssue(ValidationIssue issue) {
        if (issue == null) return;
        issues.add(issue);
        if (issue.level() == ValidationLevel.FAIL) {
            overallStatus = ValidationLevel.FAIL;
        } else if (issue.level() == ValidationLevel.WARN && overallStatus != ValidationLevel.FAIL) {
            overallStatus = ValidationLevel.WARN;
        }
    }

    public void addIssues(List<ValidationIssue> newIssues) {
        if (newIssues != null) {
            for (ValidationIssue issue : newIssues) {
                addIssue(issue);
            }
        }
    }

    public void setSkip(String reason) {
        this.overallStatus = ValidationLevel.SKIP;
        issues.add(new ValidationIssue(ValidationLevel.SKIP, "GENERAL", "", reason, null, null));
    }

    public String getUri() {
        return uri;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getResourceType() {
        return resourceType != null ? resourceType : "Unknown";
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<ValidationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public ValidationLevel getOverallStatus() {
        return overallStatus;
    }

    public boolean isPassed() {
        return overallStatus == ValidationLevel.PASS;
    }

    public boolean isFailed() {
        return overallStatus == ValidationLevel.FAIL;
    }

    public boolean hasWarnings() {
        return overallStatus == ValidationLevel.WARN;
    }

    public boolean isSkipped() {
        return overallStatus == ValidationLevel.SKIP;
    }

    public long getFailCount() {
        return issues.stream().filter(i -> i.level() == ValidationLevel.FAIL).count();
    }

    public long getWarnCount() {
        return issues.stream().filter(i -> i.level() == ValidationLevel.WARN).count();
    }
}
