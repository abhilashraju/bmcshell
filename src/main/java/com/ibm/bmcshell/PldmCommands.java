package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.net.URISyntaxException;

@ShellComponent
public class PldmCommands extends CommonCommands{
    protected PldmCommands() throws IOException {
    }
    @ShellMethod(key = "pldmtool",value = "eg: pldmtool 'subcommand'")
    void pldmtool(String subcommand){
        scmd(String.format("pldmtool %s",subcommand));
    }
}
