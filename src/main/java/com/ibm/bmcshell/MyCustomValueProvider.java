package com.ibm.bmcshell;

import java.util.List;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.inferencing.LLaMA3Client;

@Component
public class MyCustomValueProvider implements ValueProvider {
    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        String userInput = context.currentWordUpToCursor();
        try {
            var suggestions = LLaMA3Client.suggest(userInput);
             return List.of (new CompletionProposal(suggestions));
        } catch (Exception e) {
            
            return List.of(new CompletionProposal(userInput));
        }

    }
}