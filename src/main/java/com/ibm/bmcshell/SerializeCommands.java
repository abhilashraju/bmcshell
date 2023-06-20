package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.*;

@ShellComponent
public class SerializeCommands {
    String fileName;
    @ShellMethod(key = "redirect")
    void redirect(String f)
    {
        fileName=f;
    }
    @ShellMethod(key = "er")
    void enableRedirect(@ShellOption(arity=1, defaultValue="true") boolean val)
    {
        if(!val){
            fileName=null;
            return;
        };
        if(fileName == null){
            fileName="temp";
        }
    }
    @ShellMethod(key = "dr")
    void disableRedirect(@ShellOption(arity=1, defaultValue="true") boolean val)
    {
        enableRedirect(!val);
    }
    boolean isRedirected(){
        return fileName!=null;
    }
    String getFileName(){
        return fileName;
    }
    String save(String content) throws IOException {
        if(isRedirected()){
            FileWriter writer = new FileWriter(new File(getFileName()));
            writer.write(content);
            writer.close();
        }
        return content;
    }
    @ShellMethod(key = "pop")
    String read() throws IOException {
        if(isRedirected()){
            FileInputStream stream= new FileInputStream(new File(getFileName()));
            var ret= new String(stream.readAllBytes());
            stream.close();
            return ret;
        }
        return null;
    }
}
