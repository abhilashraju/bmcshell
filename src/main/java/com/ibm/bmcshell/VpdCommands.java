package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

/**
 * VPD (Vital Product Data) Commands for querying /var/lib/vpd/vpd_inventory.json
 * on the remote BMC machine.
 *
 * The JSON structure is:
 * {
 *   "<eeprom-path>": [
 *     {
 *       "inventoryPath": "...",
 *       "serviceName": "...",
 *       "extraInterfaces": {
 *         "com.ibm.ipzvpd.Location": { "LocationCode": "..." },
 *         "xyz.openbmc_project.Inventory.Item": { "PrettyName": "..." },
 *         ...
 *       },
 *       ...
 *     }
 *   ]
 * }
 */
@ShellComponent
public class VpdCommands extends CommonCommands {

    private static final String VPD_JSON = "/var/lib/vpd/vpd_inventory.json";

    protected VpdCommands() throws IOException {
    }

    // ==================== BASIC FILE COMMANDS ====================

    /**
     * Display the full VPD inventory JSON file.
     *
     * Example: vpd.show
     */
    @ShellMethod(key = "vpd.show", value = "Display the full VPD inventory JSON file")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdShow() {
        scmd(String.format("cat %s", VPD_JSON));
    }

    /**
     * Display the VPD inventory JSON file with pretty formatting (using python3 json.tool).
     *
     * Example: vpd.show-pretty
     */
    @ShellMethod(key = "vpd.show-pretty", value = "Display the VPD inventory JSON file with pretty formatting")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdShowPretty() {
        scmd(String.format("cat %s | python3 -m json.tool 2>/dev/null || cat %s", VPD_JSON, VPD_JSON));
    }

    // ==================== EEPROM PATH QUERIES ====================

    /**
     * List all EEPROM paths (top-level keys) in the VPD inventory JSON.
     *
     * Example: vpd.list-eeprom-paths
     */
    @ShellMethod(key = "vpd.list-eeprom-paths", value = "List all EEPROM paths in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListEepromPaths() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); [print(k) for k in data.keys()]\"",
            VPD_JSON));
    }

    /**
     * Show all inventory entries for a specific EEPROM path.
     *
     * Example: vpd.eeprom-entries --eeprom /sys/bus/i2c/drivers/at24/13-0050/eeprom
     *
     * @param eeprom The EEPROM device path
     */
    @ShellMethod(key = "vpd.eeprom-entries", value = "Show all inventory entries for a specific EEPROM path")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdEepromEntries(
            @ShellOption(value = { "--eeprom", "-e" }) String eeprom) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); entries=data.get('%s', []); " +
            "[print(json.dumps(e, indent=2)) for e in entries]\"",
            VPD_JSON, eeprom));
    }

    // ==================== INVENTORY PATH QUERIES ====================

    /**
     * List all inventory paths across all EEPROM entries.
     *
     * Example: vpd.list-inventory-paths
     */
    @ShellMethod(key = "vpd.list-inventory-paths", value = "List all inventory paths in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListInventoryPaths() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e['inventoryPath']) for entries in data.values() for e in entries if 'inventoryPath' in e]\"",
            VPD_JSON));
    }

    /**
     * Find the EEPROM path and full entry for a given inventory path.
     *
     * Example: vpd.find-inventory-path --path /xyz/openbmc_project/inventory/system/chassis/motherboard/pcieslot4/pcie_card4
     *
     * @param inventoryPath The inventory object path to search for
     */
    @ShellMethod(key = "vpd.find-inventory-path", value = "Find entry by inventory path")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindInventoryPath(
            @ShellOption(value = { "--path", "-p" }) String inventoryPath) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('EEPROM:', k, '\\n', json.dumps(e, indent=2)) " +
            " for k, entries in data.items() for e in entries " +
            " if e.get('inventoryPath','') == '%s']\"",
            VPD_JSON, inventoryPath));
    }

    /**
     * Search for inventory paths matching a pattern (substring match).
     *
     * Example: vpd.search-inventory-path --pattern pcieslot4
     *
     * @param pattern Substring to search for in inventory paths
     */
    @ShellMethod(key = "vpd.search-inventory-path", value = "Search inventory paths by pattern")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSearchInventoryPath(
            @ShellOption(value = { "--pattern", "-p" }) String pattern) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e['inventoryPath']) for entries in data.values() for e in entries " +
            " if '%s' in e.get('inventoryPath','')]\"",
            VPD_JSON, pattern));
    }

    // ==================== LOCATION CODE QUERIES ====================

    /**
     * List all location codes (com.ibm.ipzvpd.Location/LocationCode) in the VPD inventory.
     *
     * Example: vpd.list-location-codes
     */
    @ShellMethod(key = "vpd.list-location-codes", value = "List all location codes in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListLocationCodes() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode',''), " +
            " ' -> ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location')]\"",
            VPD_JSON));
    }

    /**
     * Find inventory entry by location code.
     *
     * Example: vpd.find-location-code --code Ufcs-ND0-P0-C4
     *
     * @param locationCode The location code to search for (e.g., Ufcs-ND0-P0-C4)
     */
    @ShellMethod(key = "vpd.find-location-code", value = "Find inventory entry by location code")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindLocationCode(
            @ShellOption(value = { "--code", "-c" }) String locationCode) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('EEPROM:', k, '\\n', json.dumps(e, indent=2)) " +
            " for k, entries in data.items() for e in entries " +
            " if e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','') == '%s']\"",
            VPD_JSON, locationCode));
    }

    /**
     * Search for location codes matching a pattern (substring match).
     *
     * Example: vpd.search-location-code --pattern ND0-P0-C4
     *
     * @param pattern Substring to search for in location codes
     */
    @ShellMethod(key = "vpd.search-location-code", value = "Search location codes by pattern")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSearchLocationCode(
            @ShellOption(value = { "--pattern", "-p" }) String pattern) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode',''), " +
            " ' -> ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if '%s' in e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','')]\"",
            VPD_JSON, pattern));
    }

    // ==================== PRETTY NAME QUERIES ====================

    /**
     * List all pretty names (xyz.openbmc_project.Inventory.Item/PrettyName) in the VPD inventory.
     *
     * Example: vpd.list-pretty-names
     */
    @ShellMethod(key = "vpd.list-pretty-names", value = "List all pretty names in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPrettyNames() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName',''), " +
            " ' -> ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName')]\"",
            VPD_JSON));
    }

    /**
     * Find inventory entries by pretty name (substring match).
     *
     * Example: vpd.find-pretty-name --name "PCIe"
     *
     * @param name Substring to search for in pretty names
     */
    @ShellMethod(key = "vpd.find-pretty-name", value = "Find inventory entries by pretty name")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindPrettyName(
            @ShellOption(value = { "--name", "-n" }) String name) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName',''), " +
            " ' -> ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if '%s' in e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','')]\"",
            VPD_JSON, name));
    }

    // ==================== INTERFACE QUERIES ====================

    /**
     * List all unique extra interface names across all inventory entries.
     *
     * Example: vpd.list-interfaces
     */
    @ShellMethod(key = "vpd.list-interfaces", value = "List all unique extra interface names in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListInterfaces() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "ifaces=set(); " +
            "[ifaces.update(e.get('extraInterfaces',{}).keys()) for entries in data.values() for e in entries]; " +
            "[print(i) for i in sorted(ifaces)]\"",
            VPD_JSON));
    }

    /**
     * Find all inventory entries that have a specific extra interface.
     *
     * Example: vpd.find-by-interface --interface xyz.openbmc_project.Inventory.Item.PCIeDevice
     *
     * @param interfaceName The interface name to search for
     */
    @ShellMethod(key = "vpd.find-by-interface", value = "Find inventory entries by extra interface name")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindByInterface(
            @ShellOption(value = { "--interface", "-i" }) String interfaceName) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if '%s' in e.get('extraInterfaces',{})]\"",
            VPD_JSON, interfaceName));
    }

    // ==================== CONNECTOR QUERIES ====================

    /**
     * List all connector/port entries (entries with xyz.openbmc_project.Inventory.Item.Connector interface).
     *
     * Example: vpd.list-connectors
     */
    @ShellMethod(key = "vpd.list-connectors", value = "List all connector/port entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListConnectors() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Item.Connector' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    // ==================== PCIe SLOT QUERIES ====================

    /**
     * List all PCIe device entries (entries with xyz.openbmc_project.Inventory.Item.PCIeDevice interface).
     *
     * Example: vpd.list-pcie-devices
     */
    @ShellMethod(key = "vpd.list-pcie-devices", value = "List all PCIe device entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPcieDevices() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Item.PCIeDevice' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    /**
     * List all PCIe slot entries (entries with xyz.openbmc_project.Inventory.Item.PCIeSlot interface).
     *
     * Example: vpd.list-pcie-slots
     */
    @ShellMethod(key = "vpd.list-pcie-slots", value = "List all PCIe slot entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPcieSlots() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Item.PCIeSlot' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    // ==================== DISK BACKPLANE QUERIES ====================

    /**
     * List all disk backplane entries (entries with xyz.openbmc_project.Inventory.Item.DiskBackplane interface).
     *
     * Example: vpd.list-disk-backplanes
     */
    @ShellMethod(key = "vpd.list-disk-backplanes", value = "List all disk backplane entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListDiskBackplanes() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Item.DiskBackplane' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    // ==================== I2C DEVICE QUERIES ====================

    /**
     * List all I2C device entries with their bus and address information.
     *
     * Example: vpd.list-i2c-devices
     */
    @ShellMethod(key = "vpd.list-i2c-devices", value = "List all I2C device entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListI2cDevices() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('Bus:', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Decorator.I2CDevice',{}).get('Bus','N/A'), " +
            " ' Addr:', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Decorator.I2CDevice',{}).get('Address','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Decorator.I2CDevice' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    // ==================== SLOT NUMBER QUERIES ====================

    /**
     * List all entries with slot number information.
     *
     * Example: vpd.list-slots
     */
    @ShellMethod(key = "vpd.list-slots", value = "List all entries with slot number information")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListSlots() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('Slot:', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Decorator.Slot',{}).get('SlotNumber','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Decorator.Slot' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    /**
     * Find inventory entry by slot number.
     *
     * Example: vpd.find-slot --slot 4
     *
     * @param slotNumber The slot number to search for
     */
    @ShellMethod(key = "vpd.find-slot", value = "Find inventory entry by slot number")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindSlot(
            @ShellOption(value = { "--slot", "-s" }) int slotNumber) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('EEPROM:', k, '\\n', json.dumps(e, indent=2)) " +
            " for k, entries in data.items() for e in entries " +
            " if e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Decorator.Slot',{}).get('SlotNumber') == %d]\"",
            VPD_JSON, slotNumber));
    }

    // ==================== CCIN QUERIES ====================

    /**
     * List all entries that have CCIN (Customer Card Identification Number) filters.
     *
     * Example: vpd.list-ccin-entries
     */
    @ShellMethod(key = "vpd.list-ccin-entries", value = "List all entries with CCIN filters in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListCcinEntries() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('inventoryPath',''), ' | CCIN:', e.get('ccin',[])) " +
            " for entries in data.values() for e in entries " +
            " if e.get('ccin')]\"",
            VPD_JSON));
    }

    /**
     * Find inventory entries that match a specific CCIN value.
     *
     * Example: vpd.find-ccin --ccin 2CE2
     *
     * @param ccin The CCIN value to search for
     */
    @ShellMethod(key = "vpd.find-ccin", value = "Find inventory entries by CCIN value")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdFindCcin(
            @ShellOption(value = { "--ccin", "-c" }) String ccin) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('inventoryPath',''), ' | CCIN:', e.get('ccin',[])) " +
            " for entries in data.values() for e in entries " +
            " if '%s' in e.get('ccin',[])]\"",
            VPD_JSON, ccin));
    }

    // ==================== REPLACEABLE ENTRIES QUERIES ====================

    /**
     * List all entries that are replaceable at runtime.
     *
     * Example: vpd.list-replaceable-runtime
     */
    @ShellMethod(key = "vpd.list-replaceable-runtime", value = "List all entries replaceable at runtime")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListReplaceableRuntime() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if e.get('replaceableAtRuntime')]\"",
            VPD_JSON));
    }

    /**
     * List all entries that are replaceable at standby.
     *
     * Example: vpd.list-replaceable-standby
     */
    @ShellMethod(key = "vpd.list-replaceable-standby", value = "List all entries replaceable at standby")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListReplaceableStandby() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if e.get('replaceableAtStandby')]\"",
            VPD_JSON));
    }

    // ==================== CABLE QUERIES ====================

    /**
     * List all cable entries (entries with xyz.openbmc_project.Inventory.Item.Cable interface).
     *
     * Example: vpd.list-cables
     */
    @ShellMethod(key = "vpd.list-cables", value = "List all cable entries in the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListCables() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A'), " +
            " ' | ', e.get('inventoryPath','')) " +
            " for entries in data.values() for e in entries " +
            " if 'xyz.openbmc_project.Inventory.Item.Cable' in e.get('extraInterfaces',{})]\"",
            VPD_JSON));
    }

    // ==================== SUMMARY / STATISTICS ====================

    /**
     * Show a summary of the VPD inventory: count of entries per EEPROM path.
     *
     * Example: vpd.summary
     */
    @ShellMethod(key = "vpd.summary", value = "Show a summary of the VPD inventory")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdSummary() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "total=sum(len(v) for v in data.values()); " +
            "print('Total EEPROM paths:', len(data)); " +
            "print('Total inventory entries:', total); " +
            "[print(' ', k, '->', len(v), 'entries') for k, v in data.items()]\"",
            VPD_JSON));
    }

    /**
     * Show a tabular view of all inventory entries: LocationCode, PrettyName, InventoryPath.
     *
     * Example: vpd.table
     */
    @ShellMethod(key = "vpd.table", value = "Show a tabular view of all VPD inventory entries")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdTable() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "print('{:<30} {:<40} {}'.format('LocationCode','PrettyName','InventoryPath')); " +
            "print('-'*120); " +
            "[print('{:<30} {:<40} {}'.format(" +
            " e.get('extraInterfaces',{}).get('com.ibm.ipzvpd.Location',{}).get('LocationCode','N/A')," +
            " e.get('extraInterfaces',{}).get('xyz.openbmc_project.Inventory.Item',{}).get('PrettyName','N/A')," +
            " e.get('inventoryPath',''))) " +
            " for entries in data.values() for e in entries]\"",
            VPD_JSON));
    }

    // ==================== PRE/POST ACTION QUERIES ====================

    /**
     * List all entries that have preAction defined.
     *
     * Example: vpd.list-preaction-entries
     */
    @ShellMethod(key = "vpd.list-preaction-entries", value = "List all entries with preAction defined")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListPreactionEntries() {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print(e.get('inventoryPath',''), ' | preAction:', list(e.get('preAction',{}).keys())) " +
            " for entries in data.values() for e in entries " +
            " if e.get('preAction')]\"",
            VPD_JSON));
    }

    /**
     * Show the full preAction and postAction details for a given inventory path.
     *
     * Example: vpd.show-actions --path /xyz/openbmc_project/inventory/system/chassis/motherboard/pcieslot4/pcie_card4
     *
     * @param inventoryPath The inventory path to show actions for
     */
    @ShellMethod(key = "vpd.show-actions", value = "Show preAction and postAction for an inventory path")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdShowActions(
            @ShellOption(value = { "--path", "-p" }) String inventoryPath) {
        scmd(String.format(
            "python3 -c \"import json; data=json.load(open('%s')); " +
            "[print('preAction:', json.dumps(e.get('preAction',{}), indent=2), " +
            " '\\npostAction:', json.dumps(e.get('postAction',{}), indent=2), " +
            " '\\npostFailAction:', json.dumps(e.get('postFailAction',{}), indent=2)) " +
            " for entries in data.values() for e in entries " +
            " if e.get('inventoryPath','') == '%s']\"",
            VPD_JSON, inventoryPath));
    }

    // ==================== GPIO QUERIES ====================

    /**
     * List all GPIO pins referenced in preAction/postAction across all entries.
     *
     * Example: vpd.list-gpio-pins
     */
    @ShellMethod(key = "vpd.list-gpio-pins", value = "List all GPIO pins referenced in VPD actions")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdListGpioPins() {
        scmd(String.format(
            "python3 -c \"" +
            "import json; data=json.load(open('%s')); " +
            "pins=set(); " +
            "def collect(d): " +
            "  [collect(v) if isinstance(v,dict) else None for v in d.values()]; " +
            "  [pins.add(d['pin']) if 'pin' in d else None]; " +
            "[collect(e.get('preAction',{})) or collect(e.get('postAction',{})) or collect(e.get('postFailAction',{})) " +
            " for entries in data.values() for e in entries]; " +
            "[print(p) for p in sorted(pins)]\"",
            VPD_JSON));
    }

    // ==================== RAW GREP FALLBACK ====================

    /**
     * Raw grep search in the VPD inventory JSON file.
     *
     * Example: vpd.grep --pattern SLOT4
     *
     * @param pattern The pattern to grep for
     */
    @ShellMethod(key = "vpd.grep", value = "Raw grep search in the VPD inventory JSON file")
    @ShellMethodAvailability("availabilityCheck")
    protected void vpdGrep(
            @ShellOption(value = { "--pattern", "-p" }) String pattern) {
        scmd(String.format("grep -n '%s' %s", pattern, VPD_JSON));
    }
}

// Made with Bob