package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class KernelCommands extends CommonCommands {
    protected Thread kernelThread;

    protected KernelCommands() throws IOException {
    }

    @ShellMethod(key = "kernel.start", value = "Start following kernel logs in real-time. eg: kernel.start")
    void kernelStart() throws IOException {
        kernelStop(); // Stop any existing kernel thread

        Thread kernelThread = new Thread(() -> scmd("journalctl -k -f"));
        kernelThread.setName("KernelLogThread");
        kernelThread.start();

        // Store the thread reference for control commands
        this.kernelThread = kernelThread;
        System.out.println("Kernel log monitoring started. Use 'kernel.stop' to stop.");
    }

    @ShellMethod(key = "kernel.dmesg", value = "Start following kernel logs using dmesg. eg: kernel.dmesg")
    void kernelDmesg() throws IOException {
        kernelStop(); // Stop any existing kernel thread

        Thread kernelThread = new Thread(() -> scmd("dmesg -w"));
        kernelThread.setName("KernelDmesgThread");
        kernelThread.start();

        // Store the thread reference for control commands
        this.kernelThread = kernelThread;
        System.out.println("Kernel dmesg monitoring started. Use 'kernel.stop' to stop.");
    }

    @ShellMethod(key = "kernel.search", value = "Search kernel logs. eg: kernel.search error warning")
    void kernelSearch(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms) throws IOException {
        kernelSearchN(searchTerms, 100);
    }

    @ShellMethod(key = "kernel.searchN", value = "Search kernel logs with limit. eg: kernel.searchN error warning -n 50")
    void kernelSearchN(@ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] searchTerms,
            @ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop(); // Stop any existing kernel thread

        if (searchTerms[0].equals("*")) {
            scmd(String.format("journalctl -k | tail -n %d", n));
            return;
        }

        // Build grep command with OR pattern (matches any of the search terms)
        String pattern = String.join("|", searchTerms);
        scmd(String.format("journalctl -k | grep -E '%s' | tail -n %d", pattern, n));
    }

    @ShellMethod(key = "kernel.boot", value = "Show kernel logs from current boot. eg: kernel.boot")
    void kernelBoot() throws IOException {
        kernelStop();
        scmd("journalctl -k -b");
    }

    @ShellMethod(key = "kernel.bootN", value = "Show kernel logs from current boot with limit. eg: kernel.bootN -n 50")
    void kernelBootN(@ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop();
        scmd(String.format("journalctl -k -b | tail -n %d", n));
    }

    @ShellMethod(key = "kernel.errors", value = "Show kernel error messages. eg: kernel.errors")
    void kernelErrors() throws IOException {
        kernelErrorsN(100);
    }

    @ShellMethod(key = "kernel.errorsN", value = "Show kernel error messages with limit. eg: kernel.errorsN -n 50")
    void kernelErrorsN(@ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop();
        scmd(String.format("journalctl -k -p err | tail -n %d", n));
    }

    @ShellMethod(key = "kernel.warnings", value = "Show kernel warnings and errors. eg: kernel.warnings")
    void kernelWarnings() throws IOException {
        kernelWarningsN(100);
    }

    @ShellMethod(key = "kernel.warningsN", value = "Show kernel warnings and errors with limit. eg: kernel.warningsN -n 50")
    void kernelWarningsN(@ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop();
        scmd(String.format("journalctl -k -p warning | tail -n %d", n));
    }

    @ShellMethod(key = "kernel.since", value = "Show kernel logs since time. eg: kernel.since '1 hour ago'")
    void kernelSince(@ShellOption String timeSpec) throws IOException {
        kernelStop();
        scmd(String.format("journalctl -k --since '%s'", timeSpec));
    }

    @ShellMethod(key = "kernel.sinceN", value = "Show kernel logs since time with limit. eg: kernel.sinceN '1 hour ago' -n 50")
    void kernelSinceN(@ShellOption String timeSpec,
            @ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop();
        scmd(String.format("journalctl -k --since '%s' | tail -n %d", timeSpec, n));
    }

    @ShellMethod(key = "kernel.level", value = "Show current kernel log level. eg: kernel.level")
    void kernelLevel() throws IOException {
        kernelStop();
        scmd("cat /proc/sys/kernel/printk");
        System.out.println(
                "\nFormat: console_loglevel default_message_loglevel minimum_console_loglevel default_console_loglevel");
        System.out.println("Log levels: 0=emerg 1=alert 2=crit 3=err 4=warn 5=notice 6=info 7=debug");
    }

    @ShellMethod(key = "kernel.setLevel", value = "Set kernel console log level (0-7). eg: kernel.setLevel 7")
    void kernelSetLevel(@ShellOption int level) throws IOException {
        if (level < 0 || level > 7) {
            System.out.println("Error: Log level must be between 0 and 7");
            System.out.println("0=emerg 1=alert 2=crit 3=err 4=warn 5=notice 6=info 7=debug");
            return;
        }
        kernelStop();
        scmd(String.format("echo %d > /proc/sys/kernel/printk", level));
        System.out.println("Kernel console log level set to: " + level);
    }

    @ShellMethod(key = "kernel.ring", value = "Show kernel ring buffer (dmesg). eg: kernel.ring")
    void kernelRing() throws IOException {
        kernelStop();
        scmd("dmesg");
    }

    @ShellMethod(key = "kernel.ringN", value = "Show kernel ring buffer with limit. eg: kernel.ringN -n 50")
    void kernelRingN(@ShellOption(value = { "-n" }, defaultValue = "100") int n) throws IOException {
        kernelStop();
        scmd(String.format("dmesg | tail -n %d", n));
    }

    @ShellMethod(key = "kernel.ringClear", value = "Clear kernel ring buffer. eg: kernel.ringClear")
    void kernelRingClear() throws IOException {
        kernelStop();
        scmd("dmesg -C");
        System.out.println("Kernel ring buffer cleared.");
    }

    @ShellMethod(key = "kernel.stop", value = "Stop kernel log monitoring. eg: kernel.stop")
    void kernelStop() {
        if (kernelThread != null && kernelThread.isAlive()) {
            kernelThread.interrupt();
            System.out.println("Kernel log monitoring stopped.");
        }
    }

    @ShellMethod(key = "kernel.help", value = "Show kernel commands help. eg: kernel.help")
    void kernelHelp() {
        System.out.println("\n=== Kernel Log Commands ===\n");
        System.out.println("kernel.start          - Start following kernel logs in real-time");
        System.out.println("kernel.dmesg          - Start following kernel logs using dmesg");
        System.out.println("kernel.stop           - Stop kernel log monitoring");
        System.out.println("\nkernel.search <terms> - Search kernel logs (default: last 100 lines)");
        System.out.println("kernel.searchN <terms> -n <num> - Search with custom limit");
        System.out.println("\nkernel.boot           - Show kernel logs from current boot");
        System.out.println("kernel.bootN -n <num> - Show boot logs with limit");
        System.out.println("\nkernel.errors         - Show kernel error messages");
        System.out.println("kernel.errorsN -n <num> - Show errors with limit");
        System.out.println("kernel.warnings       - Show kernel warnings and errors");
        System.out.println("kernel.warningsN -n <num> - Show warnings with limit");
        System.out.println("\nkernel.since '<time>' - Show logs since time (e.g., '1 hour ago', 'today')");
        System.out.println("kernel.sinceN '<time>' -n <num> - Show logs since time with limit");
        System.out.println("\nkernel.level          - Show current kernel log level");
        System.out.println("kernel.setLevel <0-7> - Set kernel console log level");
        System.out.println("                        0=emerg 1=alert 2=crit 3=err 4=warn 5=notice 6=info 7=debug");
        System.out.println("\nkernel.ring           - Show kernel ring buffer (dmesg)");
        System.out.println("kernel.ringN -n <num> - Show ring buffer with limit");
        System.out.println("kernel.ringClear      - Clear kernel ring buffer");
        System.out.println("\nkernel.help           - Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  kernel.start");
        System.out.println("  kernel.search error warning");
        System.out.println("  kernel.searchN usb network -n 50");
        System.out.println("  kernel.errors");
        System.out.println("  kernel.since '1 hour ago'");
        System.out.println("  kernel.setLevel 7");
        System.out.println("  kernel.stop\n");
    }
}

// Made with Bob
