package com.ibm.bmcshell;
import java.io.IOException;
import java.net.URISyntaxException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

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
}
