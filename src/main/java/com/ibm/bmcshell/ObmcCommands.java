package com.ibm.bmcshell;

import com.ibm.bmcshell.script.ScriptRunner;
import kotlin.Pair;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@ShellComponent
public class ObmcCommands extends CommonCommands{

    protected ObmcCommands() throws IOException {
    }
    @ShellMethod(key="wait_for_host_on")
    @ShellMethodAvailability("availabilityCheck")
    void wait_for_host_on() throws InterruptedException {

        waitForObmcStatus("CurrentPowerState==\"On\"");

    }
    @ShellMethod(key="wait_for_host_off")
    @ShellMethodAvailability("availabilityCheck")
    void wait_for_host_off() throws InterruptedException {

        waitForObmcStatus("CurrentPowerState==\"Off\"");

    }
    @ShellMethod(key="obmcutil",value = "eg obmcutil status")
    @ShellMethodAvailability("availabilityCheck")
    void obmcutil(String comm) throws InterruptedException {
        scmd("obmcutil "+comm);
    }
    void waitForObmcStatus(String condition) throws InterruptedException {
        while (true){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            redirector(outputStream,()-> {
                scmd("obmcutil status");
            });
            String res=outputStream.toString();
            System.out.println(res);
            var map= Arrays.stream(res.split("\n")).filter(a->!a.isEmpty())
                    .filter(a->!a.contains("Standard Error"))
                    .map(a->{
                        var kv=a.split(":");
                        var value=kv[1].split("\\.");
                        return new Pair(kv[0], value[value.length-1]);
                    })
                    .collect(Collectors.toMap(a-> a.getFirst().toString().trim(), a-> a.getSecond().toString().trim()));
            if(ScriptRunner.evaluateCondition(condition,map)){
                break;
            }
            sleep(2);
        }
    }
}
