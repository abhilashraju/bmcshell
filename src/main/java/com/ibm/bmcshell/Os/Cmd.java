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

    public static class PipeHandler
    {
        List<Process> precesses=new ArrayList<>();
        public PipeHandler addProcess(Process process) throws IOException {
            if(precesses.isEmpty()){
                precesses.add(process);
                return this;
            }
            var prev=precesses.get(precesses.size()-1);

            process.getOutputStream().write(prev.getInputStream().readAllBytes());
            return  this;
        }
        public int waitFor() throws InterruptedException, IOException {
            InputStream inputStream = precesses.get(precesses.size()-1).getInputStream();

            // Create a buffered reader to read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            return precesses.get(precesses.size()-1).waitFor();
        }
    }
    public static void execute(String command,String passwd)
    {
        try {
            var chains=command.split("\\|");
//            if(chains.length>1){
                 handleProcessChain(chains,passwd);
                 return;
//            }
//            handleSingelProcess(command, passwd);
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


        while (true){
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            String data = scanner.nextLine();
            if(data.isEmpty()){
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
        var args= command.split(" ");
        // Specify the command and its arguments
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SSHPASS", passwd);
        processBuilder.redirectErrorStream(true);
        // Start the process
        Process process = processBuilder.start();
        return process;
    }

     static ProcessBuilder makeNextBuilder(String command, String passwd, ProcessBuilder prev)
    {
        var args= command.split(" ");
        // Specify the command and its arguments
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().put("SSHPASS", passwd);
        processBuilder.redirectErrorStream(true);
        if(prev !=null){
            prev.redirectOutput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        }
        return processBuilder;
    }
    private static void handleProcessChain(String[] chains,String passwd) throws InterruptedException, IOException {
        PipeHandler pipe=new PipeHandler();
        AtomicReference<ProcessBuilder> current=new AtomicReference<>();
        Arrays.stream(chains).forEach(a->{
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
