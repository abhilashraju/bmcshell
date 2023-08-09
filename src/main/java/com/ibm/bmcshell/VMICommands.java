package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.IOException;
import java.net.URISyntaxException;
@ShellComponent
public class VMICommands extends CommonCommands{
    protected VMICommands() throws IOException {
    }
    @ShellMethod(key="vmi")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi() throws URISyntaxException, IOException {

        execute(new Utils.EndPoints("Systems/hypervisor","Get"),"",false,"");

    }
    @ShellMethod(key="vmi_eth_interfaces")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_eth_interfaces() throws URISyntaxException, IOException {

        execute(new Utils.EndPoints("Systems/hypervisor/EthernetInterfaces","Get"),"",false,"");

    }
    @ShellMethod(key="vmi_eth_interface")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_eth_interface(String iface) throws URISyntaxException, IOException {

        execute(new Utils.EndPoints("Systems/hypervisor/EthernetInterfaces/"+iface,"Get"),"",false,"");

    }

}
