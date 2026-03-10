package com.ibm.bmcshell.Os;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class Cmd {

    public static class PipeHandler {
        List<Process> precesses = new ArrayList<>();

        public PipeHandler addProcess(Process process) throws IOException {
            if (precesses.isEmpty()) {
                precesses.add(process);
                return this;
            }
            var prev = precesses.get(precesses.size() - 1);

            process.getOutputStream().write(prev.getInputStream().readAllBytes());
            return this;
        }

        public int waitFor() throws InterruptedException, IOException {
            Process lastProcess = precesses.get(precesses.size() - 1);
            InputStream inputStream = lastProcess.getInputStream();
            InputStream errorStream = lastProcess.getErrorStream();

            // Create buffered readers for both streams
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

            // Read standard output
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Read error output
            while ((line = errorReader.readLine()) != null) {
                System.err.println("Error: " + line);
            }

            return lastProcess.waitFor();
        }
    }

    public static void execute(String command, String passwd) {
        try {
            var chains = command.split("\\|");
            // if(chains.length>1){
            handleProcessChain(chains, passwd);
            return;
            // }
            // handleSingelProcess(command, passwd);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void handleSingelProcess(String command, String passwd) throws IOException, InterruptedException {
        Process process = getProcess(command, passwd);

        // Get the process's input stream (output)
        InputStream inputStream = process.getInputStream();

        // Create a buffered reader to read the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStream outputStream = process.getOutputStream();
        Scanner scanner = new Scanner(System.in);
        // Write data to the process's input stream (e.g., simulate user input)

        while (true) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            String data = scanner.nextLine();
            if (data.isEmpty()) {
                break;
            }
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        // Wait for the process to complete
        int exitCode = process.waitFor();

        System.out.println("Command exited with code: " + exitCode);
    }

    @NotNull
    private static Process getProcess(String command, String passwd) throws IOException {
        // Use shell to execute command to properly handle quotes and special characters
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.environment().put("SSHPASS", passwd);
        // Don't merge error stream - keep them separate for better error visibility
        // processBuilder.redirectErrorStream(true);
        // Start the process
        Process process = processBuilder.start();
        return process;
    }

    static ProcessBuilder makeNextBuilder(String command, String passwd, ProcessBuilder prev) {
        // Use shell to execute command to properly handle quotes and special characters
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.environment().put("SSHPASS", passwd);
        // Don't merge error stream - keep them separate for better error visibility
        // processBuilder.redirectErrorStream(true);
        if (prev != null) {
            prev.redirectOutput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        }
        return processBuilder;
    }

    private static void handleProcessChain(String[] chains, String passwd) throws InterruptedException, IOException {
        PipeHandler pipe = new PipeHandler();
        AtomicReference<ProcessBuilder> current = new AtomicReference<>();
        Arrays.stream(chains).forEach(a -> {
            try {
                current.set(makeNextBuilder(a, passwd, current.get()));
                pipe.addProcess(current.get().start());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        pipe.waitFor();
    }
}
