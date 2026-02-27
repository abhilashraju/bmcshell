package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class RemoteCommands extends CommonCommands {
    
    // Monitoring state
    private String monitoringService = null;
    private String monitorLogFile = null;
    private String monitorPidFile = null;
    
    // Memory monitoring state
    private Thread memMonitorThread = null;
    private volatile boolean memMonitorRunning = false;
    private String memMonitoringService = null;
    private List<MemStatData> memDataPoints = null;
    private String memMonitorPid = null;

    protected RemoteCommands() throws IOException {

    }

    @ShellMethod(key = "ro.ls", value = "eg: ro.ls path")
    @ShellMethodAvailability("availabilityCheck")
    void ls(@ShellOption(valueProvider = RemoteFileCompleter.class) String path) {
        scmd(String.format("ls -alhS %s", path));
    }

    @ShellMethod(key = "ro.mv", value = "eg: ro.mv source dest")
    @ShellMethodAvailability("availabilityCheck")
    void mv(@ShellOption(valueProvider = RemoteFileCompleter.class) String source,
            @ShellOption(valueProvider = RemoteFileCompleter.class) String dest) {
        scmd(String.format("mv %s %s", source, dest));
    }

    @ShellMethod(key = "ro.cmd", value = "eg: ro.cmd command")
    @ShellMethodAvailability("availabilityCheck")
    void cmd(String cmd) {
        scmd(cmd);
    }

    @ShellMethod(key = "ro.date.get", value = "Get current Unix timestamp in UTC (eg: ro.date.get)")
    @ShellMethodAvailability("availabilityCheck")
    void dateGet() {
        scmd("date -u +%s && date -u");
    }

    @ShellMethod(key = "ro.date.set", value = "Set date using Unix timestamp (eg: ro.date.set 1771405014)")
    @ShellMethodAvailability("availabilityCheck")
    void dateSet(String timestamp) {
        scmd(String.format("date -s @%s && date", timestamp));
    }

    @ShellMethod(key = "ro.cat", value = "eg: ro.cat filepath")
    @ShellMethodAvailability("availabilityCheck")
    void cat(@ShellOption(valueProvider = RemoteFileCompleter.class) String p) {
        scmd(String.format("cat %s", p));
    }


    @ShellMethod(key = "ro.find", value = "eg: ro.find filename [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void findFile(String filename,
            @ShellOption(value = { "--path", "-p" }, valueProvider = RemoteFileCompleter.class, defaultValue = "/") String path) {
        scmd(String.format("find %s -iname *%s*", path, filename));
    }

    @ShellMethod(key = "ro.grep", value = "eg: ro.grep pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void grep(String pattern,
            @ShellOption(value = { "--path", "-p" }, valueProvider = RemoteFileCompleter.class, defaultValue = "/") String path) {
        scmd(String.format("grep -nr %s %s", pattern, path));
    }

    @ShellMethod(key = "ro.running", value = "eg: ro.running pattern [<path>]")
    @ShellMethodAvailability("availabilityCheck")
    void running(String pattern,
            @ShellOption(value = { "--path", "-p" }, defaultValue = "/") String path) {
        scmd(String.format("ps | grep %s", pattern));
    }

    @ShellMethod(key = "ro.makefile", value = "eg: ro.makefile path content")
    @ShellMethodAvailability("availabilityCheck")
    void makeFile(@ShellOption(valueProvider = RemoteFileCompleter.class) String path, String data) {
        scmd(String.format("mkdir -p %s", path.substring(0, path.lastIndexOf('/'))));
        scmd(String.format("chmod 777 %s;echo %s > %s", path, data, path));
        scmd(String.format("ls -lh %s", path));
    }

    @ShellMethod(key = "ro.digest", value = "eg: ro.digest path to file")
    @ShellMethodAvailability("availabilityCheck")
    void digest(@ShellOption(valueProvider = RemoteFileCompleter.class) String path) {
        scmd(String.format("openssl dgst -sha256 %s", path));
    }

    @ShellMethod(key = "ro.ping", value = "eg: ro.ping ip")
    @ShellMethodAvailability("availabilityCheck")
    void ping(String ip) {
        scmd(String.format("ping -c 1 %s", ip));
    }
    @ShellMethod(key = "ro.mem.stat", value = "eg: ro.mem.stat servicename [--exe exename] [--interval 2] - Start live memory monitoring")
    @ShellMethodAvailability("availabilityCheck")
    void mem_stat(
            @ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceCommands.ServiceProvider.class, defaultValue = ShellOption.NULL) String servicename,
            @ShellOption(value = { "--exe", "-e" }, defaultValue = ShellOption.NULL) String exename,
            @ShellOption(value = { "--interval", "-i" }, defaultValue = "2") int interval) {
        
        if (memMonitorRunning) {
            System.out.println(ColorPrinter.yellow("Memory monitoring already running for service: " + memMonitoringService));
            System.out.println("Stop current monitoring with ro.mem.stat.stop before starting a new one");
            return;
        }
        
        // Validate input: either servicename or exename must be provided
        if (servicename == null && exename == null) {
            System.out.println(ColorPrinter.red("Error: Either --ser (service name) or --exe (executable name) must be provided"));
            System.out.println("Usage: ro.mem.stat --ser servicename [--interval 2]");
            System.out.println("   or: ro.mem.stat --exe exename [--interval 2]");
            return;
        }
        
        try {
            // Get connection details
            String machine = CommonCommands.machine;
            String userName = CommonCommands.getUserName();
            String passwd = CommonCommands.getPasswd();
            
            String processname;
            String displayName;
            
            // Determine process name based on input
            if (exename != null) {
                // Use exe name directly
                processname = exename;
                displayName = exename;
                System.out.println(ColorPrinter.cyan("Using executable name: " + exename));
            } else {
                // Use service name - get process name from service
                displayName = servicename;
                
                // Step 1: Deploy get_process_name.sh script if not already present
                String remoteGetProcessNameScript = "/tmp/get_process_name.sh";
                try {
                    String getProcessNameScriptContent = new String(
                        getClass().getResourceAsStream("/get_process_name.sh").readAllBytes()
                    );
                    
                    ByteArrayOutputStream deployStream = new ByteArrayOutputStream();
                    String deployCmd = String.format(
                        "cat > %s << 'EOFSCRIPT'\n%s\nEOFSCRIPT\nchmod +x %s",
                        remoteGetProcessNameScript, getProcessNameScriptContent, remoteGetProcessNameScript
                    );
                    runCommandShort(deployStream, Util.fullMachineName(machine), userName, passwd, deployCmd);
                } catch (Exception e) {
                    System.out.println(ColorPrinter.yellow("Warning: Could not deploy get_process_name.sh: " + e.getMessage()));
                }
                
                // Step 2: Get the actual process name from the service
                ByteArrayOutputStream processNameOutput = new ByteArrayOutputStream();
                String getProcessNameCmd = String.format("%s '%s'", remoteGetProcessNameScript, servicename);
                runCommandShort(processNameOutput, Util.fullMachineName(machine), userName, passwd, getProcessNameCmd);
                
                processname = processNameOutput.toString().trim();
                if (processname.isEmpty()) {
                    System.out.println(ColorPrinter.red("Error: Could not determine process name for service '" + servicename + "'"));
                    return;
                }
            }
            
            // Step 3: Find the process ID
            ByteArrayOutputStream pidOutput = new ByteArrayOutputStream();
            String pidCommand = String.format("pgrep -x '%s' | head -n 1", processname);
            runCommandShort(pidOutput, Util.fullMachineName(machine), userName, passwd, pidCommand);
            
            String pid = pidOutput.toString().trim();
            if (pid.isEmpty()) {
                // Try alternative method using systemctl only if servicename was provided
                if (servicename != null) {
                    ByteArrayOutputStream mainPidOutput = new ByteArrayOutputStream();
                    String mainPidCmd = String.format("systemctl show '%s' -p MainPID --value", servicename);
                    runCommandShort(mainPidOutput, Util.fullMachineName(machine), userName, passwd, mainPidCmd);
                    pid = mainPidOutput.toString().trim();
                }
                
                if (pid.isEmpty() || pid.equals("0")) {
                    System.out.println(ColorPrinter.red("Error: Process '" + processname + "' not found" +
                        (servicename != null ? " or service '" + servicename + "' is not running" : "")));
                    return;
                }
            }
            
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Live Memory Monitoring"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            if (servicename != null) {
                System.out.println(ColorPrinter.yellow("Service: ") + servicename);
            }
            System.out.println(ColorPrinter.yellow("Process: ") + processname);
            System.out.println(ColorPrinter.yellow("PID: ") + pid);
            System.out.println(ColorPrinter.yellow("Interval: ") + interval + " seconds");
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
            // Initialize monitoring state
            memMonitoringService = displayName;
            memMonitorPid = pid;
            memDataPoints = new ArrayList<>();
            memMonitorRunning = true;
            
            final String finalPid = pid;
            final int finalInterval = interval;
            
            // Start monitoring in a separate thread
            memMonitorThread = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                int count = 0;
                
                System.out.println(ColorPrinter.green("\n✓ Monitoring started in background"));
                System.out.println(ColorPrinter.yellow("Use 'ro.mem.stat.stop' to stop monitoring and view results\n"));
                
                while (memMonitorRunning) {
                    try {
                        // Read /proc/<pid>/statm
                        ByteArrayOutputStream statmOutput = new ByteArrayOutputStream();
                        String statmCommand = String.format("cat /proc/%s/statm 2>/dev/null || echo 'ERROR'", finalPid);
                        runCommandShort(statmOutput, Util.fullMachineName(machine), userName, passwd, statmCommand);
                        
                        String statmData = statmOutput.toString().trim();
                        if (statmData.isEmpty() || statmData.equals("ERROR")) {
                            System.out.println(ColorPrinter.red("\n✗ Process terminated or not accessible"));
                            memMonitorRunning = false;
                            break;
                        }
                        
                        // Parse statm values
                        String[] values = statmData.split("\\s+");
                        if (values.length >= 2) {
                            long totalPages = Long.parseLong(values[0]);
                            long residentPages = Long.parseLong(values[1]);
                            long sharedPages = values.length > 2 ? Long.parseLong(values[2]) : 0;
                            long textPages = values.length > 3 ? Long.parseLong(values[3]) : 0;
                            long dataPages = values.length > 5 ? Long.parseLong(values[5]) : 0;
                            
                            long totalBytes = totalPages * 4096;
                            long residentBytes = residentPages * 4096;
                            long sharedBytes = sharedPages * 4096;
                            
                            MemStatData data = new MemStatData(
                                System.currentTimeMillis() - startTime,
                                totalBytes,
                                residentBytes,
                                sharedBytes,
                                textPages,
                                dataPages
                            );
                            
                            synchronized (memDataPoints) {
                                memDataPoints.add(data);
                            }
                            
                            count++;
                            
                            // Display current reading every 10 samples
                            if (count % 10 == 0) {
                                System.out.printf("\r[%d samples] Total: %8s | Resident: %8s | Shared: %8s",
                                    count,
                                    formatBytes(totalBytes),
                                    formatBytes(residentBytes),
                                    formatBytes(sharedBytes));
                                System.out.flush();
                            }
                        }
                        
                        // Sleep for interval
                        Thread.sleep(finalInterval * 1000);
                        
                    } catch (InterruptedException e) {
                        memMonitorRunning = false;
                        break;
                    } catch (Exception e) {
                        System.out.println(ColorPrinter.red("\nError reading memory stats: " + e.getMessage()));
                        memMonitorRunning = false;
                        break;
                    }
                }
            });
            
            memMonitorThread.setDaemon(true);
            memMonitorThread.start();
            
        } catch (Exception e) {
            System.out.println(ColorPrinter.red("\nError: " + e.getMessage()));
            e.printStackTrace();
            memMonitorRunning = false;
            memMonitoringService = null;
            memDataPoints = null;
        }
    }
    
    @ShellMethod(key = "ro.mem.stat.stop", value = "Stop memory monitoring and display results")
    @ShellMethodAvailability("availabilityCheck")
    void mem_stat_stop() {
        if (!memMonitorRunning) {
            System.out.println(ColorPrinter.yellow("No memory monitoring is currently running"));
            return;
        }
        
        System.out.println(ColorPrinter.cyan("\nStopping memory monitoring..."));
        memMonitorRunning = false;
        
        // Wait for thread to finish
        if (memMonitorThread != null) {
            try {
                memMonitorThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                System.out.println(ColorPrinter.yellow("Warning: Thread join interrupted"));
            }
        }
        
        // Display results
        if (memDataPoints == null || memDataPoints.isEmpty()) {
            System.out.println(ColorPrinter.red("\nNo data collected"));
            memMonitoringService = null;
            memDataPoints = null;
            return;
        }
        
        List<MemStatData> dataPoints;
        synchronized (memDataPoints) {
            dataPoints = new ArrayList<>(memDataPoints);
        }
        
        System.out.println("\n\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Memory Statistics Summary"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        
        // Calculate statistics
        double avgTotal = dataPoints.stream().mapToLong(d -> d.totalBytes).average().orElse(0);
        long maxTotal = dataPoints.stream().mapToLong(d -> d.totalBytes).max().orElse(0);
        long minTotal = dataPoints.stream().mapToLong(d -> d.totalBytes).min().orElse(0);
        
        double avgResident = dataPoints.stream().mapToLong(d -> d.residentBytes).average().orElse(0);
        long maxResident = dataPoints.stream().mapToLong(d -> d.residentBytes).max().orElse(0);
        long minResident = dataPoints.stream().mapToLong(d -> d.residentBytes).min().orElse(0);
        
        double avgShared = dataPoints.stream().mapToLong(d -> d.sharedBytes).average().orElse(0);
        long maxShared = dataPoints.stream().mapToLong(d -> d.sharedBytes).max().orElse(0);
        
        System.out.println(ColorPrinter.yellow("Service: ") + memMonitoringService);
        System.out.println(ColorPrinter.yellow("PID: ") + memMonitorPid);
        System.out.println(ColorPrinter.yellow("Data Points Collected: ") + dataPoints.size());
        System.out.println(ColorPrinter.yellow("Monitoring Duration: ") +
            String.format("%.1f seconds", (dataPoints.get(dataPoints.size() - 1).timestamp) / 1000.0));
        System.out.println();
        
        System.out.println(ColorPrinter.green("Total Virtual Memory (VSZ):"));
        System.out.println("  Average: " + formatBytes((long)avgTotal));
        System.out.println("  Maximum: " + formatBytes(maxTotal));
        System.out.println("  Minimum: " + formatBytes(minTotal));
        System.out.println();
        
        System.out.println(ColorPrinter.green("Resident Set Size (RSS):"));
        System.out.println("  Average: " + formatBytes((long)avgResident));
        System.out.println("  Maximum: " + formatBytes(maxResident));
        System.out.println("  Minimum: " + formatBytes(minResident));
        System.out.println();
        
        System.out.println(ColorPrinter.green("Shared Memory:"));
        System.out.println("  Average: " + formatBytes((long)avgShared));
        System.out.println("  Maximum: " + formatBytes(maxShared));
        
        // Display graphs for Total and Resident memory
        System.out.println("\n" + ColorPrinter.cyan("Memory Usage Over Time:"));
        displayChart(
            dataPoints.stream().mapToDouble(d -> d.totalBytes / (1024.0 * 1024.0)).toArray(),
            "Total (VSZ) - MB", 20, 60
        );
        displayChart(
            dataPoints.stream().mapToDouble(d -> d.residentBytes / (1024.0 * 1024.0)).toArray(),
            "Resident (RSS) - MB", 20, 60
        );
        
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        
        // Clean up
        memMonitoringService = null;
        memDataPoints = null;
        memMonitorPid = null;
        memMonitorThread = null;
    }
    
    @ShellMethod(key = "ro.mem.stat.status", value = "Check memory monitoring status")
    @ShellMethodAvailability("availabilityCheck")
    void mem_stat_status() {
        if (!memMonitorRunning) {
            System.out.println(ColorPrinter.yellow("No memory monitoring is currently running"));
            return;
        }
        
        int dataCount = 0;
        if (memDataPoints != null) {
            synchronized (memDataPoints) {
                dataCount = memDataPoints.size();
            }
        }
        
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Memory Monitoring Status"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.green("✓ Monitoring is active"));
        System.out.println(ColorPrinter.yellow("Service: ") + memMonitoringService);
        System.out.println(ColorPrinter.yellow("PID: ") + memMonitorPid);
        System.out.println(ColorPrinter.yellow("Data points collected: ") + dataCount);
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println("\nUse 'ro.mem.stat.stop' to stop monitoring and view results");
    }



    @ShellMethod(key = "ro.monitor.start", value = "eg: ro.monitor.start servicename [--interval 5]")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_start(
            @ShellOption(value = { "--ser", "-s" }, valueProvider = ServiceCommands.ServiceProvider.class, defaultValue = "") String s,
            @ShellOption(value = { "--interval", "-i" }, defaultValue = "5") int interval) {
        if (s == null || s.isEmpty()) {
            System.out.println(ColorPrinter.red("Please specify a service name"));
            return;
        }
        
        if (monitoringService != null) {
            System.out.println(ColorPrinter.yellow("Already monitoring service: " + monitoringService));
            System.out.println("Stop current monitoring with ro.monitor.stop before starting a new one");
            return;
        }
        
        // Sanitize service name for file paths (replace special chars with underscores)
        String sanitizedName = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        String tempPidFile = "/tmp/monitor_" + sanitizedName + ".pid";
        
        // Clean up any existing monitoring process for this service
        try {
            System.out.println(ColorPrinter.cyan("Checking for existing monitoring processes..."));
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            String cleanupCmd = String.format(
                "# Kill all service_monitor scripts for this service\n" +
                "PIDS=$(ps | grep 'service_monitor.*%s' | grep -v grep | awk '{print $1}');\n" +
                "if [ -n \"$PIDS\" ]; then\n" +
                "  echo \"Stopping $(echo $PIDS | wc -w) old monitoring process(es)\";\n" +
                "  for pid in $PIDS; do kill $pid 2>/dev/null; done;\n" +
                "  sleep 1;\n" +
                "fi;\n" +
                "# Clean up files\n" +
                "rm -f %s /tmp/service_monitor_*.sh /tmp/get_process_name.sh /tmp/monitor_%s_*.csv /tmp/monitor_debug_*.log 2>/dev/null;\n" +
                "echo 'Cleanup complete'",
                sanitizedName, tempPidFile, sanitizedName
            );
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
            String cleanupResult = cleanupStream.toString().trim();
            if (cleanupResult.contains("Stopping")) {
                System.out.println(ColorPrinter.yellow("  " + cleanupResult));
            }
        } catch (Exception e) {
            System.out.println(ColorPrinter.yellow("  Warning: Cleanup check failed: " + e.getMessage()));
        }
        
        monitoringService = s;
        monitorLogFile = "/tmp/monitor_" + sanitizedName + "_" + System.currentTimeMillis() + ".csv";
        monitorPidFile = tempPidFile;
        
        try {
            // Read both scripts from resources
            String monitorScriptContent = new String(
                getClass().getResourceAsStream("/service_monitor.sh").readAllBytes()
            );
            String getProcessNameScriptContent = new String(
                getClass().getResourceAsStream("/get_process_name.sh").readAllBytes()
            );
            
            // Upload scripts to remote machine
            String remoteMonitorScriptPath = "/tmp/service_monitor_" + System.currentTimeMillis() + ".sh";
            String remoteGetProcessNameScriptPath = "/tmp/get_process_name.sh";
            ByteArrayOutputStream uploadStream = new ByteArrayOutputStream();
            
            // Create both scripts on remote machine
            String createScriptsCmd = String.format(
                "cat > %s << 'EOFSCRIPT1'\n%s\nEOFSCRIPT1\n" +
                "chmod +x %s\n" +
                "cat > %s << 'EOFSCRIPT2'\n%s\nEOFSCRIPT2\n" +
                "chmod +x %s",
                remoteGetProcessNameScriptPath, getProcessNameScriptContent, remoteGetProcessNameScriptPath,
                remoteMonitorScriptPath, monitorScriptContent, remoteMonitorScriptPath
            );
            
            runCommandShort(uploadStream, Util.fullMachineName(machine), userName, passwd, createScriptsCmd);
            
            // Start monitoring in background - pass sanitized name for PID file and get_process_name script path
            String startCmd = String.format(
                "nohup %s '%s' '%s' %d '%s' '%s' > /dev/null 2>&1 &",
                remoteMonitorScriptPath, s, monitorLogFile, interval, monitorPidFile, remoteGetProcessNameScriptPath
            );
            
            ByteArrayOutputStream startStream = new ByteArrayOutputStream();
            runCommandShort(startStream, Util.fullMachineName(machine), userName, passwd, startCmd);
            
            System.out.println(ColorPrinter.cyan("Starting monitoring process..."));
            
            // Wait for the background process to initialize and write first entry
            Thread.sleep(3000);
            
            // Verify the monitoring process started
            ByteArrayOutputStream verifyStream = new ByteArrayOutputStream();
            String verifyCmd = String.format(
                "if [ -f %s ]; then " +
                "  MONITOR_PID=$(cat %s); " +
                "  if kill -0 $MONITOR_PID 2>/dev/null; then " +
                "    echo 'RUNNING'; " +
                "  else " +
                "    echo 'PID_FILE_EXISTS_BUT_PROCESS_DEAD'; " +
                "  fi; " +
                "else " +
                "  echo 'PID_FILE_NOT_FOUND'; " +
                "fi",
                monitorPidFile, monitorPidFile
            );
            
            runCommandShort(verifyStream, Util.fullMachineName(machine), userName, passwd, verifyCmd);
            
            String verifyResult = verifyStream.toString().trim();
            
            if (verifyResult.contains("RUNNING")) {
                // Also verify log file is being created and check if process is actually running
                ByteArrayOutputStream logCheckStream = new ByteArrayOutputStream();
                runCommandShort(logCheckStream, Util.fullMachineName(machine), userName, passwd,
                    "test -f " + monitorLogFile + " && (wc -l " + monitorLogFile + "; tail -2 " + monitorLogFile + ") || echo '0'");
                
                String logStatus = logCheckStream.toString();
                int lineCount = 0;
                try {
                    String firstLine = logStatus.split("\n")[0].trim();
                    lineCount = Integer.parseInt(firstLine.split("\\s+")[0]);
                } catch (Exception e) {
                    // Ignore parsing errors
                }
                
                boolean processNotRunning = logStatus.contains(",N/A,0,0,0,0,0,stopped") ||
                                           logStatus.contains(",N/A,0,0,0,0,0,not_running");
                
                // Check if log file has at least 2 lines (header + data)
                if (lineCount < 2) {
                    System.out.println(ColorPrinter.yellow("⚠ Monitoring process started but log file not yet populated"));
                    System.out.println(ColorPrinter.yellow("  Waiting for first data entry..."));
                    Thread.sleep(interval * 1000 + 1000); // Wait one interval plus buffer
                    
                    // Re-check
                    ByteArrayOutputStream recheckStream = new ByteArrayOutputStream();
                    runCommandShort(recheckStream, Util.fullMachineName(machine), userName, passwd,
                        "test -f " + monitorLogFile + " && wc -l " + monitorLogFile + " || echo '0'");
                    String recheckResult = recheckStream.toString().trim();
                    try {
                        lineCount = Integer.parseInt(recheckResult.split("\\s+")[0]);
                    } catch (Exception e) {
                        lineCount = 0;
                    }
                }
                
                System.out.println(ColorPrinter.green("✓ Monitoring started successfully for service: " + s));
                System.out.println(ColorPrinter.cyan("  Interval: " + interval + " seconds"));
                System.out.println(ColorPrinter.cyan("  Log file: " + monitorLogFile));
                System.out.println(ColorPrinter.cyan("  PID file: " + monitorPidFile));
                System.out.println(ColorPrinter.cyan("  Data entries: " + Math.max(0, lineCount - 1)));
                
                if (processNotRunning) {
                    System.out.println(ColorPrinter.yellow("  ⚠ Warning: Target service '" + s + "' is not currently running"));
                    System.out.println(ColorPrinter.yellow("     Monitoring will show 'stopped' or 'not_running' until the service starts"));
                    System.out.println(ColorPrinter.yellow("     The monitor script will continue running and capture data when service starts"));
                }
                
                System.out.println(ColorPrinter.yellow("\nUse 'ro.monitor.stop' to stop monitoring and view results"));
            } else {
                System.out.println(ColorPrinter.red("✗ Failed to start monitoring"));
                System.out.println(ColorPrinter.red("  Verification result: " + verifyResult));
                
                // Debug: Check what went wrong
                ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
                runCommandShort(debugStream, Util.fullMachineName(machine), userName, passwd,
                    "ls -la /tmp/monitor_* 2>&1 | tail -10");
                System.out.println(ColorPrinter.yellow("  Files in /tmp:\n" + debugStream.toString()));
                
                // Show debug log if it exists - match the pattern used in the script
                ByteArrayOutputStream debugLogStream = new ByteArrayOutputStream();
                runCommandShort(debugLogStream, Util.fullMachineName(machine), userName, passwd,
                    "ls /tmp/monitor_debug_*.log 2>/dev/null | tail -1 | xargs cat 2>/dev/null || echo 'No debug log found'");
                System.out.println(ColorPrinter.yellow("  Debug log:\n" + debugLogStream.toString()));
                
                // Also check if CSV file has data
                if (monitorLogFile != null) {
                    ByteArrayOutputStream csvCheckStream = new ByteArrayOutputStream();
                    runCommandShort(csvCheckStream, Util.fullMachineName(machine), userName, passwd,
                        "test -f " + monitorLogFile + " && (wc -l " + monitorLogFile + "; tail -3 " + monitorLogFile + ") || echo 'No CSV file'");
                    System.out.println(ColorPrinter.yellow("  CSV file status:\n" + csvCheckStream.toString()));
                }
                
                // Clean up failed monitoring attempt
                System.out.println(ColorPrinter.cyan("\nCleaning up failed monitoring attempt..."));
                try {
                    ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
                    String cleanupCmd = String.format(
                        "if [ -f %s ]; then " +
                        "  DEAD_PID=$(cat %s); " +
                        "  kill $DEAD_PID 2>/dev/null; " +
                        "  rm -f %s; " +
                        "fi; " +
                        "rm -f /tmp/service_monitor_*.sh /tmp/get_process_name.sh /tmp/monitor_debug_%s.log 2>/dev/null; " +
                        "echo 'Cleanup done'",
                        monitorPidFile, monitorPidFile, monitorPidFile, sanitizedName
                    );
                    runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
                    System.out.println(ColorPrinter.green("  " + cleanupStream.toString().trim()));
                } catch (Exception cleanupEx) {
                    System.out.println(ColorPrinter.yellow("  Warning: Cleanup failed: " + cleanupEx.getMessage()));
                }
                
                monitoringService = null;
                monitorLogFile = null;
                monitorPidFile = null;
            }
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error starting monitor: " + e.getMessage()));
            e.printStackTrace();
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
        }
    }

    @ShellMethod(key = "ro.monitor.stop", value = "eg: ro.monitor.stop")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_stop() {
        if (monitoringService == null) {
            System.out.println(ColorPrinter.yellow("No active monitoring session"));
            return;
        }
        
        try {
            System.out.println(ColorPrinter.cyan("Stopping monitoring for: " + monitoringService));
            
            // Stop the monitoring process
            String stopCmd = String.format(
                "test -f %s && kill $(cat %s) 2>/dev/null; rm -f %s",
                monitorPidFile, monitorPidFile, monitorPidFile
            );
            
            ByteArrayOutputStream stopStream = new ByteArrayOutputStream();
            runCommandShort(stopStream, Util.fullMachineName(machine), userName, passwd, stopCmd);
            
            // Wait a moment for the process to stop
            Thread.sleep(500);
            
            // Download and display the log file
            ByteArrayOutputStream logStream = new ByteArrayOutputStream();
            runCommandShort(logStream, Util.fullMachineName(machine), userName, passwd,
                "cat " + monitorLogFile);
            
            String logContent = logStream.toString();
            
            if (logContent.isEmpty()) {
                System.out.println(ColorPrinter.red("No monitoring data collected"));
            } else {
                // Parse and display the results
                displayMonitoringResults(logContent, monitoringService);
                
                // Save locally
                String localLogFile = "monitor_" + monitoringService.replace(".service", "") + "_" +
                                     System.currentTimeMillis() + ".csv";
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get(localLogFile),
                    logContent
                );
                System.out.println(ColorPrinter.green("\n✓ Monitoring data saved to: " + localLogFile));
            }
            
            // Cleanup remote files
            String cleanupCmd = String.format("rm -f %s /tmp/service_monitor_*.sh", monitorLogFile);
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd, cleanupCmd);
            monitor_cleanup();
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error stopping monitor: " + e.getMessage()));
            e.printStackTrace();
        } finally {
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
        }
    }

    @ShellMethod(key = "ro.monitor.cleanup", value = "eg: ro.monitor.cleanup - Stop all monitoring processes")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_cleanup() {
        try {
            System.out.println(ColorPrinter.cyan("Cleaning up all monitoring processes..."));
            
            // Find and kill all service_monitor processes
            ByteArrayOutputStream listStream = new ByteArrayOutputStream();
            runCommandShort(listStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep");
            
            String processList = listStream.toString();
            if (processList.trim().isEmpty()) {
                System.out.println(ColorPrinter.green("✓ No monitoring processes found"));
                return;
            }
            
            // Show what will be killed
            System.out.println(ColorPrinter.yellow("Found monitoring processes:"));
            System.out.println(processList);
            
            // Kill all service_monitor processes
            ByteArrayOutputStream killStream = new ByteArrayOutputStream();
            runCommandShort(killStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep | awk '{print $1}' | xargs kill 2>/dev/null; " +
                "sleep 1; " +
                "ps | grep 'service_monitor' | grep -v grep | awk '{print $1}' | xargs kill -9 2>/dev/null");
            
            // Clean up all related files
            ByteArrayOutputStream cleanupStream = new ByteArrayOutputStream();
            runCommandShort(cleanupStream, Util.fullMachineName(machine), userName, passwd,
                "rm -f /tmp/monitor_*.pid /tmp/service_monitor_*.sh /tmp/get_process_name.sh /tmp/monitor_debug_*.log /tmp/monitor_*.csv 2>/dev/null; " +
                "echo 'Removed PID files, scripts, debug logs, and CSV files'");
            
            System.out.println(ColorPrinter.green("\n✓ Cleanup complete"));
            System.out.println(ColorPrinter.cyan(cleanupStream.toString()));
            
            // Verify no processes remain
            ByteArrayOutputStream verifyStream = new ByteArrayOutputStream();
            runCommandShort(verifyStream, Util.fullMachineName(machine), userName, passwd,
                "ps | grep 'service_monitor' | grep -v grep | wc -l");
            
            int remaining = 0;
            try {
                remaining = Integer.parseInt(verifyStream.toString().trim());
            } catch (NumberFormatException e) {
                // ignore
            }
            
            if (remaining > 0) {
                System.out.println(ColorPrinter.yellow("⚠ Warning: " + remaining + " process(es) still running (may require manual cleanup)"));
            } else {
                System.out.println(ColorPrinter.green("✓ All monitoring processes stopped"));
            }
            
            // Reset local state
            monitoringService = null;
            monitorLogFile = null;
            monitorPidFile = null;
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error during cleanup: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @ShellMethod(key = "ro.monitor.status", value = "eg: ro.monitor.status")
    @ShellMethodAvailability("availabilityCheck")
    void monitor_status() {
        if (monitoringService == null) {
            System.out.println(ColorPrinter.yellow("No active monitoring session"));
            return;
        }
        
        try {
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Active Monitoring Session"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.yellow("Service: ") + monitoringService);
            System.out.println(ColorPrinter.yellow("Log file: ") + monitorLogFile);
            System.out.println(ColorPrinter.yellow("PID file: ") + monitorPidFile);
            
            // Check if still running
            ByteArrayOutputStream checkStream = new ByteArrayOutputStream();
            runCommandShort(checkStream, Util.fullMachineName(machine), userName, passwd,
                "test -f " + monitorPidFile + " && ps -p $(cat " + monitorPidFile + ") > /dev/null && echo 'RUNNING' || echo 'STOPPED'");
            
            String status = checkStream.toString().trim();
            if (status.contains("RUNNING")) {
                System.out.println(ColorPrinter.green("Status: ") + ColorPrinter.green("● Running"));
                
                // Show sample of recent data
                ByteArrayOutputStream tailStream = new ByteArrayOutputStream();
                runCommandShort(tailStream, Util.fullMachineName(machine), userName, passwd,
                    "tail -n 5 " + monitorLogFile);
                
                System.out.println(ColorPrinter.cyan("\nRecent data (last 5 entries):"));
                System.out.println(tailStream.toString());
            } else {
                System.out.println(ColorPrinter.red("Status: ") + ColorPrinter.red("● Stopped"));
            }
            
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error checking monitor status: " + e.getMessage()));
        }
    }

    private void displayMonitoringResults(String csvContent, String serviceName) {
        try {
            String[] lines = csvContent.split("\\R");
            if (lines.length < 2) {
                System.out.println(ColorPrinter.red("Insufficient data collected"));
                return;
            }
            
            // Parse CSV data
            List<MonitorData> dataPoints = new ArrayList<>();
            String processName = "unknown";
            
            for (int i = 1; i < lines.length; i++) {
                String[] parts = lines[i].split(",");
                if (parts.length >= 9) {
                    try {
                        // New format: timestamp,process_name,pid,full_command_path,threads,mem_percent,mem_vsz_kb,unknown_field,cpu_percent
                        if (i == 1) {
                            processName = parts[1]; // Capture process name from first data line
                        }
                        
                        // Parse percentages (remove % suffix if present)
                        String memStr = parts[5].replace("%", "");
                        String cpuStr = parts[8].replace("%", "");
                        
                        MonitorData data = new MonitorData(
                            parts[0], // timestamp
                            parts[1], // process_name
                            parts[2], // pid
                            Double.parseDouble(cpuStr), // cpu (field 8)
                            Double.parseDouble(memStr), // mem (field 5)
                            0, // rss (not available in new format)
                            Long.parseLong(parts[6]), // vsz (field 6)
                            Integer.parseInt(parts[4]), // threads (field 4)
                            "R"  // status (not available, default to Running)
                        );
                        dataPoints.add(data);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines (like N/A entries)
                    }
                } else if (parts.length >= 8) {
                    // Old format fallback: timestamp,pid,cpu,mem,rss,vsz,threads,status
                    try {
                        MonitorData data = new MonitorData(
                            parts[0], // timestamp
                            "unknown", // process_name
                            parts[1], // pid
                            Double.parseDouble(parts[2]), // cpu
                            Double.parseDouble(parts[3]), // mem
                            Long.parseLong(parts[4]), // rss
                            Long.parseLong(parts[5]), // vsz
                            Integer.parseInt(parts[6]), // threads
                            parts[7]  // status
                        );
                        dataPoints.add(data);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                    }
                }
            }
            
            if (dataPoints.isEmpty()) {
                System.out.println(ColorPrinter.red("No valid data points collected"));
                return;
            }
            
            // Display summary statistics
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  Monitoring Results for: " + serviceName));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
            double avgCpu = dataPoints.stream().mapToDouble(d -> d.cpu).average().orElse(0);
            double maxCpu = dataPoints.stream().mapToDouble(d -> d.cpu).max().orElse(0);
            double avgMem = dataPoints.stream().mapToDouble(d -> d.mem).average().orElse(0);
            double maxMem = dataPoints.stream().mapToDouble(d -> d.mem).max().orElse(0);
            long avgRss = (long) dataPoints.stream().mapToLong(d -> d.rss).average().orElse(0);
            long maxRss = dataPoints.stream().mapToLong(d -> d.rss).max().orElse(0);
            
            System.out.println(ColorPrinter.yellow("Process Name: ") + processName);
            System.out.println(ColorPrinter.yellow("Data Points: ") + dataPoints.size());
            System.out.println(ColorPrinter.yellow("Duration: ") + dataPoints.get(0).timestamp +
                             " to " + dataPoints.get(dataPoints.size() - 1).timestamp);
            System.out.println();
            System.out.println(ColorPrinter.green("CPU Usage:"));
            System.out.println("  Average: " + String.format("%.2f%%", avgCpu));
            System.out.println("  Maximum: " + String.format("%.2f%%", maxCpu));
            System.out.println();
            System.out.println(ColorPrinter.green("Memory Usage:"));
            System.out.println("  Average: " + String.format("%.2f%%", avgMem) +
                             " (" + (avgRss / 1024) + " MB)");
            System.out.println("  Maximum: " + String.format("%.2f%%", maxMem) +
                             " (" + (maxRss / 1024) + " MB)");
            
            // Display graphs for CPU and Memory
            System.out.println("\n" + ColorPrinter.cyan("Resource Usage Over Time:"));
            displayChart(
                dataPoints.stream().mapToDouble(d -> d.cpu).toArray(),
                "CPU Usage - %", 20, 30
            );
            displayChart(
                dataPoints.stream().mapToDouble(d -> d.vsz / 1024.0).toArray(),
                "Memory Usage - MB", 20, 30
            );
            
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            
        } catch (Exception e) {
            System.err.println(ColorPrinter.red("Error displaying results: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    @ShellMethod(key = "ro.statistics", value = "Display system statistics graphs. Usage: ro.statistics <max_items>")
    @ShellMethodAvailability("availabilityCheck")
    void statistics(@ShellOption(defaultValue = "10") int maxItems) {
        try {
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("                    SYSTEM STATISTICS"));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════════════"));
            
            // 1. Memory Usage Graph
            displayMemoryUsageGraph(maxItems);
            
            // 2. CPU Usage Graph
            displayCpuUsageGraph(maxItems);
            
            // 3. Disk Usage Graph
            displayDiskUsageGraph(maxItems);
            
        } catch (Exception e) {
            System.out.println(ColorPrinter.red("Error generating statistics: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private void displayMemoryUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("1. Memory Usage by Process (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use top command to get memory usage - BusyBox top shows VSZ
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "top -bn1 | grep -v 'top -' | grep -v 'Mem:' | grep -v 'CPU:' | grep -v 'Load' | grep -E '^\\s*[0-9]+' | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No process data available"));
            return;
        }
        
        List<ProcessMemInfo> processes = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // BusyBox top format: PID PPID USER STAT VSZ %MEM %CPU %CPU COMMAND
            String[] parts = line.split("\\s+", 9);
            if (parts.length >= 9) {
                try {
                    // COMMAND is at index 8 (last element after splitting with limit 9)
                    String processName = parts[8];
                    // VSZ is at column 4 (0-indexed)
                    long vszKB = Long.parseLong(parts[4]);
                    // Convert KB to MB for display
                    double memMB = vszKB / 1024.0;
                    processes.add(new ProcessMemInfo(parts[0], processName, memMB));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Sort by memory usage
        processes.sort((a, b) -> Double.compare(b.memPercent, a.memPercent));
        if (processes.size() > maxItems) {
            processes = processes.subList(0, maxItems);
        }
        
        // Display horizontal bar chart
        if (!processes.isEmpty()) {
            double maxMem = processes.stream().mapToDouble(p -> p.memPercent).max().orElse(1.0);
            int maxNameLength = processes.stream().mapToInt(p -> p.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (ProcessMemInfo proc : processes) {
                String name = String.format("%s %s", proc.id,proc.name);
                name = name.length() > maxNameLength ? name.substring(0, maxNameLength-3) + "..." : name;
                int barLength = (int) ((proc.memPercent / maxMem) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                
                System.out.printf("%-50s │ %s %.2f MB\n", name, bar, proc.memPercent);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                Memory Usage (MB)");
        }
    }
    
    private void displayCpuUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("2. CPU Usage by Process (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // BusyBox top command to get CPU usage - run once and parse output
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "top -bn1 | grep -v 'top -' | grep -v 'Mem:' | grep -v 'CPU:' | grep -v 'Load' | grep -E '^\\s*[0-9]+' | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No process data available"));
            return;
        }
        
        List<ProcessCpuInfo> processes = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // BusyBox top format: PID PPID USER STAT VSZ %MEM %CPU %CPU COMMAND
            String[] parts = line.split("\\s+", 9);
            if (parts.length >= 9) {
                try {
                    // COMMAND is at index 8 (last element after splitting with limit 9)
                    String processName = parts[8];
                    // %CPU is at columns 6-7, but we need to parse it correctly
                    // It appears as two separate fields: "0" and "0%"
                    String cpuStr = parts[7].replace("%", "");
                    double cpuPercent = Double.parseDouble(cpuStr);
                    processes.add(new ProcessCpuInfo(processName, cpuPercent));
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Sort by CPU usage
        processes.sort((a, b) -> Double.compare(b.cpuPercent, a.cpuPercent));
        if (processes.size() > maxItems) {
            processes = processes.subList(0, maxItems);
        }
        
        // Display horizontal bar chart
        if (!processes.isEmpty()) {
            double maxCpu = processes.stream().mapToDouble(p -> p.cpuPercent).max().orElse(1.0);
            int maxNameLength = processes.stream().mapToInt(p -> p.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (ProcessCpuInfo proc : processes) {

                String name = proc.name.length() > maxNameLength ? proc.name.substring(0, maxNameLength-3) + "..." : proc.name;
                int barLength = (int) ((proc.cpuPercent / maxCpu) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                
                System.out.printf("%-50s │ %s %.1f%%\n", name, bar, proc.cpuPercent);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                CPU Usage (%)");
        }
    }
    
    private void displayDiskUsageGraph(int maxItems) throws IOException {
        System.out.println("\n" + ColorPrinter.yellow("3. Disk Usage by Binary in /usr/bin (Top " + maxItems + ")"));
        System.out.println(ColorPrinter.yellow("───────────────────────────────────────────────────────────────"));
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Get disk usage of binaries in /usr/bin sorted by size
        runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                "du -b /usr/bin/* 2>/dev/null | sort -rn | head -n " + maxItems);
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        if (lines.length == 0) {
            System.out.println(ColorPrinter.red("No binary data available"));
            return;
        }
        
        List<BinaryDiskInfo> binaries = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\s+", 2);
            if (parts.length >= 2) {
                try {
                    long sizeBytes = Long.parseLong(parts[0]);
                    String fullPath = parts[1];
                    String binaryName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                    binaries.add(new BinaryDiskInfo(binaryName, sizeBytes));
                } catch (NumberFormatException e) {
                    // Skip invalid lines
                }
            }
        }
        
        // Display horizontal bar chart
        if (!binaries.isEmpty()) {
            long maxSize = binaries.stream().mapToLong(b -> b.sizeBytes).max().orElse(1L);
            int maxNameLength = binaries.stream().mapToInt(b -> b.name.length()).max().orElse(10);
            maxNameLength = Math.min(maxNameLength, 50);
            
            for (BinaryDiskInfo binary : binaries) {
                String name = binary.name.length() > maxNameLength ? binary.name.substring(0, maxNameLength-3) + "..." : binary.name;
                int barLength = (int) ((binary.sizeBytes / (double) maxSize) * 50);
                String bar = "█".repeat(Math.max(0, barLength));
                String sizeStr = formatBytes(binary.sizeBytes);
                
                System.out.printf("%-50s │ %s %s\n", name, bar, sizeStr);
            }
            System.out.println("                               └" + "─".repeat(50) + ">");
            System.out.println("                                Disk Usage");
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // Helper classes for statistics
    private static class ProcessMemInfo {
        String id;
        String name;
        double memPercent;
        
        ProcessMemInfo(String id,String name, double memPercent) {
            this.id = id;
            this.name = name;
            this.memPercent = memPercent;
        }
    }
    
    private static class ProcessCpuInfo {
        String name;
        double cpuPercent;
        
        ProcessCpuInfo(String name, double cpuPercent) {
            this.name = name;
            this.cpuPercent = cpuPercent;
        }
    }
    
    private static class BinaryDiskInfo {
        String name;
        long sizeBytes;
        
        BinaryDiskInfo(String name, long sizeBytes) {
            this.name = name;
            this.sizeBytes = sizeBytes;
        }
    }


    private void displayAsciiGraph(double[] values, String label, int height, int width) {
        if (values.length == 0) return;
        
        double max = java.util.Arrays.stream(values).max().orElse(1);
        double min = java.util.Arrays.stream(values).min().orElse(0);
        double range = max - min;
        if (range == 0) range = 1;
        
        // Normalize values to graph height
        int[] normalized = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (int) ((values[i] - min) / range * (height - 1));
        }
        
        // Sample data points if too many
        int step = Math.max(1, values.length / width);
        
        // Draw graph from top to bottom
        for (int row = height - 1; row >= 0; row--) {
            double valueAtRow = min + (range * row / (height - 1));
            System.out.printf("%6.1f │", valueAtRow);
            
            for (int col = 0; col < Math.min(values.length, width); col += step) {
                int idx = col;
                if (normalized[idx] == row) {
                    System.out.print("●");
                } else if (normalized[idx] > row) {
                    System.out.print("│");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Draw x-axis
        System.out.print("       └");
        for (int i = 0; i < Math.min(width, values.length / step); i++) {
            System.out.print("─");
        }
        System.out.println(">");
        System.out.println("        " + label + " (Time →)");
    }

    private void displayChart(double[] values, String label, int height, int width) {
        if (values.length == 0) return;
        
        // Normalize dataset
        double max = java.util.Arrays.stream(values).max().orElse(1);
        double min = java.util.Arrays.stream(values).min().orElse(0);
        double range = max - min;
        if (range == 0) range = 1;
        
        int[] normalized = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (int) ((values[i] - min) / range * (height - 1));
        }
        
        int step = Math.max(1, values.length / width);
        
        // Display graph
        System.out.println();
        System.out.printf("%-8s%n", label);
        System.out.println(ColorPrinter.cyan("─".repeat(width + 20)));
        
        for (int row = height - 1; row >= 0; row--) {
            double valueAtRow = min + (range * row / (height - 1));
            
            System.out.printf("%6.1f │", valueAtRow);
            for (int col = 0; col < Math.min(values.length, width); col += step) {
                int idx = col;
                if (normalized[idx] == row) {
                    System.out.print(ColorPrinter.green("●"));
                } else if (normalized[idx] > row) {
                    System.out.print(ColorPrinter.green("│"));
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Draw x-axis
        System.out.print("       └");
        for (int i = 0; i < Math.min(width, values.length / step); i++) {
            System.out.print("─");
        }
        System.out.println(">");
        System.out.printf("        %-" + width + "s%n", "Time →");
        System.out.println();
    }

    // Helper class to store monitoring data
    private static class MonitorData {
        String timestamp;
        String processName;
        String pid;
        double cpu;
        double mem;
        long rss;
        long vsz;
        int threads;
        String status;
        
        MonitorData(String timestamp, String processName, String pid, double cpu, double mem,
                   long rss, long vsz, int threads, String status) {
            this.timestamp = timestamp;
            this.processName = processName;
            this.pid = pid;
            this.cpu = cpu;
            this.mem = mem;
            this.rss = rss;
            this.vsz = vsz;
            this.threads = threads;
            this.status = status;
        }
    }
    
    // Helper class to hold memory statistics data
    private static class MemStatData {
        long timestamp;      // Time offset in milliseconds
        long totalBytes;     // Total virtual memory (VSZ)
        long residentBytes;  // Resident set size (RSS)
        long sharedBytes;    // Shared memory
        long textPages;      // Text (code) pages
        long dataPages;      // Data + stack pages
        
        MemStatData(long timestamp, long totalBytes, long residentBytes,
                   long sharedBytes, long textPages, long dataPages) {
            this.timestamp = timestamp;
            this.totalBytes = totalBytes;
            this.residentBytes = residentBytes;
            this.sharedBytes = sharedBytes;
            this.textPages = textPages;
            this.dataPages = dataPages;
        }
    }
}
