package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class RbmcToolCommands extends CommonCommands {
    protected RbmcToolCommands() throws IOException {
    }

    /**
     * Display basic RBMC information
     * 
     * Example: rbmc.display
     */
    @ShellMethod(key = "rbmc.display", value = "Display basic RBMC information")
    @ShellMethodAvailability("availabilityCheck")
    protected void displayBasic() {
        scmd("rbmctool -d");
    }

    /**
     * Display RBMC information with extended details
     * 
     * Example: rbmc.display-extended
     */
    @ShellMethod(key = "rbmc.display-extended", value = "Display RBMC information with extended details")
    @ShellMethodAvailability("availabilityCheck")
    protected void displayExtended() {
        scmd("rbmctool -d -e");
    }

    /**
     * Display RBMC information in JSON format
     * 
     * Example: rbmc.display-json
     */
    @ShellMethod(key = "rbmc.display-json", value = "Display RBMC information in JSON format")
    @ShellMethodAvailability("availabilityCheck")
    protected void displayJson() {
        scmd("rbmctool -d --json");
    }

    /**
     * Display RBMC information with extended details in JSON format
     * 
     * Example: rbmc.display-extended-json
     */
    @ShellMethod(key = "rbmc.display-extended-json", value = "Display RBMC information with extended details in JSON")
    @ShellMethodAvailability("availabilityCheck")
    protected void displayExtendedJson() {
        scmd("rbmctool -d -e --json");
    }

    /**
     * Set override to disable redundancy
     * 
     * Example: rbmc.set-disable-redundancy-override
     */
    @ShellMethod(key = "rbmc.set-disable-redundancy-override", value = "Set override to disable redundancy")
    @ShellMethodAvailability("availabilityCheck")
    protected void setDisableRedundancyOverride() {
        scmd("rbmctool --set-disable-redundancy-override");
    }

    /**
     * Clear override to disable redundancy
     * 
     * Example: rbmc.clear-disable-redundancy-override
     */
    @ShellMethod(key = "rbmc.clear-disable-redundancy-override", value = "Clear override to disable redundancy")
    @ShellMethodAvailability("availabilityCheck")
    protected void clearDisableRedundancyOverride() {
        scmd("rbmctool --clear-disable-redundancy-override");
    }

    /**
     * Reset the sibling BMC
     * 
     * Example: rbmc.reset-sibling
     */
    @ShellMethod(key = "rbmc.reset-sibling", value = "Reset the sibling BMC")
    @ShellMethodAvailability("availabilityCheck")
    protected void resetSibling() {
        scmd("rbmctool --reset-sibling");
    }

    /**
     * Start a failover
     * 
     * Example: rbmc.failover
     */
    @ShellMethod(key = "rbmc.failover", value = "Start a failover")
    @ShellMethodAvailability("availabilityCheck")
    protected void failover() {
        scmd("rbmctool --failover");
    }

    /**
     * Start a forced failover (emergency use only)
     * 
     * Example: rbmc.force-failover
     */
    @ShellMethod(key = "rbmc.force-failover", value = "Start a forced failover (emergency use only)")
    @ShellMethodAvailability("availabilityCheck")
    protected void forceFailover() {
        scmd("rbmctool --force-failover");
    }

    /**
     * Display help for rbmctool
     * 
     * Example: rbmc.help
     */
    @ShellMethod(key = "rbmc.help", value = "Display help for rbmctool")
    @ShellMethodAvailability("availabilityCheck")
    protected void help() {
        scmd("rbmctool --help");
    }

    /**
     * Execute custom rbmctool command with full control
     * 
     * Example: rbmc.custom "rbmctool -d -e --json"
     * 
     * @param rbmcCommand The complete rbmctool command to execute
     */
    @ShellMethod(key = "rbmc.custom", value = "Execute custom rbmctool command")
    @ShellMethodAvailability("availabilityCheck")
    protected void custom(String rbmcCommand) {
        scmd(rbmcCommand);
    }
}

// Made with Bob