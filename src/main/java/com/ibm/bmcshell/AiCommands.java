package com.ibm.bmcshell;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import com.ibm.bmcshell.inferencing.LLaMA3Client;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URISyntaxException;
import com.ibm.bmcshell.inferencing.LLaMA3Client;
@ShellComponent
public class AiCommands extends CommonCommands{

    @ShellMethod(key="ai.ask")
    public String getQuery(String q) throws Exception {
        System.out.println(LLaMA3Client.getAnswer(q));

    }
}
