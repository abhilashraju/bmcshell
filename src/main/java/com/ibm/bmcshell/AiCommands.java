package com.ibm.bmcshell;

import java.io.IOException;
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
        LLaMA3Client.ask(q);
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
    public void testcase(@ShellOption(value = { "-f", "--framework" }, defaultValue = "gtest") String framework,
    @ShellOption(value = { "-a", "--additional" }, defaultValue = "") String extra)
        throws Exception {
        
        String testCase = "Test case for the request" + lastCurlRequest + " and response " + lastCurlResponse +
            " using the " + framework + " framework. " +extra;
        LLaMA3Client.ask(testCase);
        System.out.println();
    }
}
