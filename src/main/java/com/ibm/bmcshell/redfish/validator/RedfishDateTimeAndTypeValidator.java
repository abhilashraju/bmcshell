package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates ISO 8601 DateTime strings, Duration strings, and standard Redfish property types.
 */
public class RedfishDateTimeAndTypeValidator {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^P(?:\\d+Y)?(?:\\d+M)?(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+(?:\\.\\d+)?S)?)?$");

    public static List<ValidationIssue> validateDateTimesAndTypes(JsonNode rootNode) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (rootNode == null) return issues;

        traverseAndValidate("", rootNode, issues);
        return issues;
    }

    private static void traverseAndValidate(String path, JsonNode node, List<ValidationIssue> issues) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                String currentPath = path.isEmpty() ? key : path + "/" + key;

                // Check DateTime fields
                if (isDateTimeField(key) && value.isTextual()) {
                    String dtStr = value.asText();
                    if (!isValidIso8601DateTime(dtStr)) {
                        issues.add(ValidationIssue.fail("DATETIME_FORMAT", currentPath,
                                "Property value is not a valid ISO 8601 DateTime string", "YYYY-MM-DDTHH:MM:SSZ or +/-offset", dtStr));
                    }
                }

                // Check Duration fields
                if (isDurationField(key) && value.isTextual()) {
                    String durStr = value.asText();
                    if (!DURATION_PATTERN.matcher(durStr).matches()) {
                        issues.add(ValidationIssue.fail("DURATION_FORMAT", currentPath,
                                "Property value is not a valid ISO 8601 Duration string (e.g. PT10M)", "ISO 8601 Duration", durStr));
                    }
                }

                traverseAndValidate(currentPath, value, issues);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                traverseAndValidate(path + "[" + i + "]", node.get(i), issues);
            }
        }
    }

    private static boolean isDateTimeField(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith("datetime") || lower.endsWith("timestamp") || lower.equals("created") || lower.equals("modified") || lower.equals("datetimeoffset");
    }

    private static boolean isDurationField(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith("duration") || lower.endsWith("interval") || lower.equals("timeout");
    }

    private static boolean isValidIso8601DateTime(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            OffsetDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            try {
                OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }
}
