package com.ibm.bmcshell.redfish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.redfish.validator.*;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedfishValidatorTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testProtocolValidation() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        headers.add("OData-Version", "4.0");

        List<ValidationIssue> issues = RedfishProtocolValidator.validateResponse("/redfish/v1", HttpStatus.OK, headers);
        assertTrue(issues.isEmpty(), "Valid headers should produce no issues");

        // Invalid content-type and missing OData-Version
        HttpHeaders badHeaders = new HttpHeaders();
        badHeaders.add(HttpHeaders.CONTENT_TYPE, "text/html");
        List<ValidationIssue> badIssues = RedfishProtocolValidator.validateResponse("/redfish/v1", HttpStatus.NOT_FOUND, badHeaders);
        assertFalse(badIssues.isEmpty());
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("HTTP_STATUS")));
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("HTTP_HEADER")));
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("ODATA_HEADER")));
    }

    @Test
    public void testODataStructureValidation() throws Exception {
        String validJson = """
            {
                "@odata.id": "/redfish/v1/Systems/system",
                "@odata.type": "#ComputerSystem.v1_15_0.ComputerSystem",
                "Id": "system",
                "Name": "Primary System",
                "PowerState": "On"
            }
            """;
        JsonNode root = mapper.readTree(validJson);
        List<ValidationIssue> issues = RedfishODataValidator.validateODataStructure("/redfish/v1/Systems/system", root);
        assertTrue(issues.isEmpty(), "Valid OData resource should have no issues: " + issues);

        // Mismatched URI and missing Name
        String badJson = """
            {
                "@odata.id": "/redfish/v1/Systems/wrong",
                "@odata.type": "ComputerSystem",
                "Id": "system"
            }
            """;
        JsonNode badRoot = mapper.readTree(badJson);
        List<ValidationIssue> badIssues = RedfishODataValidator.validateODataStructure("/redfish/v1/Systems/system", badRoot);
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("ODATA_IDENTITY")));
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("ODATA_PROPERTY")));
    }

    @Test
    public void testCollectionValidation() throws Exception {
        String collectionJson = """
            {
                "@odata.id": "/redfish/v1/Chassis",
                "@odata.type": "#ChassisCollection.ChassisCollection",
                "Name": "Chassis Collection",
                "Members@odata.count": 2,
                "Members": [
                    { "@odata.id": "/redfish/v1/Chassis/chassis1" },
                    { "@odata.id": "/redfish/v1/Chassis/chassis2" }
                ]
            }
            """;
        JsonNode root = mapper.readTree(collectionJson);
        List<ValidationIssue> issues = RedfishODataValidator.validateODataStructure("/redfish/v1/Chassis", root);
        assertTrue(issues.isEmpty(), "Valid collection should have no issues: " + issues);

        // Count mismatch
        String badCollectionJson = """
            {
                "@odata.id": "/redfish/v1/Chassis",
                "@odata.type": "#ChassisCollection.ChassisCollection",
                "Name": "Chassis Collection",
                "Members@odata.count": 5,
                "Members": [
                    { "@odata.id": "/redfish/v1/Chassis/chassis1" }
                ]
            }
            """;
        JsonNode badRoot = mapper.readTree(badCollectionJson);
        List<ValidationIssue> badIssues = RedfishODataValidator.validateODataStructure("/redfish/v1/Chassis", badRoot);
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("ODATA_COLLECTION")));
    }

    @Test
    public void testActionsValidation() throws Exception {
        String actionsJson = """
            {
                "Actions": {
                    "#ComputerSystem.Reset": {
                        "target": "/redfish/v1/Systems/system/Actions/ComputerSystem.Reset",
                        "ResetType@Redfish.AllowableValues": [
                            "On",
                            "ForceOff",
                            "GracefulRestart"
                        ]
                    }
                }
            }
            """;
        JsonNode root = mapper.readTree(actionsJson);
        List<ValidationIssue> issues = RedfishActionValidator.validateActions(root);
        assertTrue(issues.isEmpty(), "Valid actions should pass: " + issues);

        // Missing target
        String badActionsJson = """
            {
                "Actions": {
                    "#ComputerSystem.Reset": {
                        "title": "Reset"
                    }
                }
            }
            """;
        JsonNode badRoot = mapper.readTree(badActionsJson);
        List<ValidationIssue> badIssues = RedfishActionValidator.validateActions(badRoot);
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("ACTIONS_CHECK")));
    }

    @Test
    public void testDateTimeAndDurationValidation() throws Exception {
        String json = """
            {
                "DateTime": "2026-11-20T07:30:00Z",
                "Created": "2026-11-20T12:45:00+05:30",
                "TimeoutDuration": "PT10M"
            }
            """;
        JsonNode root = mapper.readTree(json);
        List<ValidationIssue> issues = RedfishDateTimeAndTypeValidator.validateDateTimesAndTypes(root);
        assertTrue(issues.isEmpty(), "Valid datetimes and durations should pass: " + issues);

        String badJson = """
            {
                "DateTime": "2026/11/20 07:30:00",
                "TimeoutDuration": "10 minutes"
            }
            """;
        JsonNode badRoot = mapper.readTree(badJson);
        List<ValidationIssue> badIssues = RedfishDateTimeAndTypeValidator.validateDateTimesAndTypes(badRoot);
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("DATETIME_FORMAT")));
        assertTrue(badIssues.stream().anyMatch(i -> i.ruleCategory().equals("DURATION_FORMAT")));
    }

    @Test
    public void testJsonSchemaValidation() throws Exception {
        String schemaJson = """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "required": ["Id", "Name", "PowerState"],
                "properties": {
                    "Id": { "type": "string" },
                    "Name": { "type": "string" },
                    "PowerState": { "type": "string", "enum": ["On", "Off", "PoweringOn"] }
                }
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaJson);

        String validPayload = """
            {
                "Id": "system",
                "Name": "Primary System",
                "PowerState": "On"
            }
            """;
        List<ValidationIssue> validIssues = RedfishJsonSchemaValidator.validatePayload(mapper.readTree(validPayload), schema);
        assertTrue(validIssues.isEmpty(), "Valid payload should pass schema: " + validIssues);

        String invalidPayload = """
            {
                "Id": "system",
                "PowerState": "InvalidState"
            }
            """;
        List<ValidationIssue> invalidIssues = RedfishJsonSchemaValidator.validatePayload(mapper.readTree(invalidPayload), schema);
        assertFalse(invalidIssues.isEmpty());
        assertTrue(invalidIssues.stream().anyMatch(i -> i.ruleCategory().equals("SCHEMA_REQUIRED")));
        assertTrue(invalidIssues.stream().anyMatch(i -> i.ruleCategory().equals("SCHEMA_ENUM")));
    }

    @Test
    public void testValidationReportAndHtmlGeneration() throws Exception {
        ValidationReport report = new ValidationReport("test-bmc");

        ResourceValidationResult r1 = new ResourceValidationResult("/redfish/v1/Systems/system");
        r1.setHttpStatusCode(200);
        r1.setResourceType("#ComputerSystem.v1_15_0.ComputerSystem");
        r1.setExecutionTimeMs(45);
        report.addResult(r1);

        ResourceValidationResult r2 = new ResourceValidationResult("/redfish/v1/Chassis/bad");
        r2.setHttpStatusCode(200);
        r2.setResourceType("#Chassis.v1_14_0.Chassis");
        r2.setExecutionTimeMs(30);
        r2.addIssue(ValidationIssue.fail("ODATA_PROPERTY", "Id", "Missing Id"));
        report.addResult(r2);

        assertEquals(2, report.getTotalCount());
        assertEquals(1, report.getPassedCount());
        assertEquals(1, report.getFailedCount());

        String consoleSummary = report.toConsoleSummary();
        assertNotNull(consoleSummary);
        assertTrue(consoleSummary.contains("REDFISH SERVICE CONFORMANCE VALIDATION REPORT"));

        Path tempHtml = Files.createTempFile("rf_test_report", ".html");
        String savedPath = report.saveHtmlReport(tempHtml.toString());
        assertTrue(Files.exists(Path.of(savedPath)));
        String htmlContent = Files.readString(Path.of(savedPath));
        assertTrue(htmlContent.contains("Redfish Service Conformance Report"));
        assertTrue(htmlContent.contains("/redfish/v1/Systems/system"));
        Files.deleteIfExists(tempHtml);
    }

    @Test
    public void testChildUriExtractionForParallelCrawling() throws Exception {
        String json = """
            {
                "@odata.id": "/redfish/v1/Systems",
                "Members": [
                    { "@odata.id": "/redfish/v1/Systems/system1" },
                    { "@odata.id": "/redfish/v1/Systems/system2" }
                ],
                "Links": {
                    "Chassis": [
                        { "@odata.id": "/redfish/v1/Chassis/chassis1" }
                    ]
                }
            }
            """;
        RedfishValidatorService service = new RedfishValidatorService();
        java.util.Set<String> childUris = service.extractChildUris(mapper.readTree(json));
        assertEquals(4, childUris.size());
        assertTrue(childUris.contains("/redfish/v1/Systems"));
        assertTrue(childUris.contains("/redfish/v1/Systems/system1"));
        assertTrue(childUris.contains("/redfish/v1/Systems/system2"));
        assertTrue(childUris.contains("/redfish/v1/Chassis/chassis1"));
    }

    @Test
    public void testRedfishSchemaManagerInitializationAndLookup() throws Exception {
        RedfishSchemaManager manager = new RedfishSchemaManager();
        assertNotNull(manager.getSchemaDirectory());
        assertNotNull(manager.getValidatorsConfig());
        assertFalse(manager.getValidatorsConfig().isPreloadJsonSchema());

        // Test candidate file name extraction
        var candidates = manager.getCandidateSchemaFileNames("#Manager.v1_11_0.Manager");
        assertTrue(candidates.contains("Manager.v1_11_0.json"));
        assertTrue(candidates.contains("Manager.json"));

        var systemCandidates = manager.getCandidateSchemaFileNames("#ComputerSystem.v1_15_0.ComputerSystem");
        assertTrue(systemCandidates.contains("ComputerSystem.v1_15_0.json"));
        assertTrue(systemCandidates.contains("ComputerSystem.json"));
    }

    @Test
    public void testRedfishMetaSchemaCompilation() throws Exception {
        RedfishSchemaManager manager = new RedfishSchemaManager();
        String schemaWithRedfishMeta = """
            {
                "$schema": "http://redfish.dmtf.org/schemas/v1/redfish-schema.v1_4_0.json",
                "title": "#ComputerSystem.v1_6_0.ComputerSystem",
                "type": "object",
                "required": ["Id", "Name"],
                "properties": {
                    "Id": { "type": "string" },
                    "Name": { "type": "string" }
                }
            }
            """;

        // Save to temp schema directory
        Path tempSchemaPath = Paths.get(manager.getSchemaDirectory(), "ComputerSystem.v1_6_0.json");
        Files.createDirectories(tempSchemaPath.getParent());
        Files.writeString(tempSchemaPath, schemaWithRedfishMeta);

        var schemaOpt = manager.getSchemaForOdataType("#ComputerSystem.v1_6_0.ComputerSystem", null);
        assertTrue(schemaOpt.isPresent(), "Schema with redfish-schema.v1_4_0.json meta-schema should load successfully");

        String validPayload = """
            {
                "Id": "hypervisor",
                "Name": "Hypervisor System"
            }
            """;
        var issues = RedfishJsonSchemaValidator.validatePayload(mapper.readTree(validPayload), schemaOpt.get());
        assertTrue(issues.isEmpty(), "Valid payload should have no validation issues: " + issues);

        Files.deleteIfExists(tempSchemaPath);
    }
}
