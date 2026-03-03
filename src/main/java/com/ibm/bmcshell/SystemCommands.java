package com.ibm.bmcshell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.StandardCopyOption;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class SystemCommands extends CommonCommands {
    protected SystemCommands() throws IOException {
    }

    @ShellMethod(key = "sys.ls")
    protected void list(
            @ShellOption(value = { "--dir",
                    "-d" }, defaultValue = "./", valueProvider = FileCompleter.class) String args)
            throws IOException, InterruptedException {
        system(String.format("ls %s", args));
    }

    @ShellMethod(key = "sys.cat")
    protected void cat(
            @ShellOption(value = { "-f" }, defaultValue = "./", valueProvider = FileCompleter.class) String file)
            throws IOException, InterruptedException {
        system(String.format("cat %s", file));
    }

    @ShellMethod(key = "sys.pwd")
    protected void pwd() throws IOException, InterruptedException {
        system("pwd");
    }

    @ShellMethod(key = "sys.ping")
    protected void ping(String machine) throws IOException, InterruptedException {
        system(String.format("ping %s", machine));
    }

    @ShellMethod(key = "sys.scpjar")
    protected void scpjar(@ShellOption(value = { "--dir", "-d" }, defaultValue = "./") String dir)
            throws IOException, InterruptedException {
        scpfile(dir + "target/bmcshell-0.0.1-SNAPSHOT.jar");
    }

    @ShellMethod(key = "sys.scpfile")
    protected void scpfile(String file) throws IOException, InterruptedException {
        system(String.format("scp %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/", file));
    }

    @ShellMethod(key = "sys.scpdir")
    protected void scpDir(String file) throws IOException, InterruptedException {
        system(String.format("scp -r %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/", file));
    }

    @ShellMethod(key = "sys.deployweb")
    protected void scpweb(String file) throws IOException, InterruptedException {
        system(String.format("scp -r %s abhilash@gfwa129.aus.stglabs.ibm.com:/esw/san5/abhilash/work/bmcshelllibrary/",
                file));
    }

    @ShellMethod(key = "sys.wget")
    protected void wget(String url) throws IOException, InterruptedException {
        system(String.format("wget %s", url));
    }

    @ShellMethod(key = "sys.cmd", value = "eg: sy.cmd command")
    void cmd(String cmd) {
        system(cmd);
    }

    @ShellMethod(key = "ts.send")
    protected void send(String commands, @ShellOption(value = { "--id", "-i" }, defaultValue = "-1") String id) {
        System.out.println("Sending to " + id);
        try {
            // Get the resource as a stream
            InputStream resourceAsStream = getClass().getResourceAsStream("/tsend.sh");

            // Create a temporary file
            Path tempFile = Files.createTempFile("tsend", ".sh");

            // Copy the resource to the temporary file
            Files.copy(resourceAsStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Make the temporary file executable
            tempFile.toFile().setExecutable(true);

            // Specify the path to the shell script
            String scriptPath = tempFile.toString();

            // Specify the arguments
            String arg1 = id;
            String arg2 = commands;

            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath, arg1, arg2);

            // Start the process
            Process process = processBuilder.start();

            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Print the exit code
            System.out.println("Script executed with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ==================== CERTIFICATE INSPECTION COMMANDS ====================

    /**
     * Show certificate details in text format
     * Displays all certificate information including subject, issuer, validity
     * dates, etc.
     *
     * Example: sys.cert-details --cert /path/to/cert.pem
     * Example: sys.cert-details --cert /path/to/cert.der --format DER
     *
     * @param certFile Path to the certificate file
     * @param format   Certificate format (PEM or DER, default: PEM)
     */
    @ShellMethod(key = "sys.cert-details", value = "Show certificate details")
    protected void certDetails(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--format", "-f" }, defaultValue = "PEM") String format)
            throws IOException, InterruptedException {
        system(String.format(
                "openssl x509 -in '%s' -inform %s -text -noout 2>/dev/null || openssl x509 -in '%s' -text -noout",
                certFile, format, certFile));
    }

    /**
     * Show certificate subject information
     * 
     * Example: sys.cert-subject --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-subject", value = "Show certificate subject")
    protected void certSubject(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -subject -noout", certFile));
    }

    /**
     * Show certificate issuer information
     * 
     * Example: sys.cert-issuer --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-issuer", value = "Show certificate issuer")
    protected void certIssuer(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -issuer -noout", certFile));
    }

    /**
     * Show certificate validity dates (start and end dates)
     *
     * Example: sys.cert-dates --cert /path/to/cert.pem
     *
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-dates", value = "Show certificate validity dates")
    protected void certDates(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -dates -noout", certFile));
    }

    /**
     * Show certificate start date (notBefore)
     * 
     * Example: sys.cert-startdate --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-startdate", value = "Show certificate start date")
    protected void certStartDate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -startdate -noout", certFile));
    }

    /**
     * Show certificate end date (notAfter)
     * 
     * Example: sys.cert-enddate --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-enddate", value = "Show certificate end date")
    protected void certEndDate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -enddate -noout", certFile));
    }

    /**
     * Show certificate fingerprint (SHA256)
     * 
     * Example: sys.cert-fingerprint --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-fingerprint", value = "Show certificate SHA256 fingerprint")
    protected void certFingerprint(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -fingerprint -sha256 -noout", certFile));
    }

    /**
     * Show certificate serial number
     * 
     * Example: sys.cert-serial --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-serial", value = "Show certificate serial number")
    protected void certSerial(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -serial -noout", certFile));
    }

    /**
     * Show certificate public key
     * 
     * Example: sys.cert-pubkey --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-pubkey", value = "Show certificate public key")
    protected void certPubkey(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -pubkey -noout", certFile));
    }

    /**
     * Show certificate modulus (for RSA certificates)
     * 
     * Example: sys.cert-modulus --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-modulus", value = "Show certificate modulus")
    protected void certModulus(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -modulus -noout", certFile));
    }

    /**
     * Show certificate email addresses (if present in subject alternative names)
     * 
     * Example: sys.cert-email --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-email", value = "Show certificate email addresses")
    protected void certEmail(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -email -noout", certFile));
    }

    /**
     * Check if certificate has expired
     * 
     * Example: sys.cert-checkend --cert /path/to/cert.pem --seconds 86400
     * 
     * @param certFile Path to the certificate file
     * @param seconds  Number of seconds from now to check (default: 0 for immediate
     *                 check)
     */
    @ShellMethod(key = "sys.cert-checkend", value = "Check if certificate will expire in specified seconds")
    protected void certCheckEnd(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--seconds", "-s" }, defaultValue = "0") int seconds)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -checkend %d -noout", certFile, seconds));
    }

    /**
     * Show certificate purpose (what the certificate can be used for)
     * 
     * Example: sys.cert-purpose --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-purpose", value = "Show certificate purpose")
    protected void certPurpose(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s -purpose -noout", certFile));
    }

    /**
     * Show certificate in PEM format
     * 
     * Example: sys.cert-pem --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-pem", value = "Show certificate in PEM format")
    protected void certPem(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        system(String.format("openssl x509 -in %s", certFile));
    }

    /**
     * Show comprehensive certificate summary (subject, issuer, dates, fingerprint)
     * 
     * Example: sys.cert-summary --cert /path/to/cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "sys.cert-summary", value = "Show comprehensive certificate summary")
    protected void certSummary(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile)
            throws IOException, InterruptedException {
        System.out.println("=== Certificate Summary ===\n");

        System.out.println("Subject:");
        system(String.format("openssl x509 -in %s -subject -noout", certFile));

        System.out.println("\nIssuer:");
        system(String.format("openssl x509 -in %s -issuer -noout", certFile));

        System.out.println("\nValidity:");
        system(String.format("openssl x509 -in %s -dates -noout", certFile));

        System.out.println("\nSerial Number:");
        system(String.format("openssl x509 -in %s -serial -noout", certFile));

        System.out.println("\nSHA256 Fingerprint:");
        system(String.format("openssl x509 -in %s -fingerprint -sha256 -noout", certFile));

        System.out.println("\nExpiration Check:");
        system(String.format("openssl x509 -in %s -checkend 0 -noout ", certFile));

    }

    // ==================== CERTIFICATE CREATION COMMANDS ====================

    /**
     * Create a self-signed certificate with private key
     * Generates a new RSA private key and self-signed certificate
     *
     * Example: sys.cert-create --out mycert --days 365 --subject
     * "/C=US/ST=CA/O=MyOrg/CN=example.com"
     * Example: sys.cert-create --out mycert --days 365 --subject "/CN=example.com"
     * --format DER
     * Example: sys.cert-create --out mycert --days 730 --subject "/CN=test.com"
     * --keysize 4096
     *
     * @param outFile Base name for output files (will create .key and
     *                .crt/.pem/.der files)
     * @param days    Number of days the certificate is valid (default: 365)
     * @param subject Certificate subject (e.g.,
     *                "/C=US/ST=CA/O=MyOrg/CN=example.com")
     * @param format  Output format: PEM (default), DER, or PKCS12
     * @param keySize RSA key size in bits (default: 2048)
     */
    @ShellMethod(key = "sys.cert-create", value = "Create a self-signed certificate")
    protected void certCreate(
            @ShellOption(value = { "--out", "-o" }) String outFile,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "365") int days,
            @ShellOption(value = { "--subject", "-s" }) String subject,
            @ShellOption(value = { "--format", "-f" }, defaultValue = "PEM") String format,
            @ShellOption(value = { "--keysize", "-k" }, defaultValue = "2048") int keySize)
            throws IOException, InterruptedException {

        String formatUpper = format.toUpperCase();

        // Ensure subject starts with '/' for OpenSSL compatibility
        String formattedSubject = subject.startsWith("/") ? subject : "/" + subject;

        // Get absolute path for output files - use absolute path for actual file
        // operations
        java.io.File outFileObj = new java.io.File(outFile);
        String absolutePath = outFileObj.getAbsolutePath();

        // Generate private key
        System.out.println("Generating " + keySize + "-bit RSA private key...");
        system(String.format("openssl genrsa -out %s.key %d", absolutePath, keySize));

        // Generate certificate based on format
        switch (formatUpper) {
            case "PEM":
                System.out.println("Creating self-signed certificate in PEM format...");
                system(String.format(
                        "openssl req -new -x509 -key %s.key -out %s.pem -days %d -subj %s",
                        absolutePath, absolutePath, days, formattedSubject));
                System.out.println("\nCertificate created successfully:");
                System.out.println("  Private Key: " + absolutePath + ".key");
                System.out.println("  Certificate: " + absolutePath + ".pem");
                break;

            case "DER":
                // First create PEM, then convert to DER
                System.out.println("Creating self-signed certificate in DER format...");
                system(String.format(
                        "openssl req -new -x509 -key %s.key -out %s_temp.pem -days %d -subj %s",
                        absolutePath, absolutePath, days, formattedSubject));
                system(String.format(
                        "openssl x509 -in %s_temp.pem -outform DER -out %s.der",
                        absolutePath, absolutePath));
                system(String.format("rm %s_temp.pem", absolutePath));
                System.out.println("\nCertificate created successfully:");
                System.out.println("  Private Key: " + absolutePath + ".key");
                System.out.println("  Certificate: " + absolutePath + ".der");
                break;

            case "PKCS12":
            case "P12":
                // Create PEM first, then convert to PKCS12
                System.out.println("Creating self-signed certificate in PKCS12 format...");
                system(String.format(
                        "openssl req -new -x509 -key %s.key -out %s_temp.pem -days %d -subj %s",
                        absolutePath, absolutePath, days, formattedSubject));
                system(String.format(
                        "openssl pkcs12 -export -out %s.p12 -inkey %s.key -in %s_temp.pem -passout pass:",
                        absolutePath, absolutePath, absolutePath));
                system(String.format("rm %s_temp.pem", absolutePath));
                System.out.println("\nCertificate created successfully:");
                System.out.println("  PKCS12 Bundle: " + absolutePath + ".p12 (contains both key and certificate)");
                break;

            default:
                System.err.println("Unsupported format: " + format);
                System.err.println("Supported formats: PEM (default), DER, PKCS12");
                return;
        }

        System.out.println("  Valid for: " + days + " days");
        System.out.println("  Subject: " + formattedSubject);
    }
}
