package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates @Message.ExtendedInfo and Redfish error response structures against Message Registries.
 */
public class RedfishMessageRegistryValidator {

    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("^([A-Za-z0-9_]+)\\.([0-9]+)\\.([0-9]+)\\.([A-Za-z0-9_]+)$");

    public static List<ValidationIssue> validateMessageRegistries(JsonNode rootNode, RedfishSchemaManager schemaManager, WebClient webClient) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (rootNode == null) return issues;

        traverseAndValidateMessages("", rootNode, schemaManager, webClient, issues);
        return issues;
    }

    private static void traverseAndValidateMessages(String currentPath, JsonNode node, RedfishSchemaManager schemaManager, WebClient webClient, List<ValidationIssue> issues) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode child = entry.getValue();
                String childPath = currentPath.isEmpty() ? key : currentPath + "/" + key;

                if (key.endsWith("@Message.ExtendedInfo") || "error".equals(key) || "messages".equals(key)) {
                    if (child.isArray()) {
                        for (int i = 0; i < child.size(); i++) {
                            validateSingleMessage(childPath + "[" + i + "]", child.get(i), schemaManager, webClient, issues);
                        }
                    } else if (child.isObject()) {
                        if (child.has("@Message.ExtendedInfo")) {
                            JsonNode ext = child.get("@Message.ExtendedInfo");
                            if (ext.isArray()) {
                                for (int i = 0; i < ext.size(); i++) {
                                    validateSingleMessage(childPath + "/@Message.ExtendedInfo[" + i + "]", ext.get(i), schemaManager, webClient, issues);
                                }
                            }
                        } else if (child.has("code") && child.has("message")) {
                            validateSingleMessage(childPath, child, schemaManager, webClient, issues);
                        }
                    }
                }

                traverseAndValidateMessages(childPath, child, schemaManager, webClient, issues);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                traverseAndValidateMessages(currentPath + "[" + i + "]", node.get(i), schemaManager, webClient, issues);
            }
        }
    }

    private static void validateSingleMessage(String path, JsonNode msgNode, RedfishSchemaManager schemaManager, WebClient webClient, List<ValidationIssue> issues) {
        if (!msgNode.isObject()) {
            issues.add(ValidationIssue.fail("MESSAGE_REGISTRY", path, "Message item must be a JSON Object"));
            return;
        }

        if (!msgNode.has("MessageId")) {
            issues.add(ValidationIssue.fail("MESSAGE_REGISTRY", path + "/MessageId", "Missing required 'MessageId' property in message payload"));
            return;
        }

        String messageId = msgNode.get("MessageId").asText();
        Matcher matcher = MESSAGE_ID_PATTERN.matcher(messageId);

        if (!matcher.matches()) {
            issues.add(ValidationIssue.warn("MESSAGE_REGISTRY", path + "/MessageId",
                    "MessageId does not follow standard 'Registry.Major.Minor.Key' format", "Registry.Major.Minor.Key", messageId));
        } else {
            String registry = matcher.group(1);
            String messageKey = matcher.group(4);

            if (schemaManager != null) {
                Optional<JsonNode> defOpt = schemaManager.getMessageDefinition(registry, messageKey, webClient);
                if (defOpt.isPresent()) {
                    JsonNode def = defOpt.get();
                    if (def.has("NumberOfArgs") && msgNode.has("MessageArgs")) {
                        int expectedArgs = def.get("NumberOfArgs").asInt();
                        int actualArgs = msgNode.get("MessageArgs").size();
                        if (expectedArgs != actualArgs) {
                            issues.add(ValidationIssue.fail("MESSAGE_REGISTRY", path + "/MessageArgs",
                                    "MessageArgs count does not match registry definition for " + messageId,
                                    String.valueOf(expectedArgs), String.valueOf(actualArgs)));
                        }
                    }
                }
            }
        }

        if (!msgNode.has("Message")) {
            issues.add(ValidationIssue.warn("MESSAGE_REGISTRY", path + "/Message", "Missing 'Message' text in message payload"));
        }
    }
}
