package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * I2C Commands for BMC Shell
 * Provides comprehensive I2C bus and device management capabilities
 */
@ShellComponent
public class I2CCommands extends CommonCommands {

    protected I2CCommands() throws IOException {
    }

    // ==================== Bus Detection and Listing ====================

    @ShellMethod(key = "i2c.detect", value = "Detect I2C devices on a bus. eg: i2c.detect 0")
    void i2cDetect(@ShellOption int busNumber) throws IOException {
        scmd(String.format("i2cdetect -y %d", busNumber));
    }

    @ShellMethod(key = "i2c.detectAll", value = "Detect I2C devices on all buses. eg: i2c.detectAll")
    void i2cDetectAll() throws IOException {
        System.out.println("Scanning all I2C buses...\n");
        scmd("for bus in /sys/bus/i2c/devices/i2c-*; do " +
                "[ -d \"$bus\" ] || continue; " +
                "busnum=${bus##*/i2c-}; " +
                "echo \"=== Bus $busnum ===\"; " +
                "i2cdetect -y $busnum 2>/dev/null || echo \"Bus $busnum not accessible\"; " +
                "echo; done");
    }

    @ShellMethod(key = "i2c.buses", value = "List all I2C buses. eg: i2c.buses")
    void i2cBuses() throws IOException {
        scmd("ls -1 /sys/bus/i2c/devices/ | grep '^i2c-' | sort -V");
    }

    @ShellMethod(key = "i2c.busesInfo", value = "Show detailed information about all I2C buses. eg: i2c.busesInfo")
    void i2cBusesInfo() throws IOException {
        scmd("for bus in /sys/bus/i2c/devices/i2c-*; do " +
                "[ -d \"$bus\" ] || continue; " +
                "busnum=${bus##*/i2c-}; " +
                "echo \"Bus $busnum:\"; " +
                "cat $bus/name 2>/dev/null || echo \"  Name: N/A\"; " +
                "echo \"  Path: $bus\"; " +
                "echo; done");
    }

    // ==================== Device Reading ====================

    @ShellMethod(key = "i2c.get", value = "Read byte from I2C device. eg: i2c.get 0 0x50 0x00")
    void i2cGet(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress) throws IOException {
        scmd(String.format("i2cget -y %d %s %s", busNumber, deviceAddress, registerAddress));
    }

    @ShellMethod(key = "i2c.getWord", value = "Read word from I2C device. eg: i2c.getWord 0 0x50 0x00")
    void i2cGetWord(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress) throws IOException {
        scmd(String.format("i2cget -y %d %s %s w", busNumber, deviceAddress, registerAddress));
    }

    @ShellMethod(key = "i2c.getBlock", value = "Read block from I2C device. eg: i2c.getBlock 0 0x50 0x00")
    void i2cGetBlock(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress) throws IOException {
        scmd(String.format("i2cget -y %d %s %s i", busNumber, deviceAddress, registerAddress));
    }

    // ==================== Device Writing ====================

    @ShellMethod(key = "i2c.set", value = "Write byte to I2C device. eg: i2c.set 0 0x50 0x00 0xFF")
    void i2cSet(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress,
            @ShellOption String value) throws IOException {
        scmd(String.format("i2cset -y %d %s %s %s", busNumber, deviceAddress, registerAddress, value));
    }

    @ShellMethod(key = "i2c.setWord", value = "Write word to I2C device. eg: i2c.setWord 0 0x50 0x00 0xFFFF")
    void i2cSetWord(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress,
            @ShellOption String value) throws IOException {
        scmd(String.format("i2cset -y %d %s %s %s w", busNumber, deviceAddress, registerAddress, value));
    }

    @ShellMethod(key = "i2c.setBlock", value = "Write block to I2C device. eg: i2c.setBlock 0 0x50 0x00 0x01 0x02 0x03")
    void i2cSetBlock(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress,
            @ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] values) throws IOException {
        String valueStr = String.join(" ", values);
        scmd(String.format("i2cset -y %d %s %s %s i", busNumber, deviceAddress, registerAddress, valueStr));
    }

    // ==================== Device Dumping ====================

    @ShellMethod(key = "i2c.dump", value = "Dump all registers from I2C device. eg: i2c.dump 0 0x50")
    void i2cDump(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("i2cdump -y %d %s", busNumber, deviceAddress));
    }

    @ShellMethod(key = "i2c.dumpWord", value = "Dump device registers as words. eg: i2c.dumpWord 0 0x50")
    void i2cDumpWord(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("i2cdump -y %d %s w", busNumber, deviceAddress));
    }

    @ShellMethod(key = "i2c.dumpBlock", value = "Dump device using block read. eg: i2c.dumpBlock 0 0x50")
    void i2cDumpBlock(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("i2cdump -y %d %s i", busNumber, deviceAddress));
    }

    @ShellMethod(key = "i2c.dumpRange", value = "Dump specific register range. eg: i2c.dumpRange 0 0x50 0x00 0x10")
    void i2cDumpRange(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String startReg,
            @ShellOption String endReg) throws IOException {
        scmd(String.format("i2cdump -y %d %s b %s %s", busNumber, deviceAddress, startReg, endReg));
    }

    // ==================== EEPROM Operations ====================

    @ShellMethod(key = "i2c.eepromRead", value = "Read EEPROM content. eg: i2c.eepromRead 0 0x50 256")
    void i2cEepromRead(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption(defaultValue = "256") int size) throws IOException {
        scmd(String.format("dd if=/sys/bus/i2c/devices/%d-%s/eeprom bs=1 count=%d 2>/dev/null | hexdump -C",
                busNumber, deviceAddress.replace("0x", "00"), size));
    }

    @ShellMethod(key = "i2c.eepromDump", value = "Dump EEPROM to file. eg: i2c.eepromDump 0 0x50 /tmp/eeprom.bin")
    void i2cEepromDump(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String outputFile) throws IOException {
        scmd(String.format("dd if=/sys/bus/i2c/devices/%d-%s/eeprom of=%s 2>/dev/null && echo 'EEPROM dumped to %s'",
                busNumber, deviceAddress.replace("0x", "00"), outputFile, outputFile));
    }

    @ShellMethod(key = "i2c.eepromWrite", value = "Write file to EEPROM. eg: i2c.eepromWrite 0 0x50 /tmp/eeprom.bin")
    void i2cEepromWrite(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String inputFile) throws IOException {
        System.out.println("WARNING: Writing to EEPROM can be dangerous!");
        scmd(String.format("dd if=%s of=/sys/bus/i2c/devices/%d-%s/eeprom 2>/dev/null && echo 'EEPROM written from %s'",
                inputFile, busNumber, deviceAddress.replace("0x", "00"), inputFile));
    }

    // ==================== Device Information ====================

    @ShellMethod(key = "i2c.devices", value = "List all I2C devices. eg: i2c.devices")
    void i2cDevices() throws IOException {
        scmd("ls -1 /sys/bus/i2c/devices/ | grep -v '^i2c-' | sort");
    }

    @ShellMethod(key = "i2c.devicesDetailed", value = "List all I2C devices with details. eg: i2c.devicesDetailed")
    void i2cDevicesDetailed() throws IOException {
        System.out.println("Scanning I2C devices with detailed information...\n");
        executeResourceScript("i2c_devices_detailed.sh");
    }

    @ShellMethod(key = "i2c.findDevice", value = "Find device by name pattern. eg: i2c.findDevice eeprom")
    void i2cFindDevice(@ShellOption String pattern) throws IOException {
        executeResourceScript("i2c_find_device.sh", pattern);
    }

    @ShellMethod(key = "i2c.mapBus", value = "Map all devices on a specific bus. eg: i2c.mapBus 0")
    void i2cMapBus(@ShellOption int busNumber) throws IOException {
        executeResourceScript("i2c_map_bus.sh", String.valueOf(busNumber));
    }

    @ShellMethod(key = "i2c.addressMap", value = "Show address to device mapping for all buses. eg: i2c.addressMap")
    void i2cAddressMap() throws IOException {
        executeResourceScript("i2c_address_map.sh");
    }

    @ShellMethod(key = "i2c.deviceInfo", value = "Show device information. eg: i2c.deviceInfo 0-0050")
    void i2cDeviceInfo(@ShellOption String device) throws IOException {
        scmd(String.format("dev=/sys/bus/i2c/devices/%s; " +
                "if [ -d \"$dev\" ]; then " +
                "echo 'Device: %s'; " +
                "devname=${dev##*/}; " +
                "bus=${devname%%-*}; " +
                "addr=${devname##*-}; " +
                "echo 'Bus Number: '$bus; " +
                "echo 'Slave Address: 0x'$addr; " +
                "echo 'Device Path: '$dev; " +
                "echo 'Name:'; cat $dev/name 2>/dev/null || echo '  N/A'; " +
                "echo 'Driver:'; readlink $dev/driver 2>/dev/null | xargs basename || echo '  none'; " +
                "echo 'Modalias:'; cat $dev/modalias 2>/dev/null || echo '  N/A'; " +
                "[ -f $dev/eeprom ] && echo 'EEPROM: Available at '$dev'/eeprom'; " +
                "else echo 'Device not found: %s'; fi",
                device, device, device));
    }

    @ShellMethod(key = "i2c.pathToAddress", value = "Convert device path to bus and address. eg: i2c.pathToAddress /sys/bus/i2c/devices/0-0050")
    void i2cPathToAddress(@ShellOption String devicePath) throws IOException {
        scmd(String.format("path='%s'; " +
                "if [ -d \"$path\" ]; then " +
                "devname=$(basename \"$path\"); " +
                "bus=${devname%%-*}; " +
                "addr=${devname##*-}; " +
                "echo 'Device Path: '$path; " +
                "echo 'Device Name: '$devname; " +
                "echo 'Bus Number: '$bus; " +
                "echo 'Slave Address: 0x'$addr; " +
                "echo; echo 'Usage examples:'; " +
                "echo '  i2c.detect '$bus; " +
                "echo '  i2c.get '$bus' 0x'$addr' 0x00'; " +
                "echo '  i2c.dump '$bus' 0x'$addr; " +
                "else echo 'Invalid device path'; fi", devicePath));
    }

    @ShellMethod(key = "i2c.addressToPath", value = "Convert bus and address to device path. eg: i2c.addressToPath 0 0x50")
    void i2cAddressToPath(@ShellOption int busNumber, @ShellOption String address) throws IOException {
        String cleanAddr = address.toLowerCase().replace("0x", "");
        scmd(String.format("addr='%s'; " +
                "bus='%d'; " +
                "devname=\"$bus-00$addr\"; " +
                "path=\"/sys/bus/i2c/devices/$devname\"; " +
                "echo 'Bus Number: '$bus; " +
                "echo 'Slave Address: 0x'$addr; " +
                "echo 'Device Name: '$devname; " +
                "echo 'Device Path: '$path; " +
                "if [ -d \"$path\" ]; then " +
                "echo 'Status: Device exists'; " +
                "[ -f \"$path/name\" ] && echo 'Name: '$(cat \"$path/name\"); " +
                "[ -f \"$path/eeprom\" ] && echo 'EEPROM: Available'; " +
                "else echo 'Status: Device not registered (may still be accessible via i2c tools)'; fi",
                cleanAddr, busNumber));
    }

    @ShellMethod(key = "i2c.tree", value = "Show I2C device tree. eg: i2c.tree")
    void i2cTree() throws IOException {
        scmd("tree -L 2 /sys/bus/i2c/devices/ 2>/dev/null || find /sys/bus/i2c/devices/ -maxdepth 2 -type d");
    }

    // ==================== Driver Management ====================

    @ShellMethod(key = "i2c.drivers", value = "List I2C drivers. eg: i2c.drivers")
    void i2cDrivers() throws IOException {
        scmd("ls -1 /sys/bus/i2c/drivers/");
    }

    @ShellMethod(key = "i2c.driverInfo", value = "Show driver information. eg: i2c.driverInfo at24")
    void i2cDriverInfo(@ShellOption String driverName) throws IOException {
        scmd(String.format("echo 'Driver: %s'; " +
                "echo 'Bound devices:'; ls -1 /sys/bus/i2c/drivers/%s/ 2>/dev/null | grep -E '^[0-9]+-' || echo 'None'",
                driverName, driverName));
    }

    @ShellMethod(key = "i2c.bind", value = "Bind device to driver. eg: i2c.bind at24 0-0050")
    void i2cBind(@ShellOption String driverName,
            @ShellOption String device) throws IOException {
        scmd(String.format("echo %s > /sys/bus/i2c/drivers/%s/bind", device, driverName));
        System.out.println("Device bound to driver.");
    }

    @ShellMethod(key = "i2c.unbind", value = "Unbind device from driver. eg: i2c.unbind at24 0-0050")
    void i2cUnbind(@ShellOption String driverName,
            @ShellOption String device) throws IOException {
        scmd(String.format("echo %s > /sys/bus/i2c/drivers/%s/unbind", device, driverName));
        System.out.println("Device unbound from driver.");
    }

    // ==================== Advanced Operations ====================

    @ShellMethod(key = "i2c.transfer", value = "Perform raw I2C transfer. eg: i2c.transfer 0 0x50 w 0x00 0x01 r 16")
    void i2cTransfer(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption(arity = ShellOption.ARITY_USE_HEURISTICS) String[] operations) throws IOException {
        String opsStr = String.join(" ", operations);
        scmd(String.format("i2ctransfer -y %d %s %s", busNumber, deviceAddress, opsStr));
    }

    @ShellMethod(key = "i2c.scan", value = "Quick scan for devices on bus. eg: i2c.scan 0")
    void i2cScan(@ShellOption int busNumber) throws IOException {
        System.out.println("Scanning I2C bus " + busNumber + " for devices...\n");
        scmd(String.format("i2cdetect -y -q %d", busNumber));
    }

    @ShellMethod(key = "i2c.probe", value = "Probe specific address on bus. eg: i2c.probe 0 0x50")
    void i2cProbe(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("i2cget -y %d %s 0x00 2>&1 && echo 'Device found at %s' || echo 'No device at %s'",
                busNumber, deviceAddress, deviceAddress, deviceAddress));
    }

    // ==================== Monitoring and Debugging ====================

    @ShellMethod(key = "i2c.watch", value = "Watch I2C register continuously. eg: i2c.watch 0 0x50 0x00")
    void i2cWatch(@ShellOption int busNumber,
            @ShellOption String deviceAddress,
            @ShellOption String registerAddress) throws IOException {
        scmd(String.format("watch -n 1 'i2cget -y %d %s %s'", busNumber, deviceAddress, registerAddress));
    }

    @ShellMethod(key = "i2c.trace", value = "Enable I2C tracing. eg: i2c.trace")
    void i2cTrace() throws IOException {
        scmd("echo 1 > /sys/kernel/debug/tracing/events/i2c/enable && " +
                "echo 'I2C tracing enabled. View with: cat /sys/kernel/debug/tracing/trace'");
    }

    @ShellMethod(key = "i2c.traceOff", value = "Disable I2C tracing. eg: i2c.traceOff")
    void i2cTraceOff() throws IOException {
        scmd("echo 0 > /sys/kernel/debug/tracing/events/i2c/enable && echo 'I2C tracing disabled'");
    }

    @ShellMethod(key = "i2c.traceShow", value = "Show I2C trace buffer. eg: i2c.traceShow")
    void i2cTraceShow() throws IOException {
        scmd("cat /sys/kernel/debug/tracing/trace");
    }

    @ShellMethod(key = "i2c.traceClear", value = "Clear I2C trace buffer. eg: i2c.traceClear")
    void i2cTraceClear() throws IOException {
        scmd("echo > /sys/kernel/debug/tracing/trace && echo 'Trace buffer cleared'");
    }

    @ShellMethod(key = "i2c.stats", value = "Show I2C statistics. eg: i2c.stats 0")
    void i2cStats(@ShellOption int busNumber) throws IOException {
        scmd(String.format(
                "cat /sys/bus/i2c/devices/i2c-%d/statistics/* 2>/dev/null || echo 'Statistics not available'",
                busNumber));
    }

    // ==================== Utility Commands ====================

    @ShellMethod(key = "i2c.decode", value = "Decode I2C device (requires decode-dimms, decode-vaio). eg: i2c.decode dimms")
    void i2cDecode(@ShellOption String type) throws IOException {
        switch (type.toLowerCase()) {
            case "dimms":
                scmd("decode-dimms");
                break;
            case "vaio":
                scmd("decode-vaio");
                break;
            default:
                System.out.println("Unknown decode type. Available: dimms, vaio");
        }
    }

    @ShellMethod(key = "i2c.sensors", value = "Show I2C sensor information. eg: i2c.sensors")
    void i2cSensors() throws IOException {
        scmd("sensors");
    }

    @ShellMethod(key = "i2c.sensorsDetect", value = "Detect I2C sensors. eg: i2c.sensorsDetect")
    void i2cSensorsDetect() throws IOException {
        System.out.println("Running sensors-detect (this may take a while)...");
        scmd("sensors-detect --auto");
    }

    // ==================== VPD and FRU Operations ====================

    @ShellMethod(key = "i2c.vpdRead", value = "Read VPD from EEPROM. eg: i2c.vpdRead 0 0x50")
    void i2cVpdRead(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("dd if=/sys/bus/i2c/devices/%d-%s/eeprom bs=1 count=256 2>/dev/null | " +
                "hexdump -C | head -20",
                busNumber, deviceAddress.replace("0x", "00")));
    }

    @ShellMethod(key = "i2c.fruRead", value = "Read FRU data from EEPROM. eg: i2c.fruRead 0 0x50")
    void i2cFruRead(@ShellOption int busNumber,
            @ShellOption String deviceAddress) throws IOException {
        scmd(String.format("dd if=/sys/bus/i2c/devices/%d-%s/eeprom bs=1 2>/dev/null | " +
                "hexdump -C | grep -A 5 'FRU' || echo 'No FRU data found'",
                busNumber, deviceAddress.replace("0x", "00")));
    }

    // ==================== Help ====================

    @ShellMethod(key = "i2c.help", value = "Show I2C commands help. eg: i2c.help")
    void i2cHelp() {
        System.out.println("\n=== I2C Commands Help ===\n");

        System.out.println("Bus Detection and Listing:");
        System.out.println("  i2c.detect <bus>           - Detect devices on specific bus");
        System.out.println("  i2c.detectAll              - Detect devices on all buses");
        System.out.println("  i2c.buses                  - List all I2C buses");
        System.out.println("  i2c.busesInfo              - Show detailed bus information");
        System.out.println("  i2c.scan <bus>             - Quick scan for devices");
        System.out.println("  i2c.probe <bus> <addr>     - Probe specific address");

        System.out.println("\nDevice Reading:");
        System.out.println("  i2c.get <bus> <dev> <reg>  - Read byte from register");
        System.out.println("  i2c.getWord <bus> <dev> <reg> - Read word from register");
        System.out.println("  i2c.getBlock <bus> <dev> <reg> - Read block from register");

        System.out.println("\nDevice Writing:");
        System.out.println("  i2c.set <bus> <dev> <reg> <val> - Write byte to register");
        System.out.println("  i2c.setWord <bus> <dev> <reg> <val> - Write word to register");
        System.out.println("  i2c.setBlock <bus> <dev> <reg> <vals...> - Write block to register");

        System.out.println("\nDevice Dumping:");
        System.out.println("  i2c.dump <bus> <dev>       - Dump all registers");
        System.out.println("  i2c.dumpWord <bus> <dev>   - Dump as words");
        System.out.println("  i2c.dumpBlock <bus> <dev>  - Dump using block read");
        System.out.println("  i2c.dumpRange <bus> <dev> <start> <end> - Dump register range");

        System.out.println("\nEEPROM Operations:");
        System.out.println("  i2c.eepromRead <bus> <dev> [size] - Read EEPROM content");
        System.out.println("  i2c.eepromDump <bus> <dev> <file> - Dump EEPROM to file");
        System.out.println("  i2c.eepromWrite <bus> <dev> <file> - Write file to EEPROM");

        System.out.println("\nDevice Information:");
        System.out.println("  i2c.devices                - List all I2C devices");
        System.out.println("  i2c.devicesDetailed        - List devices with bus and address details");
        System.out.println("  i2c.deviceInfo <device>    - Show device information (e.g., 0-0050)");
        System.out.println("  i2c.tree                   - Show device tree");
        System.out.println("  i2c.findDevice <pattern>   - Find device by name pattern");
        System.out.println("  i2c.mapBus <bus>           - Map all devices on specific bus");
        System.out.println("  i2c.addressMap             - Show address to device mapping for all buses");
        System.out.println("  i2c.pathToAddress <path>   - Convert device path to bus/address");
        System.out.println("  i2c.addressToPath <bus> <addr> - Convert bus/address to device path");

        System.out.println("\nDriver Management:");
        System.out.println("  i2c.drivers                - List I2C drivers");
        System.out.println("  i2c.driverInfo <driver>    - Show driver information");
        System.out.println("  i2c.bind <driver> <device> - Bind device to driver");
        System.out.println("  i2c.unbind <driver> <device> - Unbind device from driver");

        System.out.println("\nMonitoring and Debugging:");
        System.out.println("  i2c.watch <bus> <dev> <reg> - Watch register continuously");
        System.out.println("  i2c.trace                  - Enable I2C tracing");
        System.out.println("  i2c.traceOff               - Disable I2C tracing");
        System.out.println("  i2c.traceShow              - Show trace buffer");
        System.out.println("  i2c.traceClear             - Clear trace buffer");
        System.out.println("  i2c.stats <bus>            - Show I2C statistics");

        System.out.println("\nUtility Commands:");
        System.out.println("  i2c.decode <type>          - Decode device (dimms, vaio)");
        System.out.println("  i2c.sensors                - Show sensor information");
        System.out.println("  i2c.sensorsDetect          - Detect I2C sensors");
        System.out.println("  i2c.vpdRead <bus> <dev>    - Read VPD from EEPROM");
        System.out.println("  i2c.fruRead <bus> <dev>    - Read FRU data from EEPROM");

        System.out.println("\nAdvanced:");
        System.out.println("  i2c.transfer <bus> <dev> <ops...> - Perform raw I2C transfer");

        System.out.println("\nExamples:");
        System.out.println("  # Discovery");
        System.out.println("  i2c.buses                  - List all buses");
        System.out.println("  i2c.detect 0               - Detect devices on bus 0");
        System.out.println("  i2c.addressMap             - Show all devices with addresses");
        System.out.println("  i2c.findDevice eeprom      - Find EEPROM devices");
        System.out.println("  i2c.mapBus 0               - Map all devices on bus 0");
        System.out.println();
        System.out.println("  # Address/Path Conversion");
        System.out.println("  i2c.pathToAddress /sys/bus/i2c/devices/0-0050");
        System.out.println("  i2c.addressToPath 0 0x50   - Get path for bus 0, address 0x50");
        System.out.println();
        System.out.println("  # Reading/Writing");
        System.out.println("  i2c.get 0 0x50 0x00        - Read byte from bus 0, device 0x50, reg 0x00");
        System.out.println("  i2c.dump 0 0x50            - Dump all registers");
        System.out.println("  i2c.eepromRead 0 0x50 256  - Read 256 bytes from EEPROM");
        System.out.println();
        System.out.println("  # Monitoring");
        System.out.println("  i2c.watch 0 0x50 0x00      - Watch register continuously");
        System.out.println();
        System.out.println("  # Driver Management");
        System.out.println("  i2c.bind at24 0-0050       - Bind device to driver");

        System.out.println("\nNote: Most commands require root privileges.");
        System.out.println("Device addresses are typically in hex format (e.g., 0x50).\n");
    }
}
