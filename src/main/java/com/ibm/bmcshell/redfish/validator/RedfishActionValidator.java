package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Validates Action objects, target endpoints, and @Redfish.AllowableValues.
 */
public class RedfishActionValidator {

    public static List<ValidationIssue> validateActions(JsonNode rootNode) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (rootNode == null || !rootNode.has("Actions")) {
            return issues;
        }

        JsonNode actionsNode = rootNode.get("Actions");
        if (!actionsNode.isObject()) {
            issues.add(ValidationIssue.fail("ACTIONS_CHECK", "Actions", "'Actions' property must be a JSON Object"));
            return issues;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = actionsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String actionName = entry.getKey();
            JsonNode actionObj = entry.getValue();

            if ("Oem".equals(actionName)) {
                // OEM actions object
                if (actionObj.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> oemFields = actionObj.fields();
                    while (oemFields.hasNext()) {
                        Map.Entry<String, JsonNode> oemEntry = oemFields.next();
                        validateSingleAction("Actions/Oem/" + oemEntry.getKey(), oemEntry.getValue(), issues);
                    }
                }
                continue;
            }

            if (!actionName.startsWith("#")) {
                issues.add(ValidationIssue.warn("ACTIONS_CHECK", "Actions/" + actionName,
                        "Standard Action property key should start with '#'", "#<Namespace>.<ActionName>", actionName));
            }

            validateSingleAction("Actions/" + actionName, actionObj, issues);
        }

        return issues;
    }

    private static void validateSingleAction(String path, JsonNode actionObj, List<ValidationIssue> issues) {
        if (!actionObj.isObject()) {
            issues.add(ValidationIssue.fail("ACTIONS_CHECK", path, "Action definition must be a JSON Object"));
            return;
        }

        // 1. Target property
        if (!actionObj.has("target")) {
            issues.add(ValidationIssue.fail("ACTIONS_CHECK", path + "/target", "Missing required 'target' URI for action"));
        } else {
            String target = actionObj.get("target").asText();
            if (!target.startsWith("/")) {
                issues.add(ValidationIssue.fail("ACTIONS_CHECK", path + "/target",
                        "Action target must be an absolute URI starting with '/'", "/...", target));
            }
        }

        // 2. Check parameters and @Redfish.AllowableValues
        Iterator<Map.Entry<String, JsonNode>> actionFields = actionObj.fields();
        while (actionFields.hasNext()) {
            Map.Entry<String, JsonNode> param = actionFields.next();
            String paramName = param.getKey();
            JsonNode paramValue = param.getValue();

            if (paramName.endsWith("@Redfish.AllowableValues")) {
                if (!paramValue.isArray()) {
                    issues.add(ValidationIssue.fail("ACTIONS_CHECK", path + "/" + paramName,
                            "'@Redfish.AllowableValues' must be a JSON Array of allowed values"));
                } else {
                    for (int i = 0; i < paramValue.size(); i++) {
                        if (!paramValue.get(i).isTextual()) {
                            issues.add(ValidationIssue.warn("ACTIONS_CHECK", path + "/" + paramName + "[" + i + "]",
                                    "Allowable value entry should be a string"));
                        }
                    }
                }
            }
        }
    }
}
