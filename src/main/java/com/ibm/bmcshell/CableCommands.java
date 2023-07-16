package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URISyntaxException;
@ShellComponent
public class CableCommands extends CommonCommands{
    CableCommands() throws IOException, URISyntaxException {
        super();
    }
    @ShellMethod(key="cable_collection")
    @ShellMethodAvailability("availabilityCheck")
    public String cableCollection() throws URISyntaxException {
        String target = "Cables/";
        return makeGetRequest(target,"");

    }
    @ShellMethod(key="route_cables")
    public String routeCables() throws URISyntaxException {
        String target = "Cables/<str>/";
        return makeGetRequest(target,"");
    }

}
