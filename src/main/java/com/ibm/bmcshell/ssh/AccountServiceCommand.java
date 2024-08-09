package com.ibm.bmcshell.ssh;

import com.ibm.bmcshell.CommonCommands;
import com.ibm.bmcshell.DbusCommnads;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@ShellComponent
public class AccountServiceCommand extends CommonCommands {

    protected AccountServiceCommand(DbusCommnads dbusCommnads) throws IOException {
        this.dbusCommnads = dbusCommnads;
    }
    private final DbusCommnads dbusCommnads;


    @ShellMethod(key="as.create_user")
    @ShellMethodAvailability("availabilityCheck")
    public String createUser(@ShellOption(value = {"--name", "-n"}) String name,
                             @ShellOption(value = {"--privilege", "-p"},defaultValue="priv-admin")String privilege)  {

        String args=String.format("%s %d %s %s true",name,2,"ssh redfish",privilege);
        dbusCommnads.call("xyz.openbmc_project.User.Manager",
                "/xyz/openbmc_project/user",
                "xyz.openbmc_project.User.Manager",
                "CreateUser",
                "sassb",args);
        return "Finished";
    }
}
