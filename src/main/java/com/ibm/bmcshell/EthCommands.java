package com.ibm.bmcshell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URISyntaxException;

@ShellComponent
public class EthCommands extends CommonCommands{

    @Autowired
    private Environment env;

    EthCommands() throws IOException, URISyntaxException {
        super();
    }



    @ShellMethod(key = "hello-world")
    @ShellMethodAvailability("availabilityCheck")
    public String helloWorld(
            @ShellOption(defaultValue = "spring") String arg
    ) {
        return "Hello world " + arg;
    }

    @ShellMethod(key="eth_iface")
    @ShellMethodAvailability("availabilityCheck")
    public String ethernet_interfaces(String name) throws URISyntaxException {
        String target = name.equals("*")?"Managers/bmc/EthernetInterfaces":"Managers/bmc/EthernetInterfaces/"+name;
        return makeGetRequest(target,"");

    }


}