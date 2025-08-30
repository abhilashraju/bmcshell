package com.ibm.bmcshell;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
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
public class ScriptNameProvider implements ValueProvider {

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        String userInput = context.currentWordUpToCursor();
        Path currentDir = Paths.get(".").normalize();

        try (Stream<Path> pathStream = Files.list(currentDir)) {
            return pathStream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(userInput)) // Filter based on user input
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // Handle exceptions, e.g., log them or return an empty list
            return List.of();
        }

    }
}

