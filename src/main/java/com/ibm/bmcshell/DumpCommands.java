package com.ibm.bmcshell;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.IOException;
import java.net.URISyntaxException;

@ShellComponent
public class DumpCommands extends CommonCommands{
    protected DumpCommands() throws IOException {
    }
    @ShellMethod(key="bmcdump")
    @ShellMethodAvailability("availabilityCheck")
    public void bmcdump() throws URISyntaxException, IOException {

        post("/redfish/v1/Managers/bmc/LogServices/Dump/Actions/LogService.CollectDiagnosticData","{\"DiagnosticDataType\" : \"Manager\",\"OEMDiagnosticDataType\": \"Manager\"}");
    }
    @ShellMethod(key="systemdump")
    @ShellMethodAvailability("availabilityCheck")
    public void systemdump() throws URISyntaxException, IOException {

        post("/redfish/v1/Systems/system/LogServices/Dump/Actions/LogService.CollectDiagnosticData","{\"DiagnosticDataType\":\"OEM\", \"OEMDiagnosticDataType\":\"System\"}");
    }
    @ShellMethod(key="list_systemdump")
    @ShellMethodAvailability("availabilityCheck")
    public void list_systemdump() throws URISyntaxException, IOException {

        get("/redfish/v1/Systems/system/LogServices/Dump/Entries","",true);
    }
    @ShellMethod(key="list_bmcdump")
    @ShellMethodAvailability("availabilityCheck")
    public void list_bmcdump() throws URISyntaxException, IOException {
        get("/redfish/v1/Managers/bmc/LogServices/Dump/Entries","",true);
    }
    @ShellMethod(key="bmc_dump_offload",value = "eg: bmc_dump_offload 4 out_filename")
    @ShellMethodAvailability("availabilityCheck")
    public void bmc_dump_offload(int id,String filename) throws URISyntaxException, IOException {
        get(String.format("/redfish/v1/Managers/bmc/LogServices/Dump/Entries/%d/attachment",id),filename,false);
    }
    @ShellMethod(key="system_dump_offload",value = "eg: system_dump_offload 4 out_filename")
    @ShellMethodAvailability("availabilityCheck")
    public void system_dump_offload(int id,String filename) throws URISyntaxException, IOException {
        get(String.format("/redfish/v1/Systems/system/LogServices/Dump/Entries/System/%d/attachment",id),filename,false);
    }

}
