package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.*;

@ShellComponent
public class SerializeCommands {

    String lastResult;


    String save(String content) throws IOException {
        lastResult=content;
        return content;
    }
    String lastResult(){
        return lastResult;
    }
}
