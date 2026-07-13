package com.ibm.bmcshell;

import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.stream.Collectors;

@ShellComponent
public class SearchCommands {

    private final CommandCatalog commandCatalog;

    public SearchCommands(@Lazy CommandCatalog commandCatalog) {
        this.commandCatalog = commandCatalog;
    }

    @ShellMethod(key = "search", value = "Search for commands by keyword or wildcard pattern. "
            + "Supports * (any chars) and ? (single char). "
            + "eg: search *sensor*  or  search get*  or  search delete")
    public String search(
            @ShellOption(help = "Keyword or wildcard pattern to search for (case-insensitive)") String pattern) {

        // Build regex: escape dots, convert wildcards, wrap with .* so a plain
        // word like "sensor" matches anywhere in the command name.
        String regex = ".*"
                + pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".")
                + ".*";

        String results = commandCatalog.getRegistrations().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().matches(regex.toLowerCase()))
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> {
                    String desc = e.getValue().getDescription();
                    return String.format("  %-35s %s", e.getKey(), desc != null ? desc : "");
                })
                .collect(Collectors.joining("\n"));

        if (results.isBlank()) {
            return "No commands found matching: " + pattern;
        }
        return "Commands matching '" + pattern + "':\n" + results;
    }
}
