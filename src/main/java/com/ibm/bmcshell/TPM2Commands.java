package com.ibm.bmcshell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import com.ibm.bmcshell.Utils.Util;
import static com.ibm.bmcshell.ssh.SSHShellClient.runCommandShort;

@ShellComponent
public class TPM2Commands extends CommonCommands {

    @Component
    public static class CapabilityProvider implements ValueProvider {
        private static final List<String> CAPABILITIES = List.of(
                "properties-fixed",
                "properties-variable",
                "algorithms",
                "commands",
                "pcrs",
                "handles-transient",
                "handles-persistent",
                "handles-permanent",
                "handles-pcr",
                "handles-nv-index",
                "handles-loaded-session",
                "handles-saved-session");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return CAPABILITIES.stream()
                    .filter(cap -> cap.startsWith(input))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    @Component
    public static class HierarchyProvider implements ValueProvider {
        private static final List<String> HIERARCHIES = List.of(
                "o", // Owner hierarchy
                "p", // Platform hierarchy
                "e", // Endorsement hierarchy
                "n" // Null hierarchy
        );

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return HIERARCHIES.stream()
                    .filter(h -> h.startsWith(input))
                    .map(h -> new CompletionProposal(h))
                    .collect(Collectors.toList());
        }
    }

    @Component
    public static class HashAlgorithmProvider implements ValueProvider {
        private static final List<String> ALGORITHMS = List.of(
                "sha1",
                "sha256",
                "sha384",
                "sha512",
                "sm3_256");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return ALGORITHMS.stream()
                    .filter(alg -> alg.startsWith(input))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    @Component
    public static class SignatureSchemeProvider implements ValueProvider {
        private static final List<String> SCHEMES = List.of(
                "rsassa",
                "rsapss",
                "ecdsa",
                "ecdaa",
                "ecschnorr",
                "hmac");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return SCHEMES.stream()
                    .filter(scheme -> scheme.startsWith(input))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    @Component
    public static class FormatProvider implements ValueProvider {
        private static final List<String> FORMATS = List.of(
                "pem",
                "der",
                "tss");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return FORMATS.stream()
                    .filter(fmt -> fmt.startsWith(input))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    @Component
    public static class NvAttributesProvider implements ValueProvider {
        private static final List<String> ATTRIBUTES = List.of(
                "ownerread",
                "ownerwrite",
                "authread",
                "authwrite",
                "policyread",
                "policywrite",
                "ppread",
                "ppwrite",
                "writedefine",
                "writelocked",
                "readlocked",
                "orderly",
                "clear_stclear",
                "readstclear",
                "writeall",
                "no_da");

        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            String input = context.currentWordUpToCursor().toLowerCase();
            return ATTRIBUTES.stream()
                    .filter(attr -> attr.startsWith(input))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());
        }
    }

    protected TPM2Commands() throws IOException {
    }

    private String executeTpm2Command(String command) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd,
                    "sudo " + command + " 2>&1");
            return outputStream.toString();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    @ShellMethod(key = "tpm2.getcap", value = "Get TPM capabilities - eg: tpm2.getcap properties-fixed")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2GetCap(
            @ShellOption(value = { "--capability",
                    "-c" }, defaultValue = "properties-fixed", valueProvider = CapabilityProvider.class) String capability) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Get Capability: ") + ColorPrinter.yellow(capability));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_getcap " + capability);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.startup", value = "Startup TPM - eg: tpm2.startup [--clear]")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Startup(
            @ShellOption(value = { "--clear", "-c" }, defaultValue = "false") boolean clear) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Startup"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String command = clear ? "tpm2_startup -c" : "tpm2_startup";
        String result = executeTpm2Command(command);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.selftest", value = "Run TPM self-test - eg: tpm2.selftest [--fulltest]")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2SelfTest(
            @ShellOption(value = { "--fulltest", "-f" }, defaultValue = "false") boolean fullTest) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Self Test"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String command = fullTest ? "tpm2_selftest -f" : "tpm2_selftest";
        String result = executeTpm2Command(command);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.pcrread", value = "Read PCR values - eg: tpm2.pcrread [--pcr 0]")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2PcrRead(
            @ShellOption(value = { "--pcr", "-p" }, defaultValue = "") String pcr) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 PCR Read"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String command = pcr.isEmpty() ? "tpm2_pcrread" : "tpm2_pcrread sha256:" + pcr;
        String result = executeTpm2Command(command);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.getrandom", value = "Get random bytes - eg: tpm2.getrandom --bytes 32")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2GetRandom(
            @ShellOption(value = { "--bytes", "-b" }, defaultValue = "32") int bytes) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Get Random"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_getrandom --hex " + bytes);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.clear", value = "Clear TPM (requires physical presence)")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Clear() {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.red("  WARNING: TPM2 Clear - This will erase all TPM data!"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_clear");
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.createprimary", value = "Create primary key - eg: tpm2.createprimary --hierarchy o")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2CreatePrimary(
            @ShellOption(value = { "--hierarchy",
                    "-H" }, defaultValue = "o", valueProvider = HierarchyProvider.class) String hierarchy,
            @ShellOption(value = { "--context", "-c" }, defaultValue = "primary.ctx") String context) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Create Primary Key"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_createprimary -C %s -c /tmp/%s", hierarchy, context));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.create", value = "Create key - eg: tpm2.create --parent primary.ctx")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Create(
            @ShellOption(value = { "--parent", "-C" }, defaultValue = "primary.ctx") String parent,
            @ShellOption(value = { "--public", "-u" }, defaultValue = "key.pub") String publicKey,
            @ShellOption(value = { "--private", "-r" }, defaultValue = "key.priv") String privateKey) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Create Key"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_create -C /tmp/%s -u /tmp/%s -r /tmp/%s", parent, publicKey, privateKey));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.load", value = "Load key - eg: tpm2.load --parent primary.ctx")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Load(
            @ShellOption(value = { "--parent", "-C" }, defaultValue = "primary.ctx") String parent,
            @ShellOption(value = { "--public", "-u" }, defaultValue = "key.pub") String publicKey,
            @ShellOption(value = { "--private", "-r" }, defaultValue = "key.priv") String privateKey,
            @ShellOption(value = { "--context", "-c" }, defaultValue = "key.ctx") String context) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Load Key"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_load -C /tmp/%s -u /tmp/%s -r /tmp/%s -c /tmp/%s",
                        parent, publicKey, privateKey, context));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.readpublic", value = "Read public portion of key - eg: tpm2.readpublic --context key.ctx")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2ReadPublic(
            @ShellOption(value = { "--context", "-c" }, defaultValue = "key.ctx") String context) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Read Public"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_readpublic -c /tmp/" + context);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.hash", value = "Hash data - eg: tpm2.hash --data 'hello world'")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Hash(
            @ShellOption(value = { "--data", "-d" }) String data,
            @ShellOption(value = { "--algorithm",
                    "-g" }, defaultValue = "sha256", valueProvider = HashAlgorithmProvider.class) String algorithm) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Hash"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("echo -n '%s' | tpm2_hash -g %s", data, algorithm));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.sign", value = "Sign data - eg: tpm2.sign --context key.ctx --message msg.txt")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Sign(
            @ShellOption(value = { "--context", "-c" }, defaultValue = "key.ctx") String context,
            @ShellOption(value = { "--message", "-m" }) String message,
            @ShellOption(value = { "--signature", "-s" }, defaultValue = "sig.dat") String signature) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Sign"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_sign -c /tmp/%s -m %s -s /tmp/%s", context, message, signature));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.verifysignature", value = "Verify signature - eg: tpm2.verifysignature --key key.pub --message msg.txt")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2VerifySignature(
            @ShellOption(value = { "--key", "-u" }) String publicKey,
            @ShellOption(value = { "--message", "-m" }) String message,
            @ShellOption(value = { "--signature", "-s" }, defaultValue = "sig.dat") String signature) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Verify Signature"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_verifysignature -u /tmp/%s -m %s -s /tmp/%s", publicKey, message, signature));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvdefine", value = "Define NV index - eg: tpm2.nvdefine --index 0x1500001 --size 32")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvDefine(
            @ShellOption(value = { "--index", "-i" }) String index,
            @ShellOption(value = { "--size", "-s" }, defaultValue = "32") int size) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Define"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_nvdefine %s -C o -s %d -a 'ownerwrite|ownerread|authread'", index, size));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvwrite", value = "Write to NV - eg: tpm2.nvwrite --index 0x1500001 --data 'hello'")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvWrite(
            @ShellOption(value = { "--index", "-i" }) String index,
            @ShellOption(value = { "--data", "-d" }) String data) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Write"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("bash -c \"echo -n '%s' | tpm2_nvwrite %s -C o\"", data, index));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvread", value = "Read from NV - eg: tpm2.nvread --index 0x1500001")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvRead(
            @ShellOption(value = { "--index", "-i" }) String index) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Read"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_nvread " + index + " -C o");
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvundefine", value = "Undefine NV index - eg: tpm2.nvundefine --index 0x1500001")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvUndefine(
            @ShellOption(value = { "--index", "-i" }) String index) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Undefine"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command("tpm2_nvundefine " + index + " -C o");
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.info", value = "Get TPM information summary")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Info() {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Information Summary"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        // Get manufacturer
        System.out.println("\n" + ColorPrinter.yellow("Manufacturer Information:"));
        System.out.println("-".repeat(80));
        String mfg = executeTpm2Command(
                "tpm2_getcap properties-fixed | grep -E 'TPM_PT_MANUFACTURER|TPM_PT_VENDOR_STRING|TPM_PT_FIRMWARE_VERSION'");
        System.out.println(mfg);

        // Get algorithms
        System.out.println("\n" + ColorPrinter.yellow("Supported Algorithms:"));
        System.out.println("-".repeat(80));
        String alg = executeTpm2Command("tpm2_getcap algorithms");
        System.out.println(alg);

        // Get PCR banks
        System.out.println("\n" + ColorPrinter.yellow("PCR Banks:"));
        System.out.println("-".repeat(80));
        String pcr = executeTpm2Command("tpm2_getcap pcrs");
        System.out.println(pcr);

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.status", value = "Check TPM device status")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Status() {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Device Status"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        // Check device files
        System.out.println("\n" + ColorPrinter.yellow("Device Files:"));
        System.out.println("-".repeat(80));
        String devices = executeTpm2Command("ls -la /dev/tpm* 2>/dev/null");
        System.out.println(devices);

        // Check sysfs
        System.out.println("\n" + ColorPrinter.yellow("Sysfs Information:"));
        System.out.println("-".repeat(80));
        String sysfs = executeTpm2Command("cat /sys/class/tpm/tpm0/tpm_version_major 2>/dev/null");
        System.out.println("TPM Version: " + sysfs);

        // Check dmesg
        System.out.println("\n" + ColorPrinter.yellow("Kernel Messages:"));
        System.out.println("-".repeat(80));
        String dmesg = executeTpm2Command("dmesg | grep -i tpm | tail -10");
        System.out.println(dmesg);

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.help", value = "Display TPM2 commands help")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2Help(
            @ShellOption(value = { "--command", "-c" }, defaultValue = "") String command) {

        if (!command.isEmpty()) {
            // Show help for specific command
            System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            System.out.println(ColorPrinter.cyan("  TPM2 Command Help: ") + ColorPrinter.yellow(command));
            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

            String result = executeTpm2Command(command + " --help");
            System.out.println(result);

            System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
            return;
        }

        // Show all available commands
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Commands Reference"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        System.out.println("\n" + ColorPrinter.yellow("INFORMATION & STATUS:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.info                    - Get TPM information summary");
        System.out.println("  tpm2.status                  - Check TPM device status");
        System.out.println("  tpm2.getcap [capability]     - Get TPM capabilities");
        System.out.println("                                 Options: properties-fixed, properties-variable,");
        System.out.println("                                          algorithms, commands, pcrs, handles-*");

        System.out.println("\n" + ColorPrinter.yellow("INITIALIZATION & TESTING:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.startup [--clear]       - Startup TPM (use --clear for clear startup)");
        System.out.println("  tpm2.selftest [--fulltest]   - Run TPM self-test");
        System.out.println("  tpm2.clear                   - Clear TPM (WARNING: erases all data!)");

        System.out.println("\n" + ColorPrinter.yellow("PCR OPERATIONS:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.pcrread [--pcr N]       - Read PCR values (all or specific PCR)");

        System.out.println("\n" + ColorPrinter.yellow("KEY MANAGEMENT:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.createprimary           - Create primary key");
        System.out.println("    --hierarchy o              - Owner hierarchy (default)");
        System.out.println("    --context primary.ctx      - Output context file");
        System.out.println();
        System.out.println("  tpm2.create                  - Create key");
        System.out.println("    --parent primary.ctx       - Parent key context");
        System.out.println("    --public key.pub           - Public key output");
        System.out.println("    --private key.priv         - Private key output");
        System.out.println();
        System.out.println("  tpm2.load                    - Load key into TPM");
        System.out.println("    --parent primary.ctx       - Parent key context");
        System.out.println("    --public key.pub           - Public key file");
        System.out.println("    --private key.priv         - Private key file");
        System.out.println("    --context key.ctx          - Output context");
        System.out.println();
        System.out.println("  tpm2.readpublic              - Read public portion of key");
        System.out.println("    --context key.ctx          - Key context");

        System.out.println("\n" + ColorPrinter.yellow("CRYPTOGRAPHIC OPERATIONS:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.getrandom               - Generate random bytes");
        System.out.println("    --bytes 32                 - Number of bytes (default: 32)");
        System.out.println();
        System.out.println("  tpm2.hash                    - Hash data");
        System.out.println("    --data 'text'              - Data to hash");
        System.out.println("    --algorithm sha256         - Hash algorithm (default: sha256)");
        System.out.println();
        System.out.println("  tpm2.sign                    - Sign data");
        System.out.println("    --context key.ctx          - Signing key context");
        System.out.println("    --message msg.txt          - Message file to sign");
        System.out.println("    --signature sig.dat        - Output signature file");
        System.out.println();
        System.out.println("  tpm2.verifysignature         - Verify signature");
        System.out.println("    --key key.pub              - Public key file");
        System.out.println("    --message msg.txt          - Message file");
        System.out.println("    --signature sig.dat        - Signature file");

        System.out.println("\n" + ColorPrinter.yellow("NV (NON-VOLATILE) STORAGE:"));
        System.out.println("-".repeat(80));
        System.out.println("  tpm2.nvdefine                - Define NV index");
        System.out.println("    --index 0x1500001          - NV index address");
        System.out.println("    --size 32                  - Size in bytes");
        System.out.println();
        System.out.println("  tpm2.nvwrite                 - Write to NV index");
        System.out.println("    --index 0x1500001          - NV index address");
        System.out.println("    --data 'text'              - Data to write");
        System.out.println();
        System.out.println("  tpm2.nvread                  - Read from NV index");
        System.out.println("    --index 0x1500001          - NV index address");
        System.out.println();
        System.out.println("  tpm2.nvundefine              - Undefine NV index");
        System.out.println("    --index 0x1500001          - NV index address");

        System.out.println("\n" + ColorPrinter.yellow("EXAMPLES:"));
        System.out.println("-".repeat(80));
        System.out.println("  # Get TPM information");
        System.out.println("  tpm2.info");
        System.out.println();
        System.out.println("  # Initialize and test TPM");
        System.out.println("  tpm2.startup");
        System.out.println("  tpm2.selftest");
        System.out.println();
        System.out.println("  # Read all PCRs");
        System.out.println("  tpm2.pcrread");
        System.out.println();
        System.out.println("  # Generate 32 random bytes");
        System.out.println("  tpm2.getrandom --bytes 32");
        System.out.println();
        System.out.println("  # Create and use a key");
        System.out.println("  tpm2.createprimary --hierarchy o --context primary.ctx");
        System.out.println("  tpm2.create --parent primary.ctx --public key.pub --private key.priv");
        System.out.println("  tpm2.load --parent primary.ctx --public key.pub --private key.priv --context key.ctx");
        System.out.println();
        System.out.println("  # Get help for specific tpm2-tools command");
        System.out.println("  tpm2.help --command tpm2_getcap");

        System.out.println("\n" + ColorPrinter.yellow("NOTES:"));
        System.out.println("-".repeat(80));
        System.out.println("  - All commands automatically use 'sudo' for proper permissions");
        System.out.println("  - Context files are stored in /tmp/ directory");
        System.out.println("  - Use 'tpm2.help --command <cmd>' for detailed help on specific tpm2-tools commands");
        System.out.println("  - TPM device is typically at /dev/tpm0 or /dev/tpmrm0");

        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.evictcontrol", value = "Make key persistent - eg: tpm2.evictcontrol --context rsa.ctx --handle 0x81010001")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2EvictControl(
            @ShellOption(value = { "--context", "-c" }, defaultValue = "") String context,
            @ShellOption(value = { "--handle" }, defaultValue = "0x81010001") String handle,
            @ShellOption(value = { "--remove", "-r" }, defaultValue = "false") boolean remove) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Evict Control"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        if (remove) {
            // Remove existing handle (ignore errors if handle doesn't exist)
            String removeResult = executeTpm2Command(
                    String.format("tpm2_evictcontrol -C o -c %s 2>/dev/null || true", handle));
            System.out.println("Removing handle " + handle + " (if exists)");
            if (!removeResult.trim().isEmpty()) {
                System.out.println(removeResult);
            }
        } else if (!context.isEmpty()) {
            // Create persistent handle from context
            String result = executeTpm2Command(
                    String.format("tpm2_evictcontrol -C o -c /tmp/%s %s", context, handle));
            System.out.println(result);
        } else {
            System.out.println("Error: Either --remove or --context must be specified");
        }

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.readpublic.export", value = "Export public key to PEM - eg: tpm2.readpublic.export --handle 0x81010001 --output public.pem")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2ReadPublicExport(
            @ShellOption(value = { "--handle" }, defaultValue = "0x81010001") String handle,
            @ShellOption(value = { "--output", "-o" }, defaultValue = "public.pem") String output,
            @ShellOption(value = { "--format",
                    "-f" }, defaultValue = "pem", valueProvider = FormatProvider.class) String format) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Read Public - Export to PEM"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_readpublic -c %s -f %s -o /tmp/%s", handle, format, output));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.sign.advanced", value = "Sign with advanced options - eg: tpm2.sign.advanced --handle 0x81010001 --hash test.hash --signature test.sig")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2SignAdvanced(
            @ShellOption(value = { "--handle", "-c" }, defaultValue = "0x81010001") String handle,
            @ShellOption(value = { "--hash", "-m" }) String hashFile,
            @ShellOption(value = { "--signature", "-o" }, defaultValue = "test.sig") String signature,
            @ShellOption(value = { "--algorithm",
                    "-g" }, defaultValue = "sha256", valueProvider = HashAlgorithmProvider.class) String algorithm,
            @ShellOption(value = { "--scheme",
                    "-s" }, defaultValue = "rsassa", valueProvider = SignatureSchemeProvider.class) String scheme) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Sign (Advanced)"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_sign -c %s -g %s -s %s -o /tmp/%s /tmp/%s",
                        handle, algorithm, scheme, signature, hashFile));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.verifysignature.advanced", value = "Verify signature with handle - eg: tpm2.verifysignature.advanced --handle 0x81010001 --hash test.hash --signature test.sig")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2VerifySignatureAdvanced(
            @ShellOption(value = { "--handle", "-c" }, defaultValue = "0x81010001") String handle,
            @ShellOption(value = { "--hash", "-m" }) String hashFile,
            @ShellOption(value = { "--signature", "-s" }, defaultValue = "test.sig") String signature,
            @ShellOption(value = { "--algorithm",
                    "-g" }, defaultValue = "sha256", valueProvider = HashAlgorithmProvider.class) String algorithm) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 Verify Signature (Advanced)"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format(
                        "tpm2_verifysignature -c %s -g %s -m /tmp/%s -s /tmp/%s && echo 'Signature verification: SUCCESS' || echo 'Signature verification: FAILED'",
                        handle, algorithm, hashFile, signature));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvdefine.advanced", value = "Define NV index with attributes - eg: tpm2.nvdefine.advanced --index 0x1500001 --size 2048 --attributes 'ownerread|ownerwrite'")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvDefineAdvanced(
            @ShellOption(value = { "--index", "-i" }) String index,
            @ShellOption(value = { "--size", "-s" }, defaultValue = "2048") int size,
            @ShellOption(value = { "--attributes",
                    "-a" }, defaultValue = "ownerread|ownerwrite", valueProvider = NvAttributesProvider.class) String attributes) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Define (Advanced)"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_nvdefine %s -C o -s %d -a '%s'", index, size, attributes));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvwrite.file", value = "Write file to NV - eg: tpm2.nvwrite.file --index 0x1500001 --input device.crt")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvWriteFile(
            @ShellOption(value = { "--index", "-i" }) String index,
            @ShellOption(value = { "--input", "-f" }) String inputFile) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Write from File"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String result = executeTpm2Command(
                String.format("tpm2_nvwrite %s -C o -i /tmp/%s", index, inputFile));
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "tpm2.nvread.tofile", value = "Read NV to file - eg: tpm2.nvread.tofile --index 0x1500001 --output verify.crt --size 1363")
    @ShellMethodAvailability("availabilityCheck")
    void tpm2NvReadToFile(
            @ShellOption(value = { "--index", "-i" }) String index,
            @ShellOption(value = { "--output", "-o" }) String outputFile,
            @ShellOption(value = { "--size", "-s" }, defaultValue = "0") int size) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  TPM2 NV Read to File"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        String command = size > 0
                ? String.format("tpm2_nvread %s -C o -s %d -o /tmp/%s", index, size, outputFile)
                : String.format("tpm2_nvread %s -C o -o /tmp/%s", index, outputFile);

        String result = executeTpm2Command(command);
        System.out.println(result);

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }

    @ShellMethod(key = "scmd", value = "Execute shell command - eg: scmd 'ls -la /tmp'")
    @ShellMethodAvailability("availabilityCheck")
    void shellCommand(
            @ShellOption(value = { "--command", "-c" }) String command) {
        System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        System.out.println(ColorPrinter.cyan("  Execute Shell Command"));
        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            runCommandShort(outputStream, Util.fullMachineName(machine), userName, passwd, command);
            String result = outputStream.toString();
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }

        System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
    }
}
