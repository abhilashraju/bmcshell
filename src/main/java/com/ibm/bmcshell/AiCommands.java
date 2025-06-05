package com.ibm.bmcshell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.inferencing.LLaMA3Client;

@ShellComponent
public class AiCommands extends CommonCommands {

    AiCommands() throws IOException, URISyntaxException {
        super();
    }

    @ShellMethod(key = "c")
    public void complete(String q) throws Exception {
        LLaMA3Client.suggest(q);
        System.out.println("");
    }

    @ShellMethod(key = "q")
    public void explain(@ShellOption(valueProvider = MyCustomValueProvider.class) String q)
            throws Exception {
        String bufferContent = BmcshellApplication.getCircularBufferContent();
        LLaMA3Client.ask(q + "\n\nYou may use the following context, but are not limited to it:\n\nContext:\n" + bufferContent);
        System.out.println("");
    }

    @ShellMethod(key = "?")
    public void ask() throws Exception {
        LLaMA3Client.ask("Explain the following \n\n" + lastCurlResponse);
        System.out.println("");
    }

    @ShellMethod(key = "ai.ls")
    public void listModels() throws Exception {

        LLaMA3Client.listModels();

    }

    @ShellMethod(key = "clearall")
    public void clear_buffer() throws IOException, Exception {
        BmcshellApplication.clear_buffer();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("clear")) {
            if (is == null) {
                throw new FileNotFoundException("Resource 'clear' not found");
            }
            // If your script expects a File, you need to copy it to a temp file:
            File tempScript = File.createTempFile("clear", null);
            tempScript.deleteOnExit();
            try (OutputStream os = new FileOutputStream(tempScript)) {
                is.transferTo(os);
            }
            script.script(tempScript);
        }
    }

    @ShellMethod(key = "ai.set-model")
    public void setModel(
            @ShellOption(value = { "-m", "--model" }, help = "set model to use", defaultValue = "codellama") String m)
            throws Exception {
        LLaMA3Client.setModel(m);
        System.out.println("Model set to " + m);
    }

    @ShellMethod(key = "ai.pull-model")
    public void pullModel(
            @ShellOption(value = { "-m", "--model" }, help = "model to pull") String modelName)
            throws Exception {
        LLaMA3Client.pullModel(modelName);
        System.out.println("Model pulled: " + modelName);
    }

    @ShellMethod(key = "ai.testcase")
    public void testcase(
            @ShellOption(value = { "-f", "--framework" }, defaultValue = " using gtest framework") String extra)
            throws IOException {

        String testCase = "Test case for the request" + lastCurlRequest + " and response " + lastCurlResponse + extra;
        LLaMA3Client.ask(testCase);
        System.out.println();
    }
}
