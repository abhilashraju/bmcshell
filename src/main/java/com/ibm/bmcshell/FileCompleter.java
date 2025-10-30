package com.ibm.bmcshell;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.completion.CompletionProvider;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileCompleter implements ValueProvider {

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        String userInput = completionContext.currentWordUpToCursor();
        Path basePath;

        // Determine the base path to search for completions
        if (userInput.isEmpty()) {
            basePath = Paths.get(System.getProperty("user.dir"));
        } else {
            // For partial paths, check the parent directory
            Path typedPath = Paths.get(userInput);
            if (Files.isDirectory(typedPath)) {
                basePath = typedPath;
            } else {
                basePath = typedPath.getParent();
                // Handle the case where the user is at the root
                if (basePath == null) {
                    basePath = Paths.get("");
                }
            }
        }

        if (basePath == null || !Files.isDirectory(basePath)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(basePath)) {
            return paths
                .filter(p -> p.getFileName() != null)
                .map(p -> {
                    String relativePath;
                    if (userInput.isEmpty()) {
                        relativePath = p.getFileName().toString();
                    } else if (Files.isDirectory(Paths.get(userInput))) {
                        // If user entered a full directory, start from there
                        relativePath = userInput+p.getFileName().toString();
                    } else {
                        // For partial paths, use the full path relative to CWD
                        relativePath = userInput+p.toString();
                    }
                    
                    // Append a slash for directories to make completion seamless
                    if (Files.isDirectory(p)) {
                        relativePath += "/";
                    }
                    return new CompletionProposal(relativePath);
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }
}
