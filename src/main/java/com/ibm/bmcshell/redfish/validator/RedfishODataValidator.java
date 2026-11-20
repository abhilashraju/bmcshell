package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates core OData v4 and Redfish structural conventions on JSON payloads.
 */
public class RedfishODataValidator {

    private static final Pattern ODATA_TYPE_PATTERN = Pattern.compile("^#([A-Za-z0-9_]+)(?:\\.(v[0-9]+_[0-9]+_[0-9]+))?\\.([A-Za-z0-9_]+)$");

    public static List<ValidationIssue> validateODataStructure(String requestUri, JsonNode rootNode) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (rootNode == null || !rootNode.isObject()) {
            issues.add(ValidationIssue.fail("ODATA_STRUCTURE", "", "Response payload is not a valid JSON Object"));
            return issues;
        }

        // 1. @odata.id validation
        if (!rootNode.has("@odata.id")) {
            issues.add(ValidationIssue.fail("ODATA_IDENTITY", "@odata.id", "Missing required '@odata.id' property"));
        } else {
            String odataId = rootNode.get("@odata.id").asText();
            String cleanRequestUri = normalizeUri(requestUri);
            String cleanOdataId = normalizeUri(odataId);

            if (!cleanRequestUri.equalsIgnoreCase(cleanOdataId)) {
                issues.add(ValidationIssue.fail("ODATA_IDENTITY", "@odata.id",
                        "'@odata.id' does not match requested URI", cleanRequestUri, cleanOdataId));
            }
        }

        // 2. @odata.type validation
        if (!rootNode.has("@odata.type")) {
            issues.add(ValidationIssue.fail("ODATA_TYPE", "@odata.type", "Missing required '@odata.type' property"));
        } else {
            String odataType = rootNode.get("@odata.type").asText();
            if (!ODATA_TYPE_PATTERN.matcher(odataType).matches() && !odataType.startsWith("#")) {
                issues.add(ValidationIssue.warn("ODATA_TYPE", "@odata.type",
                        "'@odata.type' does not conform to standard format '#Namespace.TypeName'", "#<Schema>.<Type>", odataType));
            }
        }

        // 3. Collection resource validation (Members and Members@odata.count)
        if (rootNode.has("Members")) {
            JsonNode membersNode = rootNode.get("Members");
            if (!membersNode.isArray()) {
                issues.add(ValidationIssue.fail("ODATA_COLLECTION", "Members", "Property 'Members' must be a JSON Array"));
            } else {
                int arraySize = membersNode.size();

                if (!rootNode.has("Members@odata.count")) {
                    issues.add(ValidationIssue.fail("ODATA_COLLECTION", "Members@odata.count",
                            "Missing required 'Members@odata.count' in collection resource"));
                } else {
                    JsonNode countNode = rootNode.get("Members@odata.count");
                    if (!countNode.isIntegralNumber()) {
                        issues.add(ValidationIssue.fail("ODATA_COLLECTION", "Members@odata.count",
                                "'Members@odata.count' must be an integer"));
                    } else {
                        int reportedCount = countNode.asInt();
                        if (reportedCount != arraySize) {
                            issues.add(ValidationIssue.fail("ODATA_COLLECTION", "Members@odata.count",
                                    "'Members@odata.count' does not match Members array size",
                                    String.valueOf(arraySize), String.valueOf(reportedCount)));
                        }
                    }
                }

                // Verify elements in Members array
                for (int i = 0; i < arraySize; i++) {
                    JsonNode member = membersNode.get(i);
                    if (!member.isObject() || !member.has("@odata.id")) {
                        issues.add(ValidationIssue.fail("ODATA_COLLECTION", "Members[" + i + "]",
                                "Collection member must be an object containing '@odata.id'"));
                    }
                }
            }
        }

        // 4. Resource Entity validation (Id and Name required unless ServiceRoot / $metadata)
        boolean isServiceRoot = requestUri.endsWith("/redfish/v1") || requestUri.endsWith("/redfish/v1/");
        if (!isServiceRoot && !rootNode.has("Members")) {
            if (!rootNode.has("Id")) {
                issues.add(ValidationIssue.fail("ODATA_PROPERTY", "Id", "Missing mandatory 'Id' property on resource entity"));
            } else {
                JsonNode idNode = rootNode.get("Id");
                if (!idNode.isTextual() && !idNode.isIntegralNumber()) {
                    issues.add(ValidationIssue.fail("ODATA_PROPERTY", "Id", "'Id' property must be a string or integer"));
                }
            }

            if (!rootNode.has("Name")) {
                issues.add(ValidationIssue.fail("ODATA_PROPERTY", "Name", "Missing mandatory 'Name' property on resource entity"));
            } else if (!rootNode.get("Name").isTextual()) {
                issues.add(ValidationIssue.fail("ODATA_PROPERTY", "Name", "'Name' property must be a string"));
            }
        }

        return issues;
    }

    private static String normalizeUri(String uri) {
        if (uri == null) return "";
        String s = uri.trim();
        if (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
