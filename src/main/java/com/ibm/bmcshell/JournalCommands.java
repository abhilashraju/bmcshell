package com.ibm.bmcshell;

import java.io.IOException;
import java.util.stream.Stream;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class JournalCommands extends CommonCommands {
    protected Thread journalThread;

    protected JournalCommands() throws IOException {
    }

    @ShellMethod(key = "journal.start", value = "eg: journal.start arg ")
    void journalctl(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] filter) throws IOException {
        journalctlStop(); // Stop any existing journal thread
        StringBuffer command = new StringBuffer();
        command.append("journalctl -f");
        Stream.of(filter).map(f -> {
            return ServiceCommands.ServiceProvider.serviceNames.stream().filter(s -> s.contains(f));
        }).flatMap(s -> s).forEach(s -> {
            command.append(" -u ").append(s);
        });
        Thread journalThread = new Thread(() -> scmd(command.toString()));
        journalThread.setName("JournalCtlThread");
        journalThread.start();

        // Store the thread reference for control commands
        this.journalThread = journalThread;
        return;

    }

    @ShellMethod(key = "journal.search", value = "eg: journal.search arg1 arg2 arg3")
    void journalSearch(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms) throws IOException {
        journalSearchN(searchTerms, 100);
    }

    @ShellMethod(key = "journal.searchN", value = "eg: journal.searchN arg1 arg2 arg3 -n 50")
    void journalSearchN(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms,
            @ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        journalctlStop(); // Stop any existing journal thread

        if (searchTerms[0].equals("*")) {
            scmd(String.format("journalctl | tail -n %d", n));
            return;
        }

        // Build grep command with OR pattern (matches any of the search terms)
        String pattern = String.join("|", searchTerms);
        scmd(String.format("journalctl | grep -E '%s' | tail -n %d", pattern, n));
    }

    @ShellMethod(key = "journal.stop", value = "eg: journal.stop")
    void journalctlStop() {
        if (journalThread != null && journalThread.isAlive()) {
            journalThread.interrupt();
            System.out.println("JournalCtl thread stopped.");
        }
    }

    @ShellMethod(key = "journal.clear", value = "eg: journal.clear")
    void journalctlClear() {
        scmd("journalctl --rotate; journalctl --vacuum-time=1s");
    }

    @ShellMethod(key = "journal.help", value = "Show journal commands help. eg: journal.help")
    void journalHelp() {
        System.out.println("\n=== Journal Log Commands ===\n");
        System.out.println("journal.start <filter>    - Start following journal logs for services matching filter");
        System.out.println("                            Filters services by name (e.g., 'bmcweb', 'network')");
        System.out.println("journal.stop              - Stop journal log monitoring");
        System.out.println("\njournal.search <terms>    - Search journal logs (default: last 100 lines)");
        System.out.println("                            Use '*' to show all logs");
        System.out.println("journal.searchN <terms> -n <num> - Search with custom limit");
        System.out.println("\njournal.clear             - Clear journal logs (rotate and vacuum)");
        System.out.println("\njournal.help              - Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  journal.start bmcweb");
        System.out.println("  journal.start network cert");
        System.out.println("  journal.search error warning");
        System.out.println("  journal.searchN certificate -n 50");
        System.out.println("  journal.search '*'");
        System.out.println("  journal.clear");
        System.out.println("  journal.stop\n");
    }
}
