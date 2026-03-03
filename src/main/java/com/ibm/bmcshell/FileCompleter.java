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
                .filter(p -> {
                    // Filter files that match the partial input
                    String fileName = p.getFileName().toString();
                    if (userInput.isEmpty()) {
                        return true;
                    }
                    
                    Path typedPath = Paths.get(userInput);
                    if (Files.isDirectory(typedPath)) {
                        // If user typed a complete directory, show all contents
                        return true;
                    } else {
                        // If user typed partial filename, filter by prefix
                        String partialName = typedPath.getFileName().toString();
                        return fileName.startsWith(partialName);
                    }
                })
                .map(p -> {
                    String relativePath;
                    String fileName = p.getFileName().toString();
                    
                    if (userInput.isEmpty()) {
                        // No input, just show filename
                        relativePath = fileName;
                    } else {
                        Path typedPath = Paths.get(userInput);
                        if (Files.isDirectory(typedPath)) {
                            // User typed a complete directory path
                            String dirPath = userInput;
                            if (!dirPath.endsWith("/")) {
                                dirPath += "/";
                            }
                            relativePath = dirPath + fileName;
                        } else {
                            // User typed partial filename - replace the partial with full filename
                            Path parentPath = typedPath.getParent();
                            if (parentPath != null) {
                                relativePath = parentPath.toString() + "/" + fileName;
                            } else {
                                relativePath = fileName;
                            }
                        }
                    }
                    
                    boolean isDirectory = Files.isDirectory(p);
                    // Append a slash for directories to make completion seamless
                    if (isDirectory && !relativePath.endsWith("/")) {
                        relativePath += "/";
                    }
                    
                    // Create completion proposal with proper settings
                    // For directories: dontQuote(true) prevents space after completion
                    // For files: complete(true) adds space after completion
                    return new CompletionProposal(relativePath)
                        .dontQuote(isDirectory)
                        .complete(!isDirectory);
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }
}
