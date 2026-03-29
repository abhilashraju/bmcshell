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
        public static class TpmObjectTypeProvider implements ValueProvider {
                private static final List<String> OBJECT_TYPES = List.of(
                                "TPM2B_PUBLIC",
                                "TPM2B_PRIVATE",
                                "TPMS_CONTEXT",
                                "TPMS_ATTEST",
                                "TPMT_PUBLIC",
                                "TPM2B_NAME",
                                "TPM2B_CREATION_DATA",
                                "TPMS_CREATION_DATA");

                @Override
                public List<CompletionProposal> complete(CompletionContext context) {
                        String input = context.currentWordUpToCursor().toUpperCase();
                        return OBJECT_TYPES.stream()
                                        .filter(type -> type.startsWith(input))
                                        .map(CompletionProposal::new)
                                        .collect(Collectors.toList());
                }
        }

        @Component
        public static class KeyAlgorithmProvider implements ValueProvider {
                private static final List<String> KEY_ALGORITHMS = List.of(
                                "rsa",
                                "rsa:rsassa",
                                "rsa:rsassa:sha256",
                                "rsa:rsassa:sha384",
                                "rsa:rsassa:sha512",
                                "rsa:rsapss",
                                "rsa:rsapss:sha256",
                                "rsa:rsapss:sha384",
                                "rsa:rsapss:sha512",
                                "rsa:null",
                                "ecc",
                                "ecc:ecdsa",
                                "ecc:ecdsa:sha256",
                                "ecc:ecdsa:sha384",
                                "ecc:ecdsa:sha512",
                                "ecc:ecdaa",
                                "ecc:ecschnorr",
                                "keyedhash",
                                "keyedhash:hmac",
                                "keyedhash:hmac:sha256",
                                "keyedhash:xor",
                                "symcipher",
                                "symcipher:aes",
                                "symcipher:aes128cfb",
                                "symcipher:aes256cfb");

                @Override
                public List<CompletionProposal> complete(CompletionContext context) {
                        String input = context.currentWordUpToCursor().toLowerCase();
                        return KEY_ALGORITHMS.stream()
                                        .filter(alg -> alg.startsWith(input))
                                        .map(CompletionProposal::new)
                                        .collect(Collectors.toList());
                }
        }

        @Component
        public static class ObjectAttributesProvider implements ValueProvider {
                private static final List<String> ATTRIBUTES = List.of(
                                "fixedtpm",
                                "stclear",
                                "fixedparent",
                                "sensitivedataorigin",
                                "userwithauth",
                                "adminwithpolicy",
                                "noda",
                                "encryptedduplication",
                                "restricted",
                                "decrypt",
                                "sign",
                                "sign|decrypt",
                                "decrypt|sign",
                                "fixedtpm|fixedparent|sensitivedataorigin|userwithauth|decrypt|sign",
                                "fixedtpm|fixedparent|sensitivedataorigin|userwithauth|restricted|decrypt",
                                "fixedtpm|fixedparent|sensitivedataorigin|userwithauth|restricted|sign");

                @Override
                public List<CompletionProposal> complete(CompletionContext context) {
                        String input = context.currentWordUpToCursor().toLowerCase();
                        return ATTRIBUTES.stream()
                                        .filter(attr -> attr.contains(input))
                                        .map(CompletionProposal::new)
                                        .collect(Collectors.toList());
                }
        }

        @Component
        public static class NvAttributesProvider implements ValueProvider {
                private static final List<String> NV_ATTRIBUTES = List.of(
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
                        return NV_ATTRIBUTES.stream()
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

        @ShellMethod(key = "tpm2.create", value = "Create key - eg: tpm2.create --parent primary.ctx --key-algorithm rsa:rsassa:sha256 --attributes 'fixedtpm|fixedparent|sensitivedataorigin|userwithauth|sign|decrypt' --auth mypassword")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2Create(
                        @ShellOption(value = { "--parent", "-C" }, defaultValue = "primary.ctx") String parent,
                        @ShellOption(value = { "--public", "-u" }, defaultValue = "key.pub") String publicKey,
                        @ShellOption(value = { "--private", "-r" }, defaultValue = "key.priv") String privateKey,
                        @ShellOption(value = { "--attributes",
                                        "-a" }, defaultValue = ShellOption.NULL, valueProvider = ObjectAttributesProvider.class) String attributes,
                        @ShellOption(value = { "--auth", "-p" }, defaultValue = ShellOption.NULL) String auth,
                        @ShellOption(value = { "--hash-algorithm",
                                        "-g" }, defaultValue = ShellOption.NULL, valueProvider = HashAlgorithmProvider.class) String hashAlg,
                        @ShellOption(value = { "--key-algorithm",
                                        "-G" }, defaultValue = ShellOption.NULL, valueProvider = KeyAlgorithmProvider.class) String keyAlg) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Create Key"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                StringBuilder cmd = new StringBuilder(
                                String.format("tpm2_create -C /tmp/%s -u /tmp/%s -r /tmp/%s", parent, publicKey,
                                                privateKey));

                if (attributes != null) {
                        cmd.append(" -a '").append(attributes).append("'");
                }
                if (auth != null) {
                        cmd.append(" -p '").append(auth).append("'");
                }
                if (hashAlg != null) {
                        cmd.append(" -g ").append(hashAlg);
                }
                if (keyAlg != null) {
                        cmd.append(" -G ").append(keyAlg);
                }
                System.out.println(cmd);
                String result = executeTpm2Command(cmd.toString());
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

        @ShellMethod(key = "tpm2.readpublic", value = "Read public portion of key - eg: tpm2.readpublic -c 0x81010001 -f pem -o public.pem")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2ReadPublic(
                        @ShellOption(value = { "--context", "-c" }, defaultValue = "key.ctx") String context,
                        @ShellOption(value = { "--format",
                                        "-f" }, defaultValue = "", valueProvider = FormatProvider.class) String format,
                        @ShellOption(value = { "--output", "-o" }, defaultValue = "") String output) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Read Public"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                // Build command based on whether context looks like a handle or file
                String contextPath = context.startsWith("0x") ? context : "/tmp/" + context;

                StringBuilder cmd = new StringBuilder("tpm2_readpublic -c " + contextPath);

                if (!format.isEmpty()) {
                        cmd.append(" -f ").append(format);
                }

                if (!output.isEmpty()) {
                        cmd.append(" -o /tmp/").append(output);
                }

                String result = executeTpm2Command(cmd.toString());
                System.out.println(result);

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.readcontext", value = "Read context from TPM handle and save to file - eg: tpm2.readcontext -c 0x81010001 --context-out key.ctx --name-out key.name")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2ReadContext(
                        @ShellOption(value = { "--handle", "-c" }, defaultValue = "0x81010001") String handle,
                        @ShellOption(value = { "--context-out", "-t" }, defaultValue = "") String contextOut,
                        @ShellOption(value = { "--name-out", "-n" }, defaultValue = "") String nameOut,
                        @ShellOption(value = { "--format",
                                        "-f" }, defaultValue = "", valueProvider = FormatProvider.class) String format,
                        @ShellOption(value = { "--output", "-o" }, defaultValue = "") String output) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Read Context from Handle"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                StringBuilder cmd = new StringBuilder("tpm2_readpublic -c " + handle);

                if (!contextOut.isEmpty()) {
                        cmd.append(" -t /tmp/").append(contextOut);
                }

                if (!nameOut.isEmpty()) {
                        cmd.append(" -n /tmp/").append(nameOut);
                }

                if (!format.isEmpty()) {
                        cmd.append(" -f ").append(format);
                }

                if (!output.isEmpty()) {
                        cmd.append(" -o /tmp/").append(output);
                }

                String result = executeTpm2Command(cmd.toString());
                System.out.println(result);

                if (!contextOut.isEmpty()) {
                        System.out.println(ColorPrinter.green("\n✓ Context saved to: /tmp/" + contextOut));
                }
                if (!nameOut.isEmpty()) {
                        System.out.println(ColorPrinter.green("✓ Name saved to: /tmp/" + nameOut));
                }
                if (!output.isEmpty()) {
                        System.out.println(ColorPrinter.green("✓ Public key saved to: /tmp/" + output));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.print", value = "Print TPM object information - eg: tpm2.print -t TPMS_CONTEXT -f context.ctx or tpm2.print -t TPM2B_PUBLIC -f key.pub or tpm2.print --handle 0x81010001")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2Print(
                        @ShellOption(value = { "--type",
                                        "-t" }, defaultValue = "TPMS_CONTEXT", valueProvider = TpmObjectTypeProvider.class) String type,
                        @ShellOption(value = { "--file",
                                        "-f" }, defaultValue = "", valueProvider = RemoteFileCompleter.class) String file,
                        @ShellOption(value = { "--handle",
                                        "-c" }, defaultValue = "") String handle) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Print Object Information"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                String result;

                // If handle is provided, read from TPM and save to temp file, then print
                if (!handle.isEmpty()) {
                        String tempFile = "temp_print_" + System.currentTimeMillis();

                        if (type.equals("TPM2B_PUBLIC") || type.equals("TPMT_PUBLIC")) {
                                // Read public key from handle
                                System.out.println(ColorPrinter
                                                .yellow("Reading public key from handle " + handle + "..."));
                                String readCmd = String.format("tpm2_readpublic -c %s -o /tmp/%s.pub", handle,
                                                tempFile);
                                executeTpm2Command(readCmd);
                                result = executeTpm2Command(
                                                String.format("tpm2_print -t %s /tmp/%s.pub", type, tempFile));
                                executeTpm2Command("rm -f /tmp/" + tempFile + ".pub");
                        } else if (type.equals("TPMS_CONTEXT")) {
                                // Read context from handle
                                System.out.println(
                                                ColorPrinter.yellow("Reading context from handle " + handle + "..."));
                                String readCmd = String.format("tpm2_readpublic -c %s -t /tmp/%s.ctx", handle,
                                                tempFile);
                                executeTpm2Command(readCmd);
                                result = executeTpm2Command(
                                                String.format("tpm2_print -t %s /tmp/%s.ctx", type, tempFile));
                                executeTpm2Command("rm -f /tmp/" + tempFile + ".ctx");
                        } else {
                                result = ColorPrinter.red("Error: Cannot read type " + type
                                                + " from TPM handle. Only TPM2B_PUBLIC, TPMT_PUBLIC, and TPMS_CONTEXT are supported for handles.");
                        }
                } else if (!file.isEmpty()) {
                        // Print from file
                        String filePath = file.startsWith("/") ? file : "/tmp/" + file;
                        result = executeTpm2Command(String.format("tpm2_print -t %s %s", type, filePath));
                } else {
                        result = ColorPrinter.red("Error: Either --file or --handle must be provided");
                }

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
                                String.format("tpm2_verifysignature -u /tmp/%s -m %s -s /tmp/%s", publicKey, message,
                                                signature));
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
                                String.format("tpm2_nvdefine %s -C o -s %d -a 'ownerwrite|ownerread|authread'", index,
                                                size));
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
                        System.out.println("\n"
                                        + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                        System.out.println(ColorPrinter.cyan("  TPM2 Command Help: ") + ColorPrinter.yellow(command));
                        System.out.println(
                                        ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                        String result = executeTpm2Command(command + " --help");
                        System.out.println(result);

                        System.out.println(
                                        ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
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

                System.out.println("\n" + ColorPrinter.yellow("CERTIFICATE MANAGEMENT:"));
                System.out.println("-".repeat(80));
                System.out.println(
                                "  tpm2.loadexternal.cert       - Load certificate as TPM object (OpenSSL compatible)");
                System.out.println("    --cert device.crt          - Certificate file or direct PEM content");
                System.out.println("    --handle 0x81000001        - Persistent handle (default: 0x81000001)");
                System.out.println("    --hierarchy o              - Hierarchy (default: owner)");
                System.out.println();
                System.out.println("  tpm2.storecert.nv            - Store certificate in NV index");
                System.out.println("    --cert device.crt          - Certificate file or direct PEM content");
                System.out.println("    --index 0x1500001          - NV index (default: 0x1500001)");
                System.out.println();
                System.out.println("  tpm2.migrate.nv2obj          - Migrate certificate from NV to TPM object");
                System.out.println("    --nv-index 0x1500001       - Source NV index");
                System.out.println("    --handle 0x81000001        - Target persistent handle");
                System.out.println("    --hierarchy o              - Hierarchy (default: owner)");

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
                System.out.println(
                                "  tpm2.load --parent primary.ctx --public key.pub --private key.priv --context key.ctx");
                System.out.println();
                System.out.println("  # Get help for specific tpm2-tools command");
                System.out.println("  tpm2.help --command tpm2_getcap");
                System.out.println();
                System.out.println("  # Load certificate as TPM object (for OpenSSL TPM2 provider)");
                System.out.println("  tpm2.loadexternal.cert --cert /tmp/device.crt --handle 0x81000001");
                System.out.println();
                System.out.println("  # Migrate existing NV certificate to TPM object");
                System.out.println("  tpm2.migrate.nv2obj --nv-index 0x1500001 --handle 0x81000001");
                System.out.println();
                System.out.println("  # Store certificate directly from PEM string");
                System.out.println(
                                "  tpm2.loadexternal.cert --cert '-----BEGIN CERTIFICATE-----...' --handle 0x81000001");

                System.out.println("\n" + ColorPrinter.yellow("NOTES:"));
                System.out.println("-".repeat(80));
                System.out.println("  - All commands automatically use 'sudo' for proper permissions");
                System.out.println("  - Context files are stored in /tmp/ directory");
                System.out.println(
                                "  - Use 'tpm2.help --command <cmd>' for detailed help on specific tpm2-tools commands");
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

        @ShellMethod(key = "tpm2.loadexternal.cert", value = "Store certificate in NV and export for OpenSSL - eg: tpm2.loadexternal.cert --cert device.crt --index 0x1500001")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2LoadExternalCert(
                        @ShellOption(value = { "--cert", "-c" }) String certInput,
                        @ShellOption(value = { "--index", "-i" }, defaultValue = "0x1500001") String nvIndex,
                        @ShellOption(value = { "--export-path",
                                        "-e" }, defaultValue = "/etc/ssl/certs/tpm-cert.pem") String exportPath) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Store Certificate in NV"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Check if certInput is a file or direct certificate content
                        String certFile;
                        boolean isDirectContent = certInput.contains("BEGIN CERTIFICATE");

                        if (isDirectContent) {
                                // Write certificate content to temporary file
                                certFile = "temp_cert_" + System.currentTimeMillis() + ".pem";
                                String writeCmd = String.format("cat > /tmp/%s << 'EOF'\n%s\nEOF", certFile, certInput);
                                executeTpm2Command(writeCmd);
                                System.out.println("Certificate content written to temporary file: /tmp/" + certFile);
                        } else {
                                // Assume it's a filename
                                certFile = certInput;
                                System.out.println("Using certificate file: /tmp/" + certFile);
                        }

                        // Get certificate size
                        String sizeResult = executeTpm2Command("wc -c < /tmp/" + certFile);
                        int certSize = Integer.parseInt(sizeResult.trim());
                        System.out.println("Certificate size: " + certSize + " bytes");

                        // Undefine existing index if it exists (ignore errors)
                        executeTpm2Command(String.format("tpm2_nvundefine %s -C o 2>/dev/null || true", nvIndex));

                        // Define NV index with appropriate size and attributes
                        String defineResult = executeTpm2Command(
                                        String.format("tpm2_nvdefine %s -C o -s %d -a 'ownerread|ownerwrite|authread'",
                                                        nvIndex, certSize));
                        System.out.println(defineResult);

                        // Write certificate to NV index
                        String writeResult = executeTpm2Command(
                                        String.format("tpm2_nvwrite %s -C o -i /tmp/%s", nvIndex, certFile));
                        System.out.println(writeResult);

                        // Export certificate to filesystem for OpenSSL access
                        String exportResult = executeTpm2Command(
                                        String.format("sudo tpm2_nvread %s -C o -o %s", nvIndex, exportPath));
                        System.out.println(exportResult);

                        // Verify the exported certificate
                        String verifyResult = executeTpm2Command(
                                        String.format("openssl x509 -in %s -text -noout | head -10", exportPath));
                        System.out.println("\nCertificate verification:");
                        System.out.println(verifyResult);

                        System.out.println(ColorPrinter.green("\n✓ Certificate stored successfully!"));
                        System.out.println(ColorPrinter.yellow("  NV Index: " + nvIndex));
                        System.out.println(ColorPrinter.yellow("  Exported to: " + exportPath));
                        System.out.println(ColorPrinter.yellow("  Use in OpenSSL: file:" + exportPath));
                        System.out
                                        .println(ColorPrinter.yellow(
                                                        "\n  Note: OpenSSL TPM2 provider cannot directly access NV indices."));
                        System.out.println(
                                        ColorPrinter.yellow(
                                                        "  The certificate has been exported to the filesystem for OpenSSL use."));

                        // Cleanup temporary file if we created it
                        if (isDirectContent) {
                                executeTpm2Command("rm -f /tmp/" + certFile);
                        }

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error: " + e.getMessage()));
                        e.printStackTrace();
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.storecert.nv", value = "Store certificate in NV index - eg: tpm2.storecert.nv --cert device.crt --index 0x1500001")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2StoreCertNv(
                        @ShellOption(value = { "--cert", "-c" }) String certInput,
                        @ShellOption(value = { "--index", "-i" }, defaultValue = "0x1500001") String index) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Store Certificate in NV Index"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Check if certInput is a file or direct certificate content
                        String certFile;
                        boolean isDirectContent = certInput.contains("BEGIN CERTIFICATE");

                        if (isDirectContent) {
                                // Write certificate content to temporary file
                                certFile = "temp_cert_" + System.currentTimeMillis() + ".pem";
                                String writeCmd = String.format("cat > /tmp/%s << 'EOF'\n%s\nEOF", certFile, certInput);
                                executeTpm2Command(writeCmd);
                                System.out.println("Certificate content written to temporary file: /tmp/" + certFile);
                        } else {
                                // Assume it's a filename
                                certFile = certInput;
                                System.out.println("Using certificate file: /tmp/" + certFile);
                        }

                        // Get certificate size
                        String sizeResult = executeTpm2Command("wc -c < /tmp/" + certFile);
                        int certSize = Integer.parseInt(sizeResult.trim());
                        System.out.println("Certificate size: " + certSize + " bytes");

                        // Undefine existing index if it exists (ignore errors)
                        executeTpm2Command(String.format("tpm2_nvundefine %s -C o 2>/dev/null || true", index));

                        // Define NV index with appropriate size and attributes
                        String defineResult = executeTpm2Command(
                                        String.format("tpm2_nvdefine %s -C o -s %d -a 'ownerread|ownerwrite|authread'",
                                                        index, certSize));
                        System.out.println(defineResult);

                        // Write certificate to NV index
                        String writeResult = executeTpm2Command(
                                        String.format("tpm2_nvwrite %s -C o -i /tmp/%s", index, certFile));
                        System.out.println(writeResult);

                        System.out.println(ColorPrinter.green("\n✓ Certificate stored in NV index: " + index));
                        System.out.println(ColorPrinter
                                        .yellow("  Note: NV indices cannot be accessed via OpenSSL TPM2 provider"));
                        System.out.println(
                                        ColorPrinter.yellow(
                                                        "  Use tpm2.nvread to retrieve, or tpm2.loadexternal.cert for OpenSSL access"));

                        // Cleanup temporary file if we created it
                        if (isDirectContent) {
                                executeTpm2Command("rm -f /tmp/" + certFile);
                        }

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error: " + e.getMessage()));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.migrate.nv2obj", value = "Migrate certificate from NV to TPM object - eg: tpm2.migrate.nv2obj --nv-index 0x1500001 --handle 0x81000001")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2MigrateNvToObject(
                        @ShellOption(value = { "--nv-index", "-n" }) String nvIndex,
                        @ShellOption(value = { "--handle" }, defaultValue = "0x81000001") String handle,
                        @ShellOption(value = { "--hierarchy",
                                        "-H" }, defaultValue = "o", valueProvider = HierarchyProvider.class) String hierarchy) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Migrate Certificate: NV Index → TPM Object"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Read certificate from NV index to temporary file
                        String tempFile = "migrated_cert_" + System.currentTimeMillis() + ".pem";
                        String readResult = executeTpm2Command(
                                        String.format("tpm2_nvread %s -C o -o /tmp/%s", nvIndex, tempFile));
                        System.out.println("Reading from NV index " + nvIndex + "...");
                        if (!readResult.isEmpty()) {
                                System.out.println(readResult);
                        }

                        // Verify it's a valid certificate
                        String verifyResult = executeTpm2Command(
                                        String.format("openssl x509 -in /tmp/%s -text -noout | head -5", tempFile));
                        System.out.println("\nCertificate preview:");
                        System.out.println(verifyResult);

                        // Convert to DER format
                        String derFile = tempFile.replace(".pem", ".der");
                        String convertResult = executeTpm2Command(
                                        String.format("openssl x509 -in /tmp/%s -outform DER -out /tmp/%s", tempFile,
                                                        derFile));
                        if (!convertResult.isEmpty()) {
                                System.out.println("Conversion output: " + convertResult);
                        }

                        // Load as external object
                        String ctxFile = "cert_" + System.currentTimeMillis() + ".ctx";
                        String loadResult = executeTpm2Command(
                                        String.format("tpm2_loadexternal -C %s -u /tmp/%s -c /tmp/%s", hierarchy,
                                                        derFile, ctxFile));
                        System.out.println(loadResult);

                        // Remove existing handle if present
                        executeTpm2Command(String.format("tpm2_evictcontrol -C %s -c %s 2>/dev/null || true", hierarchy,
                                        handle));

                        // Persist at specified handle
                        String persistResult = executeTpm2Command(
                                        String.format("tpm2_evictcontrol -C %s -c /tmp/%s %s", hierarchy, ctxFile,
                                                        handle));
                        System.out.println(persistResult);

                        System.out.println(ColorPrinter.green("\n✓ Certificate migrated successfully!"));
                        System.out.println(ColorPrinter.yellow("  From NV index: " + nvIndex));
                        System.out.println(ColorPrinter.yellow("  To TPM handle: " + handle));
                        System.out.println(ColorPrinter.yellow("  OpenSSL URI: handle:" + handle));

                        // Cleanup
                        executeTpm2Command("rm -f /tmp/" + tempFile + " /tmp/" + derFile + " /tmp/" + ctxFile);

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error: " + e.getMessage()));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.cert.as.object", value = "Store certificate as TPM object using key wrapping - eg: tpm2.cert.as.object --cert device.crt --handle 0x81000001")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2CertAsObject(
                        @ShellOption(value = { "--cert", "-c" }) String certInput,
                        @ShellOption(value = { "--handle", "-h" }, defaultValue = "0x81000001") String handle,
                        @ShellOption(value = { "--parent-handle",
                                        "-p" }, defaultValue = "0x81000000") String parentHandle) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Store Certificate as TPM Object"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Check if certInput is a file or direct certificate content
                        String certFile;
                        boolean isDirectContent = certInput.contains("BEGIN CERTIFICATE");

                        if (isDirectContent) {
                                // Write certificate content to temporary file
                                certFile = "temp_cert_" + System.currentTimeMillis() + ".pem";
                                String writeCmd = String.format("cat > /tmp/%s << 'EOF'\n%s\nEOF", certFile, certInput);
                                executeTpm2Command(writeCmd);
                                System.out.println("Certificate content written to temporary file: /tmp/" + certFile);
                        } else {
                                // Assume it's a filename
                                certFile = certInput;
                                System.out.println("Using certificate file: /tmp/" + certFile);
                        }

                        // Step 1: Create a primary key if it doesn't exist
                        System.out.println("\n" + ColorPrinter.yellow("Step 1: Ensuring primary key exists..."));
                        String primaryCtx = "primary_" + System.currentTimeMillis() + ".ctx";

                        // Try to read existing primary, if not create one
                        String readPrimary = executeTpm2Command(
                                        String.format("tpm2_readpublic -c %s -o /tmp/%s 2>/dev/null || " +
                                                        "tpm2_createprimary -C o -c /tmp/%s",
                                                        parentHandle, primaryCtx, primaryCtx));
                        System.out.println(readPrimary);

                        // Step 2: Create a sealing object with the certificate data
                        System.out.println("\n"
                                        + ColorPrinter.yellow("Step 2: Creating sealed object with certificate..."));
                        String sealPub = "cert_seal_" + System.currentTimeMillis() + ".pub";
                        String sealPriv = "cert_seal_" + System.currentTimeMillis() + ".priv";

                        String createResult = executeTpm2Command(
                                        String.format("tpm2_create -C /tmp/%s -i /tmp/%s -u /tmp/%s -r /tmp/%s -L policy.dat || "
                                                        +
                                                        "tpm2_create -C /tmp/%s -i /tmp/%s -u /tmp/%s -r /tmp/%s",
                                                        primaryCtx, certFile, sealPub, sealPriv,
                                                        primaryCtx, certFile, sealPub, sealPriv));
                        System.out.println(createResult);

                        // Step 3: Load the sealed object
                        System.out.println("\n" + ColorPrinter.yellow("Step 3: Loading sealed object..."));
                        String sealCtx = "cert_seal_" + System.currentTimeMillis() + ".ctx";
                        String loadResult = executeTpm2Command(
                                        String.format("tpm2_load -C /tmp/%s -u /tmp/%s -r /tmp/%s -c /tmp/%s",
                                                        primaryCtx, sealPub, sealPriv, sealCtx));
                        System.out.println(loadResult);

                        // Step 4: Make it persistent
                        System.out.println("\n" + ColorPrinter.yellow("Step 4: Making object persistent..."));
                        // Remove existing handle if present
                        executeTpm2Command(String.format("tpm2_evictcontrol -C o -c %s 2>/dev/null || true", handle));

                        String persistResult = executeTpm2Command(
                                        String.format("tpm2_evictcontrol -C o -c /tmp/%s %s", sealCtx, handle));
                        System.out.println(persistResult);

                        // Step 5: Test unsealing to verify
                        System.out.println(
                                        "\n" + ColorPrinter.yellow("Step 5: Verifying certificate can be unsealed..."));
                        String testFile = "test_unseal_" + System.currentTimeMillis() + ".pem";
                        String unsealResult = executeTpm2Command(
                                        String.format("tpm2_unseal -c %s -o /tmp/%s", handle, testFile));
                        System.out.println(unsealResult);

                        // Verify it's a valid certificate
                        String verifyResult = executeTpm2Command(
                                        String.format("openssl x509 -in /tmp/%s -text -noout | head -5", testFile));
                        System.out.println("\nCertificate verification:");
                        System.out.println(verifyResult);

                        System.out.println(ColorPrinter.green("\n✓ Certificate stored as TPM object successfully!"));
                        System.out.println(ColorPrinter.yellow("  Handle: " + handle));
                        System.out.println(ColorPrinter.yellow("  Type: Sealed Data Object"));
                        System.out.println(
                                        ColorPrinter.yellow("  Access: Use tpm2_unseal -c " + handle + " to retrieve"));
                        System.out.println(ColorPrinter.yellow(
                                        "\n  Note: This creates a sealed data object, not directly usable by OpenSSL TPM2 provider."));
                        System.out.println(
                                        ColorPrinter.yellow(
                                                        "  For OpenSSL access, use the NV storage + filesystem export approach."));

                        // Cleanup temporary files
                        if (isDirectContent) {
                                executeTpm2Command("rm -f /tmp/" + certFile);
                        }
                        executeTpm2Command("rm -f /tmp/" + primaryCtx + " /tmp/" + sealPub + " /tmp/" + sealPriv +
                                        " /tmp/" + sealCtx + " /tmp/" + testFile);

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error: " + e.getMessage()));
                        e.printStackTrace();
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.cert.unseal", value = "Unseal certificate from TPM object - eg: tpm2.cert.unseal --handle 0x81000001 --output cert.pem")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2CertUnseal(
                        @ShellOption(value = { "--handle", "-h" }) String handle,
                        @ShellOption(value = { "--output",
                                        "-o" }, defaultValue = "unsealed_cert.pem") String outputFile) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Unseal Certificate"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Unseal the certificate
                        String unsealResult = executeTpm2Command(
                                        String.format("tpm2_unseal -c %s -o /tmp/%s", handle, outputFile));
                        System.out.println(unsealResult);

                        // Verify it's a valid certificate
                        String verifyResult = executeTpm2Command(
                                        String.format("openssl x509 -in /tmp/%s -text -noout | head -10", outputFile));
                        System.out.println("\nCertificate verification:");
                        System.out.println(verifyResult);

                        System.out.println(ColorPrinter.green("\n✓ Certificate unsealed successfully!"));
                        System.out.println(ColorPrinter.yellow("  Output file: /tmp/" + outputFile));
                        System.out.println(ColorPrinter.yellow("  Use this file with OpenSSL applications"));

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error: " + e.getMessage()));
                }

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

        @ShellMethod(key = "tpm2.create.csr", value = "Create Certificate Signing Request using TPM key - eg: tpm2.create.csr --handle 0x81010001 --subject '/C=US/ST=CA/O=MyOrg/CN=device.local' --output device.csr --password mypass")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2CreateCSR(
                        @ShellOption(value = { "--handle", "-c" }, defaultValue = "0x81010001") String handle,
                        @ShellOption(value = { "--subject",
                                        "-s" }, defaultValue = "/C=US/ST=California/L=SanJose/O=MyCompany/OU=BMC/CN=bmc.example.com") String subject,
                        @ShellOption(value = { "--output", "-o" }, defaultValue = "device.csr") String output,
                        @ShellOption(value = { "--password", "-p" }, defaultValue = "") String password,
                        @ShellOption(value = { "--hash-alg",
                                        "-g" }, defaultValue = "sha256", valueProvider = HashAlgorithmProvider.class) String hashAlg) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Create Certificate Signing Request (CSR)"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Step 1: Check for TPM engine
                        System.out.println(ColorPrinter.yellow("\nStep 1: Checking for TPM engine support..."));
                        String engineCheck = executeTpm2Command("openssl engine -t tpm2tss 2>&1");
                        boolean hasTpmEngine = engineCheck.contains("[ available ]")
                                        && !engineCheck.contains("cannot open shared object");

                        if (!hasTpmEngine) {
                                System.out.println(ColorPrinter.red("✗ TPM2-TSS engine not available"));
                                System.out.println(ColorPrinter.yellow("Cannot create CSR without TPM engine"));
                                return;
                        }
                        System.out.println(ColorPrinter.green("✓ TPM2-TSS engine detected"));

                        // Step 2: Create CSR using TPM engine
                        System.out.println(ColorPrinter.yellow("\nStep 2: Creating CSR with TPM key..."));

                        String csrCmd;
                        if (!password.isEmpty()) {
                                System.out.println(ColorPrinter
                                                .yellow("Using provided password for TPM key authorization..."));
                                csrCmd = String.format(
                                                "openssl req -new -engine tpm2tss -keyform engine -key %s "
                                                                +
                                                                "-out /tmp/%s -subj '%s' -passin pass:'%s' 2>&1",
                                                handle, output, subject, password);
                        } else {
                                System.out.println(ColorPrinter.yellow("Attempting with empty password..."));
                                csrCmd = String.format(
                                                "openssl req -new -engine tpm2tss -keyform engine -key %s "
                                                                +
                                                                "-out /tmp/%s -subj '%s' -passin pass:'' 2>&1",
                                                handle, output, subject);
                        }

                        String result = executeTpm2Command(csrCmd);

                        if (result.contains("error") || result.contains("Error") || result.contains("ERROR")) {
                                System.out.println(ColorPrinter.red("\n✗ CSR creation failed!"));
                                System.out.println(ColorPrinter.yellow("\nError details:"));
                                System.out.println(result);
                                System.out.println(ColorPrinter.yellow("\nTry:"));
                                System.out.println("  - Provide password: --password 'your_password'");
                                System.out.println("  - Check TPM key: tpm2.readpublic -c " + handle);
                        } else {
                                System.out.println(ColorPrinter.green("\n✓ CSR created successfully: /tmp/" + output));
                                System.out.println(ColorPrinter.yellow("\nTo view the CSR:"));
                                System.out.println("  openssl req -in /tmp/" + output + " -text -noout");
                        }

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error creating CSR: " + e.getMessage()));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.create.selfsigned", value = "Create self-signed certificate using TPM key - eg: tpm2.create.selfsigned --handle 0x81010001 --subject '/C=US/O=MyOrg/CN=CA' --output ca.crt --days 3650 --password mypass")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2CreateSelfSigned(
                        @ShellOption(value = { "--handle", "-c" }, defaultValue = "0x81010001") String handle,
                        @ShellOption(value = { "--subject",
                                        "-s" }, defaultValue = "/C=US/ST=California/O=MyCompany/OU=BMC/CN=BMC-CA") String subject,
                        @ShellOption(value = { "--output", "-o" }, defaultValue = "ca.crt") String output,
                        @ShellOption(value = { "--days", "-d" }, defaultValue = "3650") String days,
                        @ShellOption(value = { "--is-ca" }, defaultValue = "true") boolean isCa,
                        @ShellOption(value = { "--password", "-p" }, defaultValue = "") String password) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Create Self-Signed Certificate"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Step 1: Export public key
                        System.out.println(ColorPrinter.yellow("\nStep 1: Exporting public key from TPM..."));
                        String pubKeyFile = "tpm_pubkey_" + System.currentTimeMillis() + ".pem";
                        executeTpm2Command(
                                        String.format("tpm2_readpublic -c %s -f pem -o /tmp/%s", handle, pubKeyFile));
                        System.out.println(ColorPrinter.green("✓ Public key exported"));

                        // Step 2: Create certificate config
                        System.out.println(ColorPrinter.yellow("\nStep 2: Creating certificate configuration..."));
                        String configFile = "cert_config_" + System.currentTimeMillis() + ".cnf";
                        String configContent = String.format(
                                        "[req]\n" +
                                                        "distinguished_name = req_distinguished_name\n" +
                                                        "x509_extensions = v3_ext\n" +
                                                        "prompt = no\n\n" +
                                                        "[req_distinguished_name]\n" +
                                                        "# Subject from parameter\n\n" +
                                                        "[v3_ext]\n" +
                                                        "subjectKeyIdentifier = hash\n" +
                                                        "authorityKeyIdentifier = keyid:always,issuer\n" +
                                                        "%s" +
                                                        "keyUsage = critical, digitalSignature, keyEncipherment%s\n",
                                        isCa ? "basicConstraints = critical, CA:TRUE\n"
                                                        : "basicConstraints = CA:FALSE\n",
                                        isCa ? ", keyCertSign, cRLSign" : "");

                        executeTpm2Command(String.format("printf '%%s' '%s' > /tmp/%s",
                                        configContent.replace("'", "'\\''"), configFile));

                        // Step 3: Check for TPM engine and create certificate
                        System.out.println(ColorPrinter.yellow("\nStep 3: Checking for TPM engine support..."));

                        // Check if tpm2tss engine is available and working
                        String engineCheck = executeTpm2Command("openssl engine -t tpm2tss 2>&1");
                        boolean hasTpmEngine = engineCheck.contains("[ available ]")
                                        && !engineCheck.contains("cannot open shared object");

                        String certResult;
                        if (hasTpmEngine) {
                                System.out.println(ColorPrinter.green("✓ TPM2-TSS engine detected!"));
                                System.out.println(ColorPrinter.yellow(
                                                "\nStep 4: Creating self-signed certificate with TPM engine..."));

                                // Use TPM engine to create certificate
                                String certCmd;
                                if (!password.isEmpty()) {
                                        System.out.println(ColorPrinter.yellow(
                                                        "Using provided password for TPM key authorization..."));
                                        // Pass password directly using -passin pass:
                                        certCmd = String.format(
                                                        "openssl req -new -x509 -engine tpm2tss -keyform engine -key %s "
                                                                        +
                                                                        "-out /tmp/%s -days %s -subj '%s' -config /tmp/%s -passin pass:'%s' 2>&1",
                                                        handle, output, days, subject, configFile, password);
                                } else {
                                        System.out.println(ColorPrinter.yellow(
                                                        "⚠ No password provided. Attempting with empty password..."));
                                        System.out.println(ColorPrinter.yellow(
                                                        "If this fails, the TPM key requires authorization."));
                                        System.out.println(ColorPrinter.yellow(
                                                        "Please use: --password 'your_password'"));
                                        // Try with empty password
                                        certCmd = String.format(
                                                        "openssl req -new -x509 -engine tpm2tss -keyform engine -key %s "
                                                                        +
                                                                        "-out /tmp/%s -days %s -subj '%s' -config /tmp/%s -passin pass:'' 2>&1",
                                                        handle, output, days, subject, configFile);
                                }
                                certResult = executeTpm2Command(certCmd);

                                if (certResult.contains("error") || certResult.contains("Error")
                                                || certResult.contains("ERROR")) {
                                        System.out.println(ColorPrinter.red("\n⚠ Certificate creation failed!"));
                                        System.out.println(ColorPrinter.yellow("\nPossible reasons:"));
                                        System.out.println(
                                                        "1. TPM key requires authorization (password/PIN) - use --password option");
                                        System.out.println("2. TPM resource manager not running");
                                        System.out.println("3. TPM is busy or locked");
                                        System.out.println("4. Key attributes don't allow signing");
                                        System.out.println("\nTry:");
                                        System.out.println("  - Check if key has auth: tpm2_readpublic -c " + handle);
                                        System.out.println("  - Check TPM status: tpm2_getcap properties-fixed");
                                        System.out.println(
                                                        "  - Create a new key without auth: tpm2.create --parent primary.ctx");
                                }
                        } else {
                                System.out.println(ColorPrinter
                                                .yellow("⚠ TPM2-TSS engine not found. Trying standard OpenSSL..."));
                                System.out.println(ColorPrinter
                                                .yellow("Note: This will likely fail without the private key.\n"));

                                String certCmd = String.format(
                                                "openssl req -new -x509 -key /tmp/%s -out /tmp/%s -days %s -subj '%s' -config /tmp/%s 2>&1",
                                                pubKeyFile, output, days, subject, configFile);
                                certResult = executeTpm2Command(certCmd);
                        }

                        // Check if certificate was actually created
                        String checkCmd = String.format("test -f /tmp/%s && echo 'EXISTS' || echo 'NOT_FOUND'", output);
                        String checkResult = executeTpm2Command(checkCmd);

                        if (checkResult.trim().equals("EXISTS")) {
                                System.out.println(ColorPrinter
                                                .green("\n✓ Self-signed certificate created: /tmp/" + output));
                                System.out.println(ColorPrinter.yellow("\nTo view the certificate:"));
                                System.out.println("  openssl x509 -in /tmp/" + output + " -text -noout");
                                if (hasTpmEngine) {
                                        System.out.println(ColorPrinter.green(
                                                        "\n✓ Certificate was signed using TPM private key via tpm2tss engine"));
                                }
                        } else {
                                System.out.println(ColorPrinter.red("\n✗ Certificate creation failed!"));
                                System.out.println(ColorPrinter.yellow("\nError details:"));
                                System.out.println(certResult);

                                if (!hasTpmEngine) {
                                        System.out.println(ColorPrinter.yellow(
                                                        "\nReason: OpenSSL cannot create self-signed certificates with only a public key."));
                                        System.out.println(ColorPrinter.yellow(
                                                        "The private key is protected inside the TPM and cannot be exported."));
                                        System.out.println(ColorPrinter.yellow("\nSolutions:"));
                                        System.out.println("1. Install tpm2-tss-engine:");
                                        System.out.println("   apt-get install libtpm2-tss-engine0");
                                        System.out.println("   or: apt-get install libtpm2-tss-engine-openssl3");
                                        System.out.println("\n2. Use tpm2-pkcs11 provider:");
                                        System.out.println("   apt-get install tpm2-pkcs11");
                                        System.out.println("\n3. Use tpm2_certify for TPM attestation certificates");
                                        System.out.println("\nPublic key saved to: /tmp/" + pubKeyFile);
                                } else {
                                        System.out.println(ColorPrinter.yellow(
                                                        "\nTPM engine library found but OpenSSL cannot load it."));
                                        System.out.println(ColorPrinter.yellow("\nDiagnosing the issue..."));

                                        // Check if symlink is needed
                                        String checkLib = executeTpm2Command(
                                                        "ls -la /usr/lib/engines-3/ | grep tpm2tss 2>&1");
                                        System.out.println("\nEngine directory contents:");
                                        System.out.println(checkLib);

                                        if (checkLib.contains("libtpm2tss.so") && !checkLib.contains("tpm2tss.so ->")) {
                                                System.out.println(ColorPrinter.yellow(
                                                                "\n⚠ Found libtpm2tss.so but missing tpm2tss.so symlink!"));
                                                System.out.println(ColorPrinter.yellow("\nTo fix, create a symlink:"));
                                                System.out.println("  cd /usr/lib/engines-3/");
                                                System.out.println("  ln -s libtpm2tss.so tpm2tss.so");
                                                System.out.println("\nOr run this command:");
                                                System.out.println(
                                                                "  sudo ln -s /usr/lib/engines-3/libtpm2tss.so /usr/lib/engines-3/tpm2tss.so");
                                        } else {
                                                System.out.println(ColorPrinter.yellow(
                                                                "\nOther possible issues:"));
                                                System.out.println(
                                                                "1. Check TPM handle is valid: tpm2_getcap handles-persistent");
                                                System.out.println(
                                                                "2. Check TPM resource manager is running: systemctl status tpm2-abrmd");
                                                System.out.println(
                                                                "3. Check library dependencies: ldd /usr/lib/engines-3/libtpm2tss.so");
                                        }
                                }
                        }

                        // Cleanup config file only
                        executeTpm2Command("rm -f /tmp/" + configFile);

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error creating certificate: " + e.getMessage()));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }

        @ShellMethod(key = "tpm2.sign.cert", value = "Sign certificate/CSR using TPM CA key - eg: tpm2.sign.cert --ca-handle 0x81010001 --ca-cert ca.crt --csr device.csr --output device.crt --days 365 --password mypass")
        @ShellMethodAvailability("availabilityCheck")
        void tpm2SignCert(
                        @ShellOption(value = { "--ca-handle", "-c" }, defaultValue = "0x81010001") String caHandle,
                        @ShellOption(value = { "--ca-cert" }, defaultValue = "ca.crt") String caCert,
                        @ShellOption(value = { "--csr" }, defaultValue = "device.csr") String csrFile,
                        @ShellOption(value = { "--output", "-o" }, defaultValue = "device.crt") String output,
                        @ShellOption(value = { "--days", "-d" }, defaultValue = "365") String days,
                        @ShellOption(value = { "--password", "-p" }, defaultValue = "") String password) {
                System.out.println("\n" + ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
                System.out.println(ColorPrinter.cyan("  TPM2 Sign Certificate with CA Key"));
                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));

                try {
                        // Step 1: Check for TPM engine
                        System.out.println(ColorPrinter.yellow("\nStep 1: Checking for TPM engine support..."));
                        String engineCheck = executeTpm2Command("openssl engine -t tpm2tss 2>&1");
                        boolean hasTpmEngine = engineCheck.contains("[ available ]")
                                        && !engineCheck.contains("cannot open shared object");

                        if (!hasTpmEngine) {
                                System.out.println(ColorPrinter.red("✗ TPM2-TSS engine not available"));
                                System.out.println(ColorPrinter.yellow("Cannot sign certificate without TPM engine"));
                                return;
                        }
                        System.out.println(ColorPrinter.green("✓ TPM2-TSS engine detected"));

                        // Step 2: Sign the CSR with OpenSSL using TPM engine
                        System.out.println(ColorPrinter.yellow("\nStep 2: Signing certificate with TPM CA key..."));

                        // Try standard method first
                        System.out.println(ColorPrinter.yellow("Attempting standard CSR signing..."));
                        String signCmd;
                        if (!password.isEmpty()) {
                                System.out.println(ColorPrinter
                                                .yellow("Using provided password for CA key authorization..."));
                                signCmd = String.format(
                                                "openssl x509 -req -in /tmp/%s -CA /tmp/%s -engine tpm2tss -CAkeyform engine -CAkey %s "
                                                                +
                                                                "-CAcreateserial -out /tmp/%s -days %s -sha256 -passin pass:'%s' 2>&1",
                                                csrFile, caCert, caHandle, output, days, password);
                        } else {
                                System.out.println(ColorPrinter.yellow("Attempting with empty password..."));
                                signCmd = String.format(
                                                "openssl x509 -req -in /tmp/%s -CA /tmp/%s -engine tpm2tss -CAkeyform engine -CAkey %s "
                                                                +
                                                                "-CAcreateserial -out /tmp/%s -days %s -sha256 -passin pass:'' 2>&1",
                                                csrFile, caCert, caHandle, output, days);
                        }

                        String result = executeTpm2Command(signCmd);

                        // Check if certificate was created
                        String checkCmd = String.format("test -f /tmp/%s && wc -c < /tmp/%s || echo '0'", output,
                                        output);
                        String sizeResult = executeTpm2Command(checkCmd);
                        int fileSize = 0;
                        try {
                                fileSize = Integer.parseInt(sizeResult.trim());
                        } catch (NumberFormatException e) {
                                // File doesn't exist
                        }

                        // If standard method failed due to verification, try workaround
                        if (fileSize < 100 && result.contains("self-signature did not match")) {
                                System.out.println(ColorPrinter.yellow(
                                                "\n⚠ CSR verification failed (expected for TPM keys with scheme=null)"));
                                System.out.println(ColorPrinter.yellow("Trying workaround method..."));

                                // Extract public key and subject from CSR
                                System.out.println(ColorPrinter.yellow("Extracting public key from CSR..."));
                                String extractPubKeyCmd = String.format(
                                                "openssl req -in /tmp/%s -pubkey -noout > /tmp/%s.pubkey 2>&1",
                                                csrFile, csrFile);
                                String extractResult = executeTpm2Command(extractPubKeyCmd);

                                if (!extractResult.contains("error") && !extractResult.contains("Error")) {
                                        System.out.println(ColorPrinter.yellow("Extracting subject from CSR..."));
                                        String extractSubjectCmd = String.format(
                                                        "openssl req -in /tmp/%s -noout -subject -nameopt RFC2253 2>&1 | grep -v Warning | sed 's/subject=//'",
                                                        csrFile);
                                        String subject = executeTpm2Command(extractSubjectCmd);

                                        if (!subject.contains("error") && !subject.trim().isEmpty()) {
                                                // Clean up subject - remove any warning lines and trim
                                                String[] lines = subject.split("\n");
                                                subject = "";
                                                for (String line : lines) {
                                                        if (!line.contains("Warning") && !line.contains("warning")
                                                                        && !line.trim().isEmpty()) {
                                                                subject = line.trim();
                                                                break;
                                                        }
                                                }

                                                if (subject.isEmpty()) {
                                                        System.out.println(ColorPrinter
                                                                        .red("Failed to extract clean subject"));
                                                        executeTpm2Command(
                                                                        String.format("rm -f /tmp/%s.pubkey", csrFile));
                                                        return;
                                                }
                                                System.out.println(ColorPrinter.green("Subject: " + subject));

                                                // Create temporary self-signed cert
                                                System.out.println(ColorPrinter
                                                                .yellow("Creating temporary certificate..."));
                                                String tempCertCmd = String.format(
                                                                "openssl req -new -x509 -key /tmp/%s.pubkey -keyform PEM -out /tmp/%s.temp -days 1 -subj '%s' 2>&1",
                                                                csrFile, output, subject);
                                                String tempResult = executeTpm2Command(tempCertCmd);

                                                if (!tempResult.contains("error") && !tempResult.contains("Error")) {
                                                        // Re-sign with TPM CA key
                                                        System.out.println(ColorPrinter
                                                                        .yellow("Signing with TPM CA key..."));
                                                        String workaroundCmd;
                                                        if (!password.isEmpty()) {
                                                                workaroundCmd = String.format(
                                                                                "openssl x509 -in /tmp/%s.temp -CA /tmp/%s -engine tpm2tss -CAkeyform engine -CAkey %s "
                                                                                                +
                                                                                                "-CAcreateserial -out /tmp/%s -days %s -sha256 -passin pass:'%s' 2>&1",
                                                                                output, caCert, caHandle, output, days,
                                                                                password);
                                                        } else {
                                                                workaroundCmd = String.format(
                                                                                "openssl x509 -in /tmp/%s.temp -CA /tmp/%s -engine tpm2tss -CAkeyform engine -CAkey %s "
                                                                                                +
                                                                                                "-CAcreateserial -out /tmp/%s -days %s -sha256 -passin pass:'' 2>&1",
                                                                                output, caCert, caHandle, output, days);
                                                        }

                                                        result = executeTpm2Command(workaroundCmd);
                                                        sizeResult = executeTpm2Command(checkCmd);
                                                        try {
                                                                fileSize = Integer.parseInt(sizeResult.trim());
                                                        } catch (NumberFormatException e) {
                                                                fileSize = 0;
                                                        }
                                                }
                                        }
                                }

                                // Clean up temporary files
                                executeTpm2Command(String.format("rm -f /tmp/%s.pubkey /tmp/%s.temp", csrFile, output));
                        }

                        if (fileSize > 100) {
                                System.out.println(ColorPrinter
                                                .green("\n✓ Certificate signed successfully: /tmp/" + output));
                                System.out.println(ColorPrinter.yellow("\nTo view the certificate:"));
                                System.out.println("  openssl x509 -in /tmp/" + output + " -text -noout");
                                System.out.println(ColorPrinter.yellow("\nTo verify the certificate:"));
                                System.out.println("  openssl verify -CAfile /tmp/" + caCert + " /tmp/" + output);
                        } else {
                                System.out.println(ColorPrinter.red("\n✗ Certificate signing failed!"));
                                System.out.println(ColorPrinter.yellow("\nCommand output:"));
                                System.out.println(result);
                                System.out.println(ColorPrinter.yellow("\nTry:"));
                                System.out.println("  - Provide password: --password 'your_password'");
                                System.out.println("  - Check CA key: tpm2.readpublic -c " + caHandle);
                                System.out.println("  - Verify CA cert exists: ls -l /tmp/" + caCert);
                        }

                } catch (Exception e) {
                        System.out.println(ColorPrinter.red("Error signing certificate: " + e.getMessage()));
                }

                System.out.println(ColorPrinter.cyan("═══════════════════════════════════════════════════════"));
        }
}
