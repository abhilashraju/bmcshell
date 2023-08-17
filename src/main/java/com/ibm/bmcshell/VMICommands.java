package com.ibm.bmcshell;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.ibm.bmcshell.ssh.SSHShellClient.runShell;

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
    public void vmi_eth_interface(String iface, @ShellOption(value = {"--data", "-d"},defaultValue="") String d , @ShellOption(value = {"--file", "-f"},defaultValue="") String f, @ShellOption(value = {"--patch", "-p"},defaultValue="false") boolean p) throws URISyntaxException, IOException {
        if(!d.isEmpty()){
            d=d.substring(0,d.length()-1);///some strange , comes at end . need to remove it
        }
        if(!f.isEmpty()){
            var stream=new FileInputStream(new File(f));
            d = new String(stream.readAllBytes());
            stream.close();
        }
        execute(new Utils.EndPoints("Systems/hypervisor/EthernetInterfaces/"+iface,"Get"),d,p,"");

    }
    @ShellMethod(key="vmi.ssh")
    @ShellMethodAvailability("availabilityCheck")
    public void vmi_ssh(){
        runShell(String.format("%s.aus.stglabs.ibm.com",machine),userName,passwd,2201);
        System.out.println("Exited Shell");
        displayCurrent();

    }


}
