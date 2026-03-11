package com.ibm.bmcshell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

/**
 * Value provider for script files in shellhome directory
 * Provides auto-completion for script names
 */
@Component
public class ShellHomeScriptProvider implements ValueProvider {

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        // Get the current shellhome path from CommonCommands
        String scriptHome = CommonCommands.shellHomePath;
        Path scriptDir = Paths.get(scriptHome);

        if (!Files.exists(scriptDir)) {
            return List.of();
        }

        try (Stream<Path> pathStream = Files.list(scriptDir)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> !name.startsWith("."))
                    .filter(name -> !name.startsWith("history"))
                    .filter(name -> !name.endsWith("_out"))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }
}

// Made with Bob
