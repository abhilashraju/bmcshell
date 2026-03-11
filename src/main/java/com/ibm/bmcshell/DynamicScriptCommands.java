package com.ibm.bmcshell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.commands.Script;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dynamic Script Commands - Automatically creates shell commands from script
 * files
 * in the shellhome directory. Users can execute scripts by just typing the
 * filename.
 * 
 * This class provides a unified interface to run scripts from shellhome/
 * directory.
 * Instead of typing "r -f scriptname", users can now type "scriptname"
 * directly.
 * 
 * Usage:
 * 1. Place script files in shellhome/ directory
 * 2. Run 'scripts.reload' to load them
 * 3. Type the script name to execute it
 * 
 * Example:
 * checkdump - runs shellhome/checkdump
 * ifaces - runs shellhome/ifaces
 * disses - runs shellhome/disses
 */
@ShellComponent
public class DynamicScriptCommands extends CommonCommands {

    @Autowired
    Script script;

    private Map<String, String> scriptCache = new HashMap<>();

    /**
     * Get the current shellhome path from CommonCommands
     */
    private String getScriptHome() {
        return CommonCommands.shellHomePath;
    }

    public DynamicScriptCommands() throws IOException {
        super();
        loadScripts();
    }

    /**
     * Load all script files from shellhome directory
     */
    private void loadScripts() {
        try {
            Path scriptDir = Paths.get(getScriptHome());
            if (!Files.exists(scriptDir)) {
                System.out.println(ColorPrinter
                        .yellow("Warning: shellhome directory not found at: " + scriptDir.toAbsolutePath()));
                return;
            }

            try (Stream<Path> paths = Files.walk(scriptDir, 1)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().startsWith("."))
                        .filter(p -> !p.getFileName().toString().startsWith("history"))
                        .filter(p -> !p.getFileName().toString().endsWith("_out"))
                        .forEach(p -> {
                            String filename = p.getFileName().toString();
                            scriptCache.put(filename, p.toString());
                        });
            }

            if (!scriptCache.isEmpty()) {
                System.out.println(ColorPrinter
                        .green("Loaded " + scriptCache.size() + " dynamic script commands from shellhome/"));
            }
        } catch (IOException e) {
            System.err.println("Error loading scripts: " + e.getMessage());
        }
    }

    /**
     * Reload scripts from shellhome directory
     */
    @ShellMethod(key = "scripts.reload", value = "Reload script files from shellhome directory")
    public String reloadScripts() {
        scriptCache.clear();
        loadScripts();
        return ColorPrinter.green("Scripts reloaded. Available commands: " + String.join(", ", scriptCache.keySet()));
    }

    /**
     * List all available script commands
     */
    @ShellMethod(key = "scripts.list", value = "List all available script commands from shellhome")
    public void listScripts() {
        if (scriptCache.isEmpty()) {
            System.out.println(ColorPrinter.yellow("No scripts found in shellhome/"));
            System.out.println("Place your script files in the shellhome/ directory and run 'scripts.reload'");
            return;
        }

        System.out.println(ColorPrinter.cyan("\n=== Available Script Commands ==="));
        System.out.println(ColorPrinter.gray("You can run these scripts by typing their name:\n"));

        scriptCache.keySet().stream().sorted().forEach(name -> {
            System.out.println(ColorPrinter.green("  " + name) + ColorPrinter.gray(" - run with: " + name));
        });

        System.out.println(ColorPrinter.gray("\nTo see script content: scripts.show <scriptname>"));
        System.out.println(ColorPrinter.gray("To reload scripts: scripts.reload\n"));
    }

    /**
     * Execute individual scripts - these are the dynamic commands
     * Users can type the script name directly to execute it
     */
    @ShellMethod(key = "checkdump", value = "Execute checkdump script from shellhome")
    @ShellMethodAvailability("availabilityCheck")
    public void checkdump() throws Exception {
        executeScriptByName("checkdump");
    }

    @ShellMethod(key = "checkreboot", value = "Execute checkreboot script from shellhome")
    @ShellMethodAvailability("availabilityCheck")
    public void checkreboot() throws Exception {
        executeScriptByName("checkreboot");
    }

    @ShellMethod(key = "ifaces", value = "Execute ifaces script from shellhome")
    @ShellMethodAvailability("availabilityCheck")
    public void ifaces() throws Exception {
        executeScriptByName("ifaces");
    }

    @ShellMethod(key = "disses", value = "Execute disses script from shellhome")
    @ShellMethodAvailability("availabilityCheck")
    public void disses() throws Exception {
        executeScriptByName("disses");
    }

    @ShellMethod(key = "dissub", value = "Execute dissub script from shellhome")
    @ShellMethodAvailability("availabilityCheck")
    public void dissub() throws Exception {
        executeScriptByName("dissub");
    }

    /**
     * Generic method to execute any script from shellhome by name
     * This provides a fallback for scripts not explicitly defined above
     */
    @ShellMethod(key = "x", value = "Execute any script from shellhome by name. Usage: x -s <scriptname> or x <scriptname>")
    @ShellMethodAvailability("availabilityCheck")
    public void executeAnyScript(
            @ShellOption(value = { "-s", "--script" }, valueProvider = ShellHomeScriptProvider.class) String scriptName)
            throws Exception {
        executeScriptByName(scriptName);
    }

    /**
     * Core execution logic
     */
    private void executeScriptByName(String scriptName) throws Exception {
        // First check cache
        String scriptPath = scriptCache.get(scriptName);

        // If not in cache, try to find it
        if (scriptPath == null) {
            Path possiblePath = Paths.get(getScriptHome() + scriptName);
            if (Files.exists(possiblePath)) {
                scriptPath = possiblePath.toString();
                scriptCache.put(scriptName, scriptPath);
            } else {
                System.out.println(ColorPrinter.red("Script not found: " + scriptName));
                System.out
                        .println(ColorPrinter.yellow("Available scripts: " + String.join(", ", scriptCache.keySet())));
                System.out.println(ColorPrinter.gray("Run 'scripts.list' to see all available scripts"));
                return;
            }
        }

        System.out.println(ColorPrinter.cyan("Executing script: " + scriptName));
        File scriptFile = new File(scriptPath);
        script.script(scriptFile);
    }

    /**
     * Create a new script in shellhome using vi editor
     */
    @ShellMethod(key = "scripts.create", value = "Create a new script in shellhome using vi editor. Usage: scripts.create -s <name>")
    public String createScript(@ShellOption(value = { "-s", "--script" }) String scriptName)
            throws IOException, InterruptedException {
        Path scriptPath = Paths.get(getScriptHome() + scriptName);

        // Create shellhome directory if it doesn't exist
        Files.createDirectories(scriptPath.getParent());

        // Create empty file if it doesn't exist
        if (!Files.exists(scriptPath)) {
            Files.createFile(scriptPath);
        }

        // Open vi editor
        ProcessBuilder pb = new ProcessBuilder("vi", scriptPath.toString());
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            scriptCache.put(scriptName, scriptPath.toString());
            return ColorPrinter.green("Script created: " + scriptName + "\nYou can now run it with: x " + scriptName);
        } else {
            return ColorPrinter.red("Failed to create script");
        }
    }

    /**
     * Show script content - supports wildcards and regex
     */
    @ShellMethod(key = "scripts.show", value = "Show the content of script(s). Supports wildcards (* ?) and regex. Usage: scripts.show [-s <pattern>]")
    public void showScript(
            @ShellOption(value = { "-s", "--script" }, defaultValue = "*") String pattern)
            throws IOException {
        List<String> matchingScripts = findMatchingScripts(pattern);

        if (matchingScripts.isEmpty()) {
            System.out.println(ColorPrinter.red("No scripts found matching pattern: " + pattern));
            return;
        }

        int shownCount = 0;
        int skippedCount = 0;

        for (String scriptName : matchingScripts) {
            String scriptPath = scriptCache.get(scriptName);
            if (scriptPath != null) {
                try {
                    System.out.println(ColorPrinter.cyan("\n=== Script: " + scriptName + " ==="));
                    String content = Files.readString(Paths.get(scriptPath));
                    System.out.println(content);
                    System.out.println(ColorPrinter.cyan("=== End of " + scriptName + " ===\n"));
                    shownCount++;
                } catch (java.nio.charset.MalformedInputException e) {
                    System.out.println(ColorPrinter.yellow("Skipped (binary file): " + scriptName));
                    System.out.println(ColorPrinter.cyan("=== End of " + scriptName + " ===\n"));
                    skippedCount++;
                } catch (Exception e) {
                    System.out.println(ColorPrinter.red("Error reading: " + scriptName + " - " + e.getMessage()));
                    System.out.println(ColorPrinter.cyan("=== End of " + scriptName + " ===\n"));
                    skippedCount++;
                }
            }
        }

        System.out.println(ColorPrinter.green("Showed " + shownCount + " script(s)" +
                (skippedCount > 0 ? ", skipped " + skippedCount + " binary/unreadable file(s)" : "")));
    }

    /**
     * Edit a script using vi editor
     */
    @ShellMethod(key = "scripts.edit", value = "Edit a script using vi editor. Usage: scripts.edit -s <scriptname>")
    public String editScript(
            @ShellOption(value = { "-s", "--script" }, valueProvider = ShellHomeScriptProvider.class) String scriptName)
            throws IOException, InterruptedException {
        String scriptPath = scriptCache.get(scriptName);
        if (scriptPath == null) {
            return ColorPrinter.red("Script not found: " + scriptName);
        }

        // Open vi editor
        ProcessBuilder pb = new ProcessBuilder("vi", scriptPath);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return ColorPrinter.green("Script updated: " + scriptName);
        } else {
            return ColorPrinter.red("Failed to edit script");
        }
    }

    /**
     * Delete script(s) - supports wildcards and regex
     */
    @ShellMethod(key = "scripts.delete", value = "Delete script(s). Supports wildcards (* ?) and regex. Usage: scripts.delete [-s <pattern>]")
    public String deleteScript(
            @ShellOption(value = { "-s", "--script" }, defaultValue = "*") String pattern)
            throws IOException {
        List<String> matchingScripts = findMatchingScripts(pattern);

        if (matchingScripts.isEmpty()) {
            return ColorPrinter.red("No scripts found matching pattern: " + pattern);
        }

        int deletedCount = 0;
        for (String scriptName : matchingScripts) {
            String scriptPath = scriptCache.get(scriptName);
            if (scriptPath != null) {
                Files.delete(Paths.get(scriptPath));
                scriptCache.remove(scriptName);
                System.out.println(ColorPrinter.gray("Deleted: " + scriptName));
                deletedCount++;
            }
        }

        return ColorPrinter.green("Deleted " + deletedCount + " script(s)");
    }

    /**
     * Find scripts matching a pattern (supports wildcards and regex)
     */
    private List<String> findMatchingScripts(String pattern) {
        String regexPattern = pattern;

        // Only convert wildcards if pattern contains * or ? but NOT regex characters
        // like []
        boolean isWildcard = (pattern.contains("*") || pattern.contains("?")) &&
                !pattern.contains("[") && !pattern.contains("]") &&
                !pattern.contains("(") && !pattern.contains(")") &&
                !pattern.contains("+") && !pattern.contains("{");

        if (isWildcard) {
            // Escape special regex characters for wildcard mode
            regexPattern = pattern.replace(".", "\\.")
                    .replace("^", "\\^")
                    .replace("$", "\\$")
                    .replace("|", "\\|");
            // Convert wildcards to regex
            regexPattern = regexPattern.replace("*", ".*").replace("?", ".");
        }
        // Otherwise treat as pure regex pattern

        final String finalPattern = regexPattern;
        return scriptCache.keySet().stream()
                .filter(name -> {
                    try {
                        return name.matches(finalPattern);
                    } catch (Exception e) {
                        // Invalid regex, return false
                        return false;
                    }
                })
                .sorted()
                .collect(Collectors.toList());
    }
}

// Made with Bob
