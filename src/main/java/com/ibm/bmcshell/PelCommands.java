package com.ibm.bmcshell;

import static com.ibm.bmcshell.ssh.SSHShellClient.runCommand;

import java.io.IOException;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Io;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.bmcshell.Utils.Util;

@ShellComponent
public class PelCommands extends CommonCommands{
    PelCommands() throws IOException {
        super();
    }
    String  getPel(){
        StringBuffer sb = new StringBuffer();
        runCommand(Util.fullMachineName(machine), userName, passwd, "peltool -a", sb);
        return sb.toString();
    }
    @ShellMethod(key="pel.list", value = "List the PELs use pel.list")
    @ShellMethodAvailability("availabilityCheck")
    void pelList() throws IOException, InterruptedException {
        String pelOutput = getPel();
        if (pelOutput.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            var tree=mapper.readTree(pelOutput);
            String formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
            System.out.println(formattedJson);
        } else {
            System.out.println("No PELs found.");
        }
    }
    java.util.stream.Stream<JsonNode> primarySrc(JsonNode node, String filter) {
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
            .filter(s -> s.get("Primary SRC").has(filter));
    }
    
    @ShellMethod(key="pel.callouts", value = "List callouts pel.callouts")
    @ShellMethodAvailability("availabilityCheck")
    void pelCallouts() throws IOException, InterruptedException {
        String pelOutput = getPel();
        if (pelOutput.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            var tree=mapper.readTree(pelOutput);
            primarySrc(tree, "Callout Section").forEach(callout -> {
                try {
                    ObjectMapper mapper2 = new ObjectMapper();
                    String json = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(callout);
                    System.out.println(json);
                } catch (IOException e) {
                    System.err.println("Error printing callout: " + e.getMessage());
                }
               
            });
        } else {
            System.out.println("No PELs found.");
        }
    }
    @ShellMethod(key="pel.count", value = "No of pels pel.count")
    @ShellMethodAvailability("availabilityCheck")
    void pelCount() throws InterruptedException {
        scmd(String.format("peltool -n "));
    }
    @ShellMethod(key="pel.createdBy", value = "No of pels pel.createdBy pattern")
    @ShellMethodAvailability("availabilityCheck")
    void pelCount(String pattern) throws InterruptedException, JsonMappingException, JsonProcessingException {
        String pelOutput = getPel();
        if (pelOutput.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            var tree=mapper.readTree(pelOutput);
            primarySrc(tree, "Created by").filter(s->s.get("Primary SRC").get("Created by").asText().contains(pattern))
                .forEach(callout -> {
                    try {
                        ObjectMapper mapper2 = new ObjectMapper();
                        String json = mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(callout);
                        System.out.println(json);
                    } catch (IOException e) {
                        System.err.println("Error printing callout: " + e.getMessage());
                    }
                });
        } else {
            System.out.println("No PELs found.");
        }
    }
}
