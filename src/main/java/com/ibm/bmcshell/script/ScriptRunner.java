package com.ibm.bmcshell.script;

import kotlin.Pair;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ScriptRunner {
    public static void main(String[] args) {
       String result="CurrentBMCState     : xyz.openbmc_project.State.BMC.BMCState.Ready\n" +
               "CurrentPowerState   : xyz.openbmc_project.State.Chassis.PowerState.Off\n" +
               "CurrentHostState    : xyz.openbmc_project.State.Host.HostState.Off\n" +
               "BootProgress        : xyz.openbmc_project.State.Boot.Progress.ProgressStages.Unspecified\n" +
               "OperatingSystemState: xyz.openbmc_project.State.OperatingSystem.Status.OSStatus.Inactive";
        var map=Arrays.stream(result.split("\n")).map(a->{
            var kv=a.split(":");
            var value=kv[1].split("\\.");
            return new Pair(kv[0], value[value.length-1]);
        }).collect(Collectors.toMap(a-> a.getFirst().toString().trim(), a-> a.getSecond().toString().trim()));
        System.out.println(evaluateCondition("CurrentHostState==\"On\"",map));
    }
    public static boolean evaluateCondition(String script, Map<String,String> args)
    {
        try (Context context = Context.create()) {
            // Create a Bindings object to hold variables
            Value bindings = context.getBindings("js");
            for(var p:args.entrySet()){
                bindings.putMember(p.getKey(), p.getValue());
            }
            // Pass values to the Bindings object
            // Evaluate JavaScript code that uses the variables
            Value result = context.eval("js", script);
            return result.asBoolean();
        }
    }
}
