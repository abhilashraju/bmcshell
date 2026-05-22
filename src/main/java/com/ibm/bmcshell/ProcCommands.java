package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

/**
 * Spring Shell commands for inspecting /proc filesystem on remote BMC
 * Provides tools to inspect file descriptors, sockets, and process information
 * All commands execute on the remote BMC machine via SSH
 */
@ShellComponent
public class ProcCommands extends CommonCommands {

    public ProcCommands() throws IOException {
        super();
    }

    /**
     * List all file descriptors for a process
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.fd", "pfd" }, value = "List file descriptors for a process (eg: proc.fd 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void listFileDescriptors(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("ls -l /proc/%s/fd/", pid));
    }

    /**
     * List socket file descriptors for a process
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.fd-sockets",
            "pfds" }, value = "List socket file descriptors for a process (eg: proc.fd-sockets 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void listSocketDescriptors(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("ls -l /proc/%s/fd/ | grep socket", pid));
    }

    /**
     * Show all Unix domain sockets from /proc/net/unix
     */
    @ShellMethod(key = { "proc.unix-sockets", "punix" }, value = "Show Unix domain sockets (eg: proc.unix-sockets)")
    @ShellMethodAvailability("availabilityCheck")
    public void showUnixSockets() {
        scmd("cat /proc/net/unix");
    }

    /**
     * Show TCP sockets from /proc/net/tcp
     */
    @ShellMethod(key = { "proc.tcp-sockets", "ptcp" }, value = "Show TCP sockets (eg: proc.tcp-sockets)")
    @ShellMethodAvailability("availabilityCheck")
    public void showTcpSockets() {
        scmd("cat /proc/net/tcp");
    }

    /**
     * Show UDP sockets from /proc/net/udp
     */
    @ShellMethod(key = { "proc.udp-sockets", "pudp" }, value = "Show UDP sockets (eg: proc.udp-sockets)")
    @ShellMethodAvailability("availabilityCheck")
    public void showUdpSockets() {
        scmd("cat /proc/net/udp");
    }

    /**
     * Find all processes using a specific socket file
     * 
     * @param socketPath Path to the socket file (e.g., /var/run/console-bash.sock)
     */
    @ShellMethod(key = { "proc.find-socket-users",
            "psu" }, value = "Find processes using a socket file (eg: proc.find-socket-users /var/run/console-bash.sock)")
    @ShellMethodAvailability("availabilityCheck")
    public void findSocketUsers(
            @ShellOption(help = "Socket file path") String socketPath) {
        String command = String.format(
                "for pid in /proc/[0-9]*; do " +
                        "for fd in $pid/fd/*; do " +
                        "if readlink $fd 2>/dev/null | grep -q \"%s\"; then " +
                        "echo \"PID: $(basename $pid), FD: $(basename $fd), CMD: $(cat $pid/cmdline 2>/dev/null | tr '\\\\0' ' ')\"; "
                        +
                        "fi; " +
                        "done; " +
                        "done",
                socketPath);
        scmd(command);
    }

    /**
     * Find all processes with socket file descriptors
     */
    @ShellMethod(key = { "proc.find-all-sockets",
            "psall" }, value = "Find all processes with socket file descriptors (eg: proc.find-all-sockets)")
    @ShellMethodAvailability("availabilityCheck")
    public void findAllSocketUsers() {
        String command = "for pid in /proc/[0-9]*; do " +
                "sockets=$(ls -l $pid/fd 2>/dev/null | grep socket | wc -l); " +
                "if [ $sockets -gt 0 ]; then " +
                "echo \"PID: $(basename $pid), Sockets: $sockets, CMD: $(cat $pid/cmdline 2>/dev/null | tr '\\\\0' ' ')\"; "
                +
                "fi; " +
                "done";
        scmd(command);
    }

    /**
     * Show detailed socket information by inode
     * 
     * @param inode Socket inode number (from /proc/net/unix)
     */
    @ShellMethod(key = { "proc.find-by-inode",
            "pinode" }, value = "Find process by socket inode (eg: proc.find-by-inode 12345)")
    @ShellMethodAvailability("availabilityCheck")
    public void findByInode(
            @ShellOption(help = "Socket inode number") String inode) {
        String command = String.format(
                "for pid in /proc/[0-9]*; do " +
                        "if ls -l $pid/fd 2>/dev/null | grep -q \"socket:\\\\[%s\\\\]\"; then " +
                        "echo \"PID: $(basename $pid), CMD: $(cat $pid/cmdline 2>/dev/null | tr '\\\\0' ' ')\"; " +
                        "ls -l $pid/fd 2>/dev/null | grep \"socket:\\\\[%s\\\\]\"; " +
                        "fi; " +
                        "done",
                inode, inode);
        scmd(command);
    }

    /**
     * Show process information from /proc/<pid>/status
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.status", "pstat" }, value = "Show process status (eg: proc.status 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void showProcessStatus(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("cat /proc/%s/status", pid));
    }

    /**
     * Show process command line
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.cmdline", "pcmd" }, value = "Show process command line (eg: proc.cmdline 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void showProcessCmdline(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("cat /proc/%s/cmdline | tr '\\\\0' ' ' && echo", pid));
    }

    /**
     * Show process environment variables
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.environ", "penv" }, value = "Show process environment (eg: proc.environ 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void showProcessEnviron(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("cat /proc/%s/environ | tr '\\\\0' '\\\\n'", pid));
    }

    /**
     * Show process memory maps
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.maps", "pmaps" }, value = "Show process memory maps (eg: proc.maps 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void showProcessMaps(
            @ShellOption(help = "Process ID") String pid) {
        scmd(String.format("cat /proc/%s/maps", pid));
    }

    /**
     * Search for Unix domain socket by name pattern
     * 
     * @param pattern Pattern to search for in socket paths
     */
    @ShellMethod(key = { "proc.search-unix-socket",
            "psearch" }, value = "Search Unix sockets by name pattern (eg: proc.search-unix-socket console)")
    @ShellMethodAvailability("availabilityCheck")
    public void searchUnixSocket(
            @ShellOption(help = "Socket name pattern") String pattern) {
        scmd(String.format("grep -i \"%s\" /proc/net/unix", pattern));
    }

    /**
     * Show socket statistics using ss command (if available)
     */
    @ShellMethod(key = { "proc.ss", "pss" }, value = "Show socket statistics (ss) (eg: proc.ss)")
    @ShellMethodAvailability("availabilityCheck")
    public void showSocketStats() {
        scmd("ss -anp 2>/dev/null || echo 'ss command not available'");
    }

    /**
     * Show Unix domain socket statistics
     */
    @ShellMethod(key = { "proc.ss-unix",
            "pssunix" }, value = "Show Unix domain socket statistics (ss) (eg: proc.ss-unix)")
    @ShellMethodAvailability("availabilityCheck")
    public void showUnixSocketStats() {
        scmd("ss -xp 2>/dev/null || echo 'ss command not available'");
    }

    /**
     * Analyze socket reference count for a specific socket
     * 
     * @param socketPath Path to the socket file
     */
    @ShellMethod(key = { "proc.socket-refcount",
            "pref" }, value = "Analyze socket reference count (eg: proc.socket-refcount /var/run/console-bash.sock)")
    @ShellMethodAvailability("availabilityCheck")
    public void analyzeSocketRefcount(
            @ShellOption(help = "Socket file path") String socketPath) {
        System.out.println(ColorPrinter.cyan("=== Socket Reference Count Analysis ==="));
        System.out.println(ColorPrinter.yellow("Searching for: " + socketPath));

        // Show socket info from /proc/net/unix
        System.out.println(ColorPrinter.cyan("\n1. Socket info from /proc/net/unix:"));
        scmd(String.format("grep \"%s\" /proc/net/unix", socketPath));

        // Find all processes using this socket
        System.out.println(ColorPrinter.cyan("\n2. Processes using this socket:"));
        String command = String.format(
                "for pid in /proc/[0-9]*; do " +
                        "for fd in $pid/fd/*; do " +
                        "if readlink $fd 2>/dev/null | grep -q \"%s\"; then " +
                        "echo \"  PID: $(basename $pid), FD: $(basename $fd), CMD: $(cat $pid/cmdline 2>/dev/null | tr '\\\\0' ' ')\"; "
                        +
                        "fi; " +
                        "done; " +
                        "done",
                socketPath);
        scmd(command);

        System.out.println(ColorPrinter.cyan("\n3. Explanation:"));
        System.out.println("  - RefCount shows how many file descriptors reference this socket");
        System.out.println("  - Common causes for RefCount > 1:");
        System.out.println("    * Multiple processes connected to the socket");
        System.out.println("    * Duplicated file descriptors (dup/dup2)");
        System.out.println("    * Forked child process inheriting the socket");
        System.out.println("    * Socket passed between processes via SCM_RIGHTS");
    }

    /**
     * Show comprehensive socket inspection for a process
     * 
     * @param pid Process ID to inspect
     */
    @ShellMethod(key = { "proc.inspect-process",
            "pinspect" }, value = "Comprehensive socket inspection for a process (eg: proc.inspect-process 1234)")
    @ShellMethodAvailability("availabilityCheck")
    public void inspectProcess(
            @ShellOption(help = "Process ID") String pid) {
        System.out.println(ColorPrinter.cyan("=== Process Socket Inspection ==="));
        System.out.println(ColorPrinter.yellow("Process ID: " + pid));

        // Show command line
        System.out.println(ColorPrinter.cyan("\n1. Command Line:"));
        scmd(String.format("cat /proc/%s/cmdline 2>/dev/null | tr '\\\\0' ' ' && echo || echo 'Process not found'",
                pid));

        // Show all file descriptors
        System.out.println(ColorPrinter.cyan("\n2. All File Descriptors:"));
        scmd(String.format("ls -l /proc/%s/fd/ 2>/dev/null || echo 'Cannot access file descriptors'", pid));

        // Show only socket descriptors
        System.out.println(ColorPrinter.cyan("\n3. Socket File Descriptors:"));
        scmd(String.format("ls -l /proc/%s/fd/ 2>/dev/null | grep socket || echo 'No sockets found'", pid));

        // Show network connections
        System.out.println(ColorPrinter.cyan("\n4. Network Connections (if ss available):"));
        scmd(String.format("ss -anp 2>/dev/null | grep 'pid=%s' || echo 'ss not available or no connections'", pid));
    }

    /**
     * Show help for proc commands
     */
    @ShellMethod(key = { "proc.help", "phelp" }, value = "Show help for proc commands")
    @ShellMethodAvailability("availabilityCheck")
    public void procHelp() {
        System.out.println(ColorPrinter.cyan("=== Proc Commands Help ===\n"));
        System.out.println(ColorPrinter.yellow("File Descriptor Commands:"));
        System.out.println("  proc.fd <pid>              - List all file descriptors for a process");
        System.out.println("  proc.fd-sockets <pid>      - List only socket file descriptors");
        System.out.println("");
        System.out.println(ColorPrinter.yellow("Socket Information Commands:"));
        System.out.println("  proc.unix-sockets          - Show all Unix domain sockets");
        System.out.println("  proc.tcp-sockets           - Show all TCP sockets");
        System.out.println("  proc.udp-sockets           - Show all UDP sockets");
        System.out.println("  proc.ss                    - Show socket statistics (ss command)");
        System.out.println("  proc.ss-unix               - Show Unix socket statistics");
        System.out.println("");
        System.out.println(ColorPrinter.yellow("Socket Analysis Commands:"));
        System.out.println("  proc.find-socket-users <path>  - Find processes using a socket file");
        System.out.println("  proc.find-all-sockets          - Find all processes with sockets");
        System.out.println("  proc.find-by-inode <inode>     - Find process by socket inode");
        System.out.println("  proc.search-unix-socket <pat>  - Search Unix sockets by pattern");
        System.out.println("  proc.socket-refcount <path>    - Analyze socket reference count");
        System.out.println("");
        System.out.println(ColorPrinter.yellow("Process Information Commands:"));
        System.out.println("  proc.status <pid>          - Show process status");
        System.out.println("  proc.cmdline <pid>         - Show process command line");
        System.out.println("  proc.environ <pid>         - Show process environment");
        System.out.println("  proc.maps <pid>            - Show process memory maps");
        System.out.println("  proc.inspect-process <pid> - Comprehensive process inspection");
        System.out.println("");
        System.out.println(ColorPrinter.yellow("Short Aliases:"));
        System.out.println("  pfd, pfds, punix, ptcp, pudp, psu, psall, pinode,");
        System.out.println("  pstat, pcmd, penv, pmaps, psearch, pss, pssunix,");
        System.out.println("  pref, pinspect, phelp");
        System.out.println("");
        System.out.println(ColorPrinter.green("Example Usage:"));
        System.out.println("  proc.socket-refcount /var/run/console-bash.sock");
        System.out.println("  proc.find-socket-users /var/run/console-bash.sock");
        System.out.println("  proc.inspect-process 1234");
        System.out.println("  proc.search-unix-socket console");
    }
}
