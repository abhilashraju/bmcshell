package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

/**
 * RemoteBinaryCompleter provides completion for executable binaries found in
 * /usr/bin
 * on the remote machine accessed via SSH. Results are cached per machine to
 * improve performance.
 */
@Component
public class RemoteBinaryCompleter implements ValueProvider {

    // Cache for binaries per machine (key: machine name, value: list of binaries)
    private static final Map<String, List<String>> binaryCache = new HashMap<>();

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        String userInput = completionContext.currentWordUpToCursor();

        // Get connection details from CommonCommands
        String machine = CommonCommands.machine;
        String userName = CommonCommands.getUserName();
        String passwd = CommonCommands.getPasswd();

        // If no connection is configured, return empty list
        if (machine == null || userName == null || passwd == null) {
            System.err.println("RemoteBinaryCompleter: No connection configured");
            return List.of();
        }

        try {
            // Get binaries from cache or fetch if not cached
            List<String> binaries = getBinaries(machine, userName, passwd);

            if (binaries.isEmpty()) {
                System.err.println("RemoteBinaryCompleter: No binaries found");
                return List.of();
            }

            // Filter based on user input and prepend /usr/bin/
            List<CompletionProposal> proposals = binaries.stream()
                    .filter(binary -> {
                        String fullPath = "/usr/bin/" + binary;
                        if (userInput == null || userInput.isEmpty()) {
                            return true;
                        }
                        // Match against full path or just the binary name
                        return fullPath.startsWith(userInput) || binary.startsWith(userInput);
                    })
                    .map(binary -> new CompletionProposal("/usr/bin/" + binary).complete(true))
                    .collect(Collectors.toList());

            System.err.println("RemoteBinaryCompleter: Returning " + proposals.size() + " proposals");
            return proposals;

        } catch (Exception e) {
            System.err.println("RemoteBinaryCompleter error: " + e.getMessage());
            e.printStackTrace();
            // Return empty list on error
            return List.of();
        }
    }

    /**
     * Gets binaries from cache or fetches them if not cached
     * 
     * @param machine  Remote machine name
     * @param userName SSH username
     * @param passwd   SSH password
     * @return List of binary names
     */
    private List<String> getBinaries(String machine, String userName, String passwd) {
        // Check if we have cached binaries for this machine
        synchronized (binaryCache) {
            if (binaryCache.containsKey(machine)) {
                List<String> cached = binaryCache.get(machine);
                System.err.println("RemoteBinaryCompleter: Using cached binaries (" + cached.size() + " items)");
                return cached;
            }

            // Not cached, fetch from remote
            System.err.println("RemoteBinaryCompleter: Fetching binaries from remote machine: " + machine);
            List<String> binaries = listRemoteBinaries(machine, userName, passwd);

            // Cache the result
            binaryCache.put(machine, binaries);
            System.err.println("RemoteBinaryCompleter: Cached " + binaries.size() + " binaries");

            return binaries;
        }
    }

    /**
     * Lists executable binaries in /usr/bin on the remote machine
     * 
     * @param machine  Remote machine name
     * @param userName SSH username
     * @param passwd   SSH password
     * @return List of binary names
     */
    private List<String> listRemoteBinaries(String machine, String userName, String passwd) {
        List<String> binaries = new ArrayList<>();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // List files in /usr/bin, one per line
            // -1: one file per line
            // We'll filter for executables by checking if they're files (not directories)
            String command = "ls -1 /usr/bin 2>/dev/null | sort";

            System.err.println("RemoteBinaryCompleter: Executing command: " + command);
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);

            String output = outputStream.toString().trim();
            if (!output.isEmpty()) {
                String[] lines = output.split("\\R");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        binaries.add(line);
                    }
                }
                System.err.println("RemoteBinaryCompleter: Found " + binaries.size() + " binaries");
            } else {
                System.err.println("RemoteBinaryCompleter: No output from command");
            }
        } catch (Exception e) {
            System.err.println("RemoteBinaryCompleter: Error listing binaries: " + e.getMessage());
            e.printStackTrace();
        }

        return binaries;
    }

    /**
     * Clears the cache for a specific machine or all machines
     * 
     * @param machine Machine name to clear cache for, or null to clear all
     */
    public static void clearCache(String machine) {
        synchronized (binaryCache) {
            if (machine == null) {
                binaryCache.clear();
                System.err.println("RemoteBinaryCompleter: Cleared all cache");
            } else {
                binaryCache.remove(machine);
                System.err.println("RemoteBinaryCompleter: Cleared cache for " + machine);
            }
        }
    }
}
