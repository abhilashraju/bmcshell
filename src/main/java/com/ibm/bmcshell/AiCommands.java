package com.ibm.bmcshell;
import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.ibm.bmcshell.inferencing.LLaMA3Client;
@ShellComponent
public class AiCommands extends CommonCommands{

    AiCommands() throws IOException, URISyntaxException {
        super();
    }
    @ShellMethod(key="q")
    public void getQuery(String q) throws Exception {
        LLaMA3Client.ask(q);
        System.out.println("");
    }
    @ShellMethod(key="c")
    public void complete(String q) throws Exception {
        LLaMA3Client.suggest(q);
        System.out.println("");
    }
    @ShellMethod(key="e")
    public void explain(@ShellOption(value = { "-e", "--explain" }, help = "explain following",defaultValue="") String q) throws Exception {
        if (q == null || q.isEmpty()) {
            LLaMA3Client.suggest("Explain th following \n\n"+lastCurlResponse);
        }
        LLaMA3Client.suggest(q);
        System.out.println("");
    }
    @ShellMethod(key="ai.ls")
    public void listModels() throws Exception {
        
        LLaMA3Client.listModels();
        
    }
    @ShellMethod(key="ai.set-model")
    public void setModel(@ShellOption(value = { "-m", "--model" }, help = "set model to use",defaultValue="codellama") String m) throws Exception {
        LLaMA3Client.setModel(m);
        System.out.println("Model set to "+m);
    }
}
