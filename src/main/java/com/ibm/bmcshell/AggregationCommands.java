package com.ibm.bmcshell;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bmcshell.Utils.Util;

/**
 * AggregationCommands - Shell commands for the Redfish Aggregation Service.
 *
 * Covers the following endpoints from aggregation_service.hpp:
 *   GET  /redfish/v1/AggregationService/
 *   GET  /redfish/v1/AggregationService/AggregationSources/
 *   POST /redfish/v1/AggregationService/AggregationSources/
 *   GET  /redfish/v1/AggregationService/AggregationSources/{id}
 *   PATCH /redfish/v1/AggregationService/AggregationSources/{id}
 *   DELETE /redfish/v1/AggregationService/AggregationSources/{id}
 */
@ShellComponent
public class AggregationCommands extends CommonCommands {

    private static final String AGG_SERVICE_PATH   = "/redfish/v1/AggregationService/";
    private static final String AGG_SOURCES_PATH   = "/redfish/v1/AggregationService/AggregationSources/";

    protected AggregationCommands() throws IOException {
    }

    // -------------------------------------------------------------------------
    // agg.service — GET /redfish/v1/AggregationService/
    // -------------------------------------------------------------------------

    /**
     * Retrieves the top-level AggregationService resource.
     *
     * Example: agg.service
     */
    @ShellMethod(key = "agg.service", value = "Get the Aggregation Service resource")
    @ShellMethodAvailability("availabilityCheck")
    public void getAggregationService() throws URISyntaxException, IOException {
        get(AGG_SERVICE_PATH, "", true);
    }

    // -------------------------------------------------------------------------
    // agg.sources — GET /redfish/v1/AggregationService/AggregationSources/
    // -------------------------------------------------------------------------

    /**
     * Lists all aggregation sources (satellite BMC entries).
     *
     * Example: agg.sources
     */
    @ShellMethod(key = "agg.sources", value = "List all AggregationSources")
    @ShellMethodAvailability("availabilityCheck")
    public void listAggregationSources() throws URISyntaxException, IOException {
        String response = makeGetRequest(AGG_SOURCES_PATH, "");
        if (response == null) {
            System.out.println("No response from AggregationSources collection.");
            return;
        }
        System.out.println(prettyPrint(response));

        // Push member URIs onto the endpoint stack so the user can pick one with 's'
        try {
            JsonNode root = new ObjectMapper().readTree(response);
            JsonNode members = root.path("Members");
            if (members.isArray()) {
                List<Util.EndPoints> eps = new ArrayList<>();
                members.forEach(m -> {
                    String odata = m.path("@odata.id").asText();
                    if (!odata.isEmpty()) {
                        eps.add(new Util.EndPoints(odata, "Get"));
                    }
                });
                if (!eps.isEmpty()) {
                    endPoints.push(eps);
                    System.out.printf("%nFound %d source(s). Use 's <index>' to inspect one.%n", eps.size());
                }
            }
        } catch (Exception ex) {
            // If JSON parsing fails the raw response was already printed above.
        }
    }

    // -------------------------------------------------------------------------
    // agg.source.get — GET /redfish/v1/AggregationService/AggregationSources/{id}
    // -------------------------------------------------------------------------

    /**
     * Retrieves details for a single aggregation source by its ID.
     *
     * Example: agg.source.get --id abc12345
     */
    @ShellMethod(key = "agg.source.get", value = "Get a specific AggregationSource by ID. eg: agg.source.get --id abc12345")
    @ShellMethodAvailability("availabilityCheck")
    public void getAggregationSource(
            @ShellOption(value = { "--id", "-i" }, help = "AggregationSource ID") String id)
            throws URISyntaxException, IOException {
        get(AGG_SOURCES_PATH + id + "/", "", false);
    }

    // -------------------------------------------------------------------------
    // agg.source.add — POST /redfish/v1/AggregationService/AggregationSources/
    // -------------------------------------------------------------------------

    /**
     * Registers a new aggregation source (satellite BMC).
     * HostName must be an absolute URI, e.g. https://192.168.1.50
     *
     * Example: agg.source.add --hostname https://192.168.1.50
     * Example: agg.source.add --hostname https://192.168.1.50 --username admin --password secret
     */
    @ShellMethod(key = "agg.source.add", value = "Add a new AggregationSource (satellite BMC). eg: agg.source.add --hostname https://192.168.1.50 [--username admin] [--password secret]")
    @ShellMethodAvailability("availabilityCheck")
    public void addAggregationSource(
            @ShellOption(value = { "--hostname" }, help = "Satellite BMC URL, e.g. https://192.168.1.50") String hostname,
            @ShellOption(value = { "--username", "-u" }, defaultValue = ShellOption.NULL, help = "Optional username") String username,
            @ShellOption(value = { "--password", "-p" }, defaultValue = ShellOption.NULL, help = "Optional password") String password)
            throws URISyntaxException, IOException {

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append(String.format("\"HostName\": \"%s\"", hostname));
        if (username != null && !username.isEmpty()) {
            json.append(String.format(", \"UserName\": \"%s\"", username));
        }
        if (password != null && !password.isEmpty()) {
            json.append(String.format(", \"Password\": \"%s\"", password));
        }
        json.append("}");

        post(AGG_SOURCES_PATH, json.toString(), false);
    }

    // -------------------------------------------------------------------------
    // agg.source.update — PATCH /redfish/v1/AggregationService/AggregationSources/{id}
    // -------------------------------------------------------------------------

    /**
     * Updates the credentials of an existing writable aggregation source.
     *
     * Example: agg.source.update --id abc12345 --username admin
     * Example: agg.source.update --id abc12345 --password newpassword
     */
    @ShellMethod(key = "agg.source.update", value = "Update credentials for an AggregationSource. eg: agg.source.update --id abc12345 [--username admin] [--password secret]")
    @ShellMethodAvailability("availabilityCheck")
    public void updateAggregationSource(
            @ShellOption(value = { "--id", "-i" }, help = "AggregationSource ID") String id,
            @ShellOption(value = { "--username", "-u" }, defaultValue = ShellOption.NULL, help = "New username") String username,
            @ShellOption(value = { "--password", "-p" }, defaultValue = ShellOption.NULL, help = "New password") String password)
            throws URISyntaxException, IOException {

        if (username == null && password == null) {
            System.out.println("Nothing to update: provide --username and/or --password.");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;
        if (username != null) {
            json.append(String.format("\"UserName\": \"%s\"", username));
            first = false;
        }
        if (password != null) {
            if (!first) {
                json.append(", ");
            }
            json.append(String.format("\"Password\": \"%s\"", password));
        }
        json.append("}");

        patch(AGG_SOURCES_PATH + id + "/", json.toString());
    }

    // -------------------------------------------------------------------------
    // agg.source.delete — DELETE /redfish/v1/AggregationService/AggregationSources/{id}
    // -------------------------------------------------------------------------

    /**
     * Removes a writable aggregation source by its ID.
     *
     * Example: agg.source.delete --id abc12345
     */
    @ShellMethod(key = "agg.source.delete", value = "Delete an AggregationSource by ID. eg: agg.source.delete --id abc12345")
    @ShellMethodAvailability("availabilityCheck")
    public void deleteAggregationSource(
            @ShellOption(value = { "--id", "-i" }, help = "AggregationSource ID") String id)
            throws URISyntaxException, IOException {

        System.out.println(makeDeleteRequest(AGG_SOURCES_PATH + id + "/"));
    }

    // -------------------------------------------------------------------------
    // agg.help — quick reference
    // -------------------------------------------------------------------------

    @ShellMethod(key = "agg.help", value = "Show Aggregation Service command reference")
    public String aggregationHelp() {
        return """
                ┌─────────────────────────────────────────────────────────────────────────┐
                │                   Aggregation Service Commands                          │
                ├─────────────────────┬───────────────────────────────────────────────────┤
                │ agg.service         │ GET  /redfish/v1/AggregationService/              │
                │ agg.sources         │ GET  AggregationSources collection                │
                │ agg.source.get      │ GET  AggregationSources/{id}  --id <id>           │
                │ agg.source.add      │ POST AggregationSources       --hostname <url>    │
                │                     │      [--username <u>] [--password <p>]            │
                │ agg.source.update   │ PATCH AggregationSources/{id} --id <id>           │
                │                     │      [--username <u>] [--password <p>]            │
                │ agg.source.delete   │ DELETE AggregationSources/{id} --id <id>          │
                └─────────────────────┴───────────────────────────────────────────────────┘
                """;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Pretty-prints a JSON string; falls back to the raw string on error. */
    private String prettyPrint(String json) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new ObjectMapper().readTree(json));
        } catch (Exception e) {
            return json;
        }
    }
}
