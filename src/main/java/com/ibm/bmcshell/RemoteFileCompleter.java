package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

/**
 * RemoteFileCompleter provides file and directory completion for remote paths
 * accessed via SSH. It executes 'ls' commands on the remote machine to fetch
 * directory contents and provides completion suggestions.
 */
@Component
public class RemoteFileCompleter implements ValueProvider {

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        String userInput = completionContext.currentWordUpToCursor();
        
        // Get connection details from CommonCommands
        String machine = CommonCommands.machine;
        String userName = CommonCommands.getUserName();
        String passwd = CommonCommands.getPasswd();
        
        // If no connection is configured, return empty list
        if (machine == null || userName == null || passwd == null) {
            return List.of();
        }
        
        try {
            String remotePath;
            String prefix = "";
            String filterPrefix = "";
            
            // Determine the remote path to list and filter prefix
            if (userInput.isEmpty()) {
                // Start from current directory on remote
                remotePath = ".";
                prefix = "";
                filterPrefix = "";
            } else if (userInput.endsWith("/")) {
                // User typed a complete directory path - list everything in that directory
                remotePath = userInput;
                prefix = userInput;
                filterPrefix = "";
            } else {
                // User is typing a partial path
                int lastSlash = userInput.lastIndexOf('/');
                if (lastSlash >= 0) {
                    // Has a slash - list parent directory and filter by filename
                    remotePath = userInput.substring(0, lastSlash + 1);
                    prefix = remotePath;
                    filterPrefix = userInput.substring(lastSlash + 1);
                    if (remotePath.isEmpty()) {
                        remotePath = "/";
                        prefix = "/";
                    }
                } else {
                    // No slash - list current directory and filter by what's typed
                    remotePath = ".";
                    prefix = "";
                    filterPrefix = userInput;
                }
            }
            
            // Execute ls command on remote machine
            List<String> remoteFiles = listRemoteDirectory(machine, userName, passwd, remotePath);
            
            final String finalFilterPrefix = filterPrefix;
            final String finalPrefix = prefix;
            
            return remoteFiles.stream()
                .filter(file -> {
                    // Filter based on what user has typed after the last slash
                    if (finalFilterPrefix.isEmpty()) {
                        return true;
                    }
                    return file.startsWith(finalFilterPrefix);
                })
                .map(file -> {
                    String completionValue = finalPrefix + file;
                    boolean isDirectory = file.endsWith("/");
                    
                    // Create completion proposal with proper settings
                    // For directories: dontQuote(true) prevents space after completion
                    // For files: complete(true) adds space after completion
                    return new CompletionProposal(completionValue)
                        .dontQuote(isDirectory)
                        .complete(!isDirectory);
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            // Return empty list on error
            return List.of();
        }
    }
    
    /**
     * Lists files and directories in a remote directory
     * 
     * @param machine Remote machine name
     * @param userName SSH username
     * @param passwd SSH password
     * @param remotePath Path to list on remote machine
     * @return List of file/directory names (directories have trailing /)
     */
    private List<String> listRemoteDirectory(String machine, String userName, String passwd, String remotePath) {
        List<String> files = new ArrayList<>();
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Use ls with -1 (one file per line) and -p (append / to directories)
            // Also use -A to show hidden files but not . and ..
            String command = String.format("ls -1Ap %s 2>/dev/null", escapePath(remotePath));
            
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);
            
            String output = outputStream.toString().trim();
            if (!output.isEmpty()) {
                String[] lines = output.split("\\R");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equals("./") && !line.equals("../")) {
                        files.add(line);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail and return empty list
        }
        
        return files;
    }
    
    /**
     * Escapes special characters in path for shell command
     * 
     * @param path Path to escape
     * @return Escaped path safe for shell
     */
    private String escapePath(String path) {
        // Simple escaping - wrap in single quotes and escape any single quotes
        return "'" + path.replace("'", "'\\''") + "'";
    }
}

// Made with Bob
