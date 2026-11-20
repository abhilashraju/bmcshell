package com.ibm.bmcshell.redfish.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates JSON payloads against DMTF and OEM JSON Schemas using networknt JsonSchema validator.
 */
public class RedfishJsonSchemaValidator {

    public static List<ValidationIssue> validatePayload(JsonNode payload, JsonSchema jsonSchema) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (jsonSchema == null) {
            issues.add(ValidationIssue.warn("SCHEMA_VALIDATION", "", "No matching JSON Schema available for validation"));
            return issues;
        }

        if (payload == null) {
            issues.add(ValidationIssue.fail("SCHEMA_VALIDATION", "", "Payload is null"));
            return issues;
        }

        try {
            Set<ValidationMessage> validationMessages = jsonSchema.validate(payload);
            for (ValidationMessage vm : validationMessages) {
                String path = vm.getInstanceLocation() != null ? vm.getInstanceLocation().toString() : "";
                String message = vm.getMessage();
                String type = vm.getType();

                // Categorize issue
                String category = "SCHEMA_CONFORMANCE";
                if ("required".equalsIgnoreCase(type)) {
                    category = "SCHEMA_REQUIRED";
                } else if ("enum".equalsIgnoreCase(type)) {
                    category = "SCHEMA_ENUM";
                } else if ("type".equalsIgnoreCase(type)) {
                    category = "SCHEMA_TYPE";
                } else if ("additionalProperties".equalsIgnoreCase(type)) {
                    category = "SCHEMA_UNKNOWN_PROP";
                }

                issues.add(ValidationIssue.fail(category, path, message));
            }
        } catch (Exception e) {
            issues.add(ValidationIssue.warn("SCHEMA_VALIDATION", "", "Error executing JSON schema validation: " + e.getMessage()));
        }

        return issues;
    }
}
