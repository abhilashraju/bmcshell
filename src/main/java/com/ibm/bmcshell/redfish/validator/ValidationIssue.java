package com.ibm.bmcshell.redfish.validator;

/**
 * Represents a single validation issue or warning found during Redfish conformance testing.
 */
public record ValidationIssue(
        ValidationLevel level,
        String ruleCategory,
        String propertyPath,
        String message,
        String expected,
        String actual
) {
    public static ValidationIssue fail(String ruleCategory, String propertyPath, String message) {
        return new ValidationIssue(ValidationLevel.FAIL, ruleCategory, propertyPath, message, null, null);
    }

    public static ValidationIssue fail(String ruleCategory, String propertyPath, String message, String expected, String actual) {
        return new ValidationIssue(ValidationLevel.FAIL, ruleCategory, propertyPath, message, expected, actual);
    }

    public static ValidationIssue warn(String ruleCategory, String propertyPath, String message) {
        return new ValidationIssue(ValidationLevel.WARN, ruleCategory, propertyPath, message, null, null);
    }

    public static ValidationIssue warn(String ruleCategory, String propertyPath, String message, String expected, String actual) {
        return new ValidationIssue(ValidationLevel.WARN, ruleCategory, propertyPath, message, expected, actual);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(level).append("] ");
        sb.append("[").append(ruleCategory).append("] ");
        if (propertyPath != null && !propertyPath.isEmpty()) {
            sb.append(propertyPath).append(": ");
        }
        sb.append(message);
        if (expected != null || actual != null) {
            sb.append(" (Expected: ").append(expected).append(", Actual: ").append(actual).append(")");
        }
        return sb.toString();
    }
}
