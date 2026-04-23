package com.ibm.bmcshell;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class OpenSslCommands extends CommonCommands {
    protected OpenSslCommands() throws IOException {
    }

    // ==================== CERTIFICATE VERIFICATION COMMANDS ====================

    /**
     * Verify a certificate against a CA certificate
     * Basic certificate validation
     * 
     * Example: ssl.verify --cert leaf_cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the certificate file to verify
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify", value = "Verify certificate against CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void verify(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    /**
     * Verify certificate with verbose output
     * Shows detailed verification information
     * 
     * Example: ssl.verify-verbose --cert leaf_cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the certificate file to verify
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-verbose", value = "Verify certificate with verbose output")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyVerbose(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -verbose -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    /**
     * Verify certificate chain with intermediate CA
     * Validates complete certificate chain
     * 
     * Example: ssl.verify-chain --cert leaf_cert.pem --ca root_ca.pem
     * --intermediate intermediate_ca.pem
     * 
     * @param certFile         Path to the leaf certificate file
     * @param caFile           Path to the root CA certificate file
     * @param intermediateFile Path to the intermediate CA certificate file
     */
    @ShellMethod(key = "ssl.verify-chain", value = "Verify certificate chain with intermediate CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyChain(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile,
            @ShellOption(value = { "--intermediate",
                    "-i" }, valueProvider = RemoteFileCompleter.class) String intermediateFile) {
        String command = String.format("openssl verify -CAfile %s -untrusted %s %s",
                caFile, intermediateFile, certFile);
        scmd(command);
    }

    /**
     * Show complete certificate chain
     * Displays the full chain of trust
     * 
     * Example: ssl.show-chain --cert leaf_cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the certificate file
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.show-chain", value = "Show complete certificate chain")
    @ShellMethodAvailability("availabilityCheck")
    protected void showChain(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -show_chain -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    // ==================== CERTIFICATE INFORMATION COMMANDS ====================

    /**
     * Display certificate details
     * Shows all certificate information in text format
     * 
     * Example: ssl.cert-info --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-info", value = "Display certificate details")
    @ShellMethodAvailability("availabilityCheck")
    protected void certInfo(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -text -noout", certFile);
        scmd(command);
    }

    /**
     * Show certificate validity dates
     * Displays notBefore and notAfter dates
     * 
     * Example: ssl.cert-dates --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-dates", value = "Show certificate validity dates")
    @ShellMethodAvailability("availabilityCheck")
    protected void certDates(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -dates", certFile);
        scmd(command);
    }

    /**
     * Show certificate start date (notBefore)
     * Displays when the certificate becomes valid
     * 
     * Example: ssl.cert-startdate --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-startdate", value = "Show certificate start date")
    @ShellMethodAvailability("availabilityCheck")
    protected void certStartDate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -startdate", certFile);
        scmd(command);
    }

    /**
     * Show certificate end date (notAfter)
     * Displays when the certificate expires
     * 
     * Example: ssl.cert-enddate --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-enddate", value = "Show certificate end date")
    @ShellMethodAvailability("availabilityCheck")
    protected void certEndDate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -enddate", certFile);
        scmd(command);
    }

    /**
     * Show certificate subject
     * Displays the certificate subject DN
     * 
     * Example: ssl.cert-subject --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-subject", value = "Show certificate subject")
    @ShellMethodAvailability("availabilityCheck")
    protected void certSubject(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -subject", certFile);
        scmd(command);
    }

    /**
     * Show certificate issuer
     * Displays the certificate issuer DN
     * 
     * Example: ssl.cert-issuer --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-issuer", value = "Show certificate issuer")
    @ShellMethodAvailability("availabilityCheck")
    protected void certIssuer(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -issuer", certFile);
        scmd(command);
    }

    /**
     * Show certificate serial number
     * Displays the certificate serial number
     * 
     * Example: ssl.cert-serial --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-serial", value = "Show certificate serial number")
    @ShellMethodAvailability("availabilityCheck")
    protected void certSerial(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -serial", certFile);
        scmd(command);
    }

    /**
     * Show certificate fingerprint
     * Displays SHA256 fingerprint by default
     * 
     * Example: ssl.cert-fingerprint --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.cert-fingerprint", value = "Show certificate fingerprint")
    @ShellMethodAvailability("availabilityCheck")
    protected void certFingerprint(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -fingerprint -sha256", certFile);
        scmd(command);
    }

    // ==================== CERTIFICATE EXPIRY CHECKS ====================

    /**
     * Check if certificate has expired
     * Returns 0 if valid, 1 if expired
     * 
     * Example: ssl.check-expiry --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.check-expiry", value = "Check if certificate has expired")
    @ShellMethodAvailability("availabilityCheck")
    protected void checkExpiry(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -noout -checkend 0", certFile);
        scmd(command);
    }

    /**
     * Check if certificate expires within specified seconds
     * Useful for proactive monitoring
     * 
     * Example: ssl.check-expiry-in --cert cert.pem --seconds 86400
     * 
     * @param certFile Path to the certificate file
     * @param seconds  Number of seconds to check ahead
     */
    @ShellMethod(key = "ssl.check-expiry-in", value = "Check if certificate expires within specified seconds")
    @ShellMethodAvailability("availabilityCheck")
    protected void checkExpiryIn(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--seconds", "-s" }) int seconds) {
        String command = String.format("openssl x509 -in %s -noout -checkend %d", certFile, seconds);
        scmd(command);
    }

    // ==================== CERTIFICATE VALIDATION WITH PURPOSE ====================

    /**
     * Verify certificate for SSL server purpose
     * Validates certificate is suitable for SSL server use
     * 
     * Example: ssl.verify-server --cert server_cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the server certificate file
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-server", value = "Verify certificate for SSL server purpose")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyServer(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -purpose sslserver -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    /**
     * Verify certificate for SSL client purpose
     * Validates certificate is suitable for SSL client use
     * 
     * Example: ssl.verify-client --cert client_cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the client certificate file
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-client", value = "Verify certificate for SSL client purpose")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyClient(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -purpose sslclient -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    // ==================== TIME-BASED VERIFICATION ====================

    /**
     * Verify certificate at specific time
     * Useful for testing certificates with epoch start dates
     * 
     * Example: ssl.verify-at-time --cert cert.pem --ca ca_cert.pem --timestamp 0
     * 
     * @param certFile  Path to the certificate file
     * @param caFile    Path to the CA certificate file
     * @param timestamp Unix timestamp (seconds since epoch)
     */
    @ShellMethod(key = "ssl.verify-at-time", value = "Verify certificate at specific time")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyAtTime(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile,
            @ShellOption(value = { "--timestamp", "-t" }) long timestamp) {
        String command = String.format("openssl verify -attime %d -CAfile %s %s", timestamp, caFile, certFile);
        scmd(command);
    }

    /**
     * Verify certificate at epoch (1970-01-01)
     * Tests if certificate is valid from Unix epoch
     * 
     * Example: ssl.verify-at-epoch --cert cert.pem --ca ca_cert.pem
     * 
     * @param certFile Path to the certificate file
     * @param caFile   Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-at-epoch", value = "Verify certificate at Unix epoch")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyAtEpoch(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format("openssl verify -attime 0 -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    // ==================== CERTIFICATE GENERATION COMMANDS ====================

    /**
     * Generate self-signed CA certificate
     * Creates a new CA certificate and private key
     * 
     * Example: ssl.gen-ca --key ca_key.pem --cert ca_cert.pem --days 36500
     * 
     * @param keyFile  Output path for private key
     * @param certFile Output path for certificate
     * @param days     Validity period in days (default: 36500)
     */
    @ShellMethod(key = "ssl.gen-ca", value = "Generate self-signed CA certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void genCA(
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "36500") int days) {
        String command = String.format(
                "openssl req -x509 -newkey rsa:2048 -keyout %s -out %s -days %d -nodes -subj '/CN=Test CA'",
                keyFile, certFile, days);
        scmd(command);
    }

    /**
     * Generate certificate signing request (CSR)
     * Creates a CSR and private key
     * 
     * Example: ssl.gen-csr --key leaf_key.pem --csr leaf_req.pem
     * 
     * @param keyFile Output path for private key
     * @param csrFile Output path for CSR
     */
    @ShellMethod(key = "ssl.gen-csr", value = "Generate certificate signing request")
    @ShellMethodAvailability("availabilityCheck")
    protected void genCSR(
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--csr" }, valueProvider = RemoteFileCompleter.class) String csrFile) {
        String command = String.format(
                "openssl req -newkey rsa:2048 -keyout %s -out %s -nodes -subj '/CN=Test Leaf'",
                keyFile, csrFile);
        scmd(command);
    }

    /**
     * Sign certificate with CA
     * Signs a CSR with CA certificate and key
     * 
     * Example: ssl.sign-cert --csr leaf_req.pem --ca-cert ca_cert.pem --ca-key
     * ca_key.pem --cert leaf_cert.pem --days 36500
     * 
     * @param csrFile    Path to the CSR file
     * @param caCertFile Path to the CA certificate file
     * @param caKeyFile  Path to the CA private key file
     * @param certFile   Output path for signed certificate
     * @param days       Validity period in days (default: 36500)
     */
    @ShellMethod(key = "ssl.sign-cert", value = "Sign certificate with CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void signCert(
            @ShellOption(value = { "--csr" }, valueProvider = RemoteFileCompleter.class) String csrFile,
            @ShellOption(value = { "--ca-cert" }, valueProvider = RemoteFileCompleter.class) String caCertFile,
            @ShellOption(value = { "--ca-key" }, valueProvider = RemoteFileCompleter.class) String caKeyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "36500") int days) {
        String command = String.format(
                "openssl x509 -req -in %s -CA %s -CAkey %s -CAcreateserial -out %s -days %d",
                csrFile, caCertFile, caKeyFile, certFile, days);
        scmd(command);
    }

    /**
     * Sign certificate with TPM CA key
     * Signs a CSR using a TPM-based CA key via the tpm2tss OpenSSL engine
     * This allows signing regular OpenSSL-generated CSRs with a CA key stored in
     * TPM
     *
     * Example: ssl.sign-cert-tpm --csr device.csr --ca-cert ca.crt --ca-handle
     * 0x81010001
     * --cert device.crt --days 365 --password abhi
     *
     * @param csrFile    Path to the CSR file (regular OpenSSL CSR)
     * @param caCertFile Path to the CA certificate file
     * @param caHandle   TPM persistent handle for CA key (default: 0x81010001)
     * @param certFile   Output path for signed certificate
     * @param days       Validity period in days (default: 365)
     * @param password   TPM key password (optional, use empty string for no
     *                   password)
     */
    @ShellMethod(key = "ssl.sign-cert-tpm", value = "Sign certificate with TPM CA key")
    @ShellMethodAvailability("availabilityCheck")
    protected void signCertTPM(
            @ShellOption(value = { "--csr" }, valueProvider = RemoteFileCompleter.class) String csrFile,
            @ShellOption(value = { "--ca-cert" }, valueProvider = RemoteFileCompleter.class) String caCertFile,
            @ShellOption(value = { "--ca-handle" }, defaultValue = "0x81010001") String caHandle,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "365") int days,
            @ShellOption(value = { "--password", "-p" }, defaultValue = "") String password) {

        String command;
        if (!password.isEmpty()) {
            command = String.format(
                    "openssl x509 -req -in %s -CA %s -engine tpm2tss -CAkeyform engine -CAkey %s " +
                            "-CAcreateserial -out %s -days %d -sha256 -passin pass:'%s'",
                    csrFile, caCertFile, caHandle, certFile, days, password);
        } else {
            command = String.format(
                    "openssl x509 -req -in %s -CA %s -engine tpm2tss -CAkeyform engine -CAkey %s " +
                            "-CAcreateserial -out %s -days %d -sha256",
                    csrFile, caCertFile, caHandle, certFile, days);
        }
        scmd(command);
    }

    // ==================== PUBLIC KEY OPERATIONS ====================

    /**
     * Extract public key from certificate
     * Outputs the public key in PEM format
     * 
     * Example: ssl.extract-pubkey --cert cert.pem --pubkey pubkey.pem
     * 
     * @param certFile   Path to the certificate file
     * @param pubkeyFile Output path for public key
     */
    @ShellMethod(key = "ssl.extract-pubkey", value = "Extract public key from certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void extractPubkey(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--pubkey", "-p" }, valueProvider = RemoteFileCompleter.class) String pubkeyFile) {
        String command = String.format("openssl x509 -in %s -pubkey -noout > %s", certFile, pubkeyFile);
        scmd(command);
    }

    /**
     * Show public key from certificate
     * Displays the public key without saving to file
     * 
     * Example: ssl.show-pubkey --cert cert.pem
     * 
     * @param certFile Path to the certificate file
     */
    @ShellMethod(key = "ssl.show-pubkey", value = "Show public key from certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void showPubkey(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile) {
        String command = String.format("openssl x509 -in %s -pubkey -noout", certFile);
        scmd(command);
    }

    // ==================== FORMAT CONVERSION COMMANDS ====================

    /**
     * Convert PEM certificate to DER format
     * 
     * Example: ssl.pem-to-der --in cert.pem --out cert.der
     * 
     * @param inFile  Input PEM file
     * @param outFile Output DER file
     */
    @ShellMethod(key = "ssl.pem-to-der", value = "Convert PEM certificate to DER")
    @ShellMethodAvailability("availabilityCheck")
    protected void pemToDer(
            @ShellOption(value = { "--in", "-i" }, valueProvider = RemoteFileCompleter.class) String inFile,
            @ShellOption(value = { "--out", "-o" }, valueProvider = RemoteFileCompleter.class) String outFile) {
        String command = String.format("openssl x509 -in %s -outform DER -out %s", inFile, outFile);
        scmd(command);
    }

    /**
     * Convert DER certificate to PEM format
     * 
     * Example: ssl.der-to-pem --in cert.der --out cert.pem
     * 
     * @param inFile  Input DER file
     * @param outFile Output PEM file
     */
    @ShellMethod(key = "ssl.der-to-pem", value = "Convert DER certificate to PEM")
    @ShellMethodAvailability("availabilityCheck")
    protected void derToPem(
            @ShellOption(value = { "--in", "-i" }, valueProvider = RemoteFileCompleter.class) String inFile,
            @ShellOption(value = { "--out", "-o" }, valueProvider = RemoteFileCompleter.class) String outFile) {
        String command = String.format("openssl x509 -in %s -inform DER -out %s", inFile, outFile);
        scmd(command);
    }
    // ==================== CERTIFICATE CHAIN OPERATIONS ====================

    /**
     * Create certificate chain file from multiple certificates
     * Concatenates multiple certificate files into a single chain file
     * The order matters: typically leaf cert first, then intermediates, then root
     * CA
     * 
     * Example: ssl.create-chain --certs cert1.pem cert2.pem cert3.pem --output
     * chain.pem
     * Example: ssl.create-chain --certs leaf.pem intermediate.pem root.pem --output
     * fullchain.pem
     * 
     * @param certFiles  Array of certificate file paths to concatenate (in order)
     * @param outputFile Output path for the certificate chain file
     */
    @ShellMethod(key = "ssl.create-chain", value = "Create certificate chain from multiple certificates")
    @ShellMethodAvailability("availabilityCheck")
    protected void createCertChain(
            @ShellOption(value = { "--certs",
                    "-c" }, arity = 2147483647, valueProvider = RemoteFileCompleter.class) String[] certFiles,
            @ShellOption(value = { "--output", "-o" }, valueProvider = RemoteFileCompleter.class) String outputFile) {
        if (certFiles == null || certFiles.length < 2) {
            System.err.println("Error: At least 2 certificate files are required to create a chain");
            return;
        }

        // Build the cat command to concatenate all certificates
        StringBuilder command = new StringBuilder("cat");
        for (String certFile : certFiles) {
            command.append(" ").append(certFile);
        }
        command.append(" > ").append(outputFile);

        scmd(command.toString());
    }

    // ==================== SERVER CERTIFICATE INSPECTION ====================

    /**
     * Inspect certificate from a remote server
     * Connects to server and displays certificate information
     *
     * Example: ssl.inspect-server --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.inspect-server", value = "Inspect certificate from remote server")
    @ShellMethodAvailability("availabilityCheck")
    protected void inspectServer(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -text -noout", host,
                port);
        scmd(command);
    }

    /**
     * Get certificate from remote server
     * Downloads and displays the server certificate
     *
     * Example: ssl.get-server-cert --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.get-server-cert", value = "Get certificate from remote server")
    @ShellMethodAvailability("availabilityCheck")
    protected void getServerCert(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format("openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null", host, port);
        scmd(command);
    }

    /**
     * Save server certificate to file
     * Downloads server certificate and saves it to a file
     *
     * Example: ssl.save-server-cert --host example.com --port 443 --output
     * server.pem
     *
     * @param host       Server hostname or IP address
     * @param port       Server port (default: 443)
     * @param outputFile Output path for certificate
     */
    @ShellMethod(key = "ssl.save-server-cert", value = "Save server certificate to file")
    @ShellMethodAvailability("availabilityCheck")
    protected void saveServerCert(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--output", "-o" }, valueProvider = RemoteFileCompleter.class) String outputFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM > %s",
                host, port, outputFile);
        scmd(command);
    }

    /**
     * Check server certificate expiration
     * Shows validity dates for server certificate
     *
     * Example: ssl.check-server-expiry --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.check-server-expiry", value = "Check server certificate expiration")
    @ShellMethodAvailability("availabilityCheck")
    protected void checkServerExpiry(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -noout -dates", host,
                port);
        scmd(command);
    }

    /**
     * Get server certificate subject
     * Shows the subject DN of server certificate
     *
     * Example: ssl.get-server-subject --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.get-server-subject", value = "Get server certificate subject")
    @ShellMethodAvailability("availabilityCheck")
    protected void getServerSubject(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -noout -subject",
                host, port);
        scmd(command);
    }

    /**
     * Get server certificate issuer
     * Shows the issuer DN of server certificate
     *
     * Example: ssl.get-server-issuer --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.get-server-issuer", value = "Get server certificate issuer")
    @ShellMethodAvailability("availabilityCheck")
    protected void getServerIssuer(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -noout -issuer", host,
                port);
        scmd(command);
    }

    /**
     * Get server certificate fingerprint
     * Shows SHA256 fingerprint of server certificate
     *
     * Example: ssl.get-server-fingerprint --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.get-server-fingerprint", value = "Get server certificate fingerprint")
    @ShellMethodAvailability("availabilityCheck")
    protected void getServerFingerprint(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null | openssl x509 -noout -fingerprint -sha256",
                host, port);
        scmd(command);
    }

    /**
     * Inspect server with SNI (Server Name Indication)
     * Useful for servers with virtual hosting
     *
     * Example: ssl.inspect-server-sni --host example.com --servername example.com
     * --port 443
     *
     * @param host       Server hostname or IP address
     * @param serverName SNI server name
     * @param port       Server port (default: 443)
     */
    @ShellMethod(key = "ssl.inspect-server-sni", value = "Inspect server certificate with SNI")
    @ShellMethodAvailability("availabilityCheck")
    protected void inspectServerSNI(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--servername", "-s" }) String serverName,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format(
                "openssl s_client -connect %s:%d -servername %s -showcerts </dev/null 2>/dev/null | openssl x509 -text -noout",
                host, port, serverName);
        scmd(command);
    }

    /**
     * Verify server certificate against CA
     * Connects to server and verifies its certificate
     *
     * Example: ssl.verify-server-cert --host example.com --port 443 --ca
     * ca_cert.pem
     * Example: ssl.verify-server-cert --host example.com --port 443 --ca-nv-index
     * 0x1500001
     *
     * @param host      Server hostname or IP address
     * @param port      Server port (default: 443)
     * @param caFile    Path to CA certificate file
     * @param caNvIndex TPM NV index containing the CA certificate in PEM format
     */
    @ShellMethod(key = "ssl.verify-server-cert", value = "Verify server certificate against CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyServerCert(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = {
                    "--ca" }, defaultValue = ShellOption.NULL, valueProvider = RemoteFileCompleter.class) String caFile,
            @ShellOption(value = { "--ca-nv-index" }, defaultValue = ShellOption.NULL) String caNvIndex) {
        String caSource = caFile;
        if (caSource == null || caSource.isBlank()) {
            if (caNvIndex == null || caNvIndex.isBlank()) {
                System.out.println("Error: Provide either --ca <ca_cert.pem> or --ca-nv-index <0x1500001>");
                return;
            }
            String tempCaFile = String.format("/tmp/tpm_ca_cert_%d.pem", System.currentTimeMillis());
            caSource = tempCaFile;
            String command = String.format(
                    "tpm2_nvread %s -C o -o %s ; openssl s_client -connect %s:%d -CAfile %s -showcerts </dev/null 2>/dev/null; rm -f %s",
                    caNvIndex, tempCaFile, host, port, tempCaFile, tempCaFile);
            scmd(command);
            return;
        }
        String command = String.format("openssl s_client -connect %s:%d -CAfile %s -showcerts </dev/null 2>/dev/null",
                host, port, caSource);
        scmd(command);
    }

    /**
     * Get full certificate chain from server
     * Downloads and displays the complete certificate chain
     *
     * Example: ssl.get-server-chain --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.get-server-chain", value = "Get full certificate chain from server")
    @ShellMethodAvailability("availabilityCheck")
    protected void getServerChain(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format("openssl s_client -connect %s:%d -showcerts </dev/null 2>/dev/null", host, port);
        scmd(command);
    }

    /**
     * Test TLS connection to server
     * Shows connection details and supported protocols
     *
     * Example: ssl.test-connection --host example.com --port 443
     *
     * @param host Server hostname or IP address
     * @param port Server port (default: 443)
     */
    @ShellMethod(key = "ssl.test-connection", value = "Test TLS connection to server")
    @ShellMethodAvailability("availabilityCheck")
    protected void testConnection(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port) {
        String command = String.format("openssl s_client -connect %s:%d -brief </dev/null 2>/dev/null", host, port);
        scmd(command);
    }

    /**
     * Test specific TLS version with server
     * Checks if server supports specific TLS version
     *
     * Example: ssl.test-tls-version --host example.com --port 443 --version tls1_2
     *
     * @param host    Server hostname or IP address
     * @param port    Server port (default: 443)
     * @param version TLS version (tls1, tls1_1, tls1_2, tls1_3)
     */
    @ShellMethod(key = "ssl.test-tls-version", value = "Test specific TLS version with server")
    @ShellMethodAvailability("availabilityCheck")
    protected void testTLSVersion(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--version", "-v" }) String version) {
        String command = String.format("openssl s_client -connect %s:%d -%s -showcerts </dev/null 2>/dev/null", host,
                port, version);
        scmd(command);
    }

    // ==================== MUTUAL TLS (mTLS) COMMANDS ====================

    /**
     * Test mTLS connection with client certificate
     * Establishes mutual TLS connection where both client and server authenticate
     *
     * Example: ssl.test-mtls --host example.com --port 443 --cert client.pem --key
     * client-key.pem
     *
     * @param host     Server hostname or IP address
     * @param port     Server port (default: 443)
     * @param certFile Path to client certificate file
     * @param keyFile  Path to client private key file
     */
    @ShellMethod(key = "ssl.test-mtls", value = "Test mTLS connection with client certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void testMTLS(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -showcerts </dev/null 2>/dev/null",
                host, port, certFile, keyFile);
        scmd(command);
    }

    /**
     * Test mTLS connection with full verification
     * Establishes mutual TLS with client cert and verifies server cert against CA
     *
     * Example: ssl.test-mtls-verify --host example.com --port 443 --cert client.pem
     * --key client-key.pem --ca ca.pem
     *
     * @param host     Server hostname or IP address
     * @param port     Server port (default: 443)
     * @param certFile Path to client certificate file
     * @param keyFile  Path to client private key file
     * @param caFile   Path to CA certificate file for server verification
     */
    @ShellMethod(key = "ssl.test-mtls-verify", value = "Test mTLS with full certificate verification")
    @ShellMethodAvailability("availabilityCheck")
    protected void testMTLSVerify(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -CAfile %s -showcerts </dev/null 2>/dev/null",
                host, port, certFile, keyFile, caFile);
        scmd(command);
    }

    /**
     * Test mTLS connection with certificate chain
     * Uses client certificate with intermediate CA chain
     *
     * Example: ssl.test-mtls-chain --host example.com --port 443 --cert client.pem
     * --key client-key.pem --chain chain.pem
     *
     * @param host      Server hostname or IP address
     * @param port      Server port (default: 443)
     * @param certFile  Path to client certificate file
     * @param keyFile   Path to client private key file
     * @param chainFile Path to certificate chain file
     */
    @ShellMethod(key = "ssl.test-mtls-chain", value = "Test mTLS with certificate chain")
    @ShellMethodAvailability("availabilityCheck")
    protected void testMTLSChain(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--chain" }, valueProvider = RemoteFileCompleter.class) String chainFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -cert_chain %s -showcerts </dev/null 2>/dev/null",
                host, port, certFile, keyFile, chainFile);
        scmd(command);
    }

    /**
     * Test mTLS with SNI support
     * Mutual TLS connection with Server Name Indication
     *
     * Example: ssl.test-mtls-sni --host 192.168.1.100 --servername example.com
     * --port 443 --cert client.pem --key client-key.pem
     *
     * @param host       Server hostname or IP address
     * @param serverName SNI server name
     * @param port       Server port (default: 443)
     * @param certFile   Path to client certificate file
     * @param keyFile    Path to client private key file
     */
    @ShellMethod(key = "ssl.test-mtls-sni", value = "Test mTLS with SNI support")
    @ShellMethodAvailability("availabilityCheck")
    protected void testMTLSSNI(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--servername", "-s" }) String serverName,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -servername %s -cert %s -key %s -showcerts </dev/null 2>/dev/null",
                host, port, serverName, certFile, keyFile);
        scmd(command);
    }

    /**
     * Verify mTLS connection status
     * Shows detailed connection information including client cert verification
     *
     * Example: ssl.verify-mtls --host example.com --port 443 --cert client.pem
     * --key client-key.pem --ca ca.pem
     *
     * @param host     Server hostname or IP address
     * @param port     Server port (default: 443)
     * @param certFile Path to client certificate file
     * @param keyFile  Path to client private key file
     * @param caFile   Path to CA certificate file
     */
    @ShellMethod(key = "ssl.verify-mtls", value = "Verify mTLS connection status")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyMTLS(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--ca" }, valueProvider = RemoteFileCompleter.class) String caFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -CAfile %s -state -debug </dev/null 2>&1",
                host, port, certFile, keyFile, caFile);
        scmd(command);
    }

    /**
     * Test mTLS with specific TLS version
     * Mutual TLS connection using specific protocol version
     *
     * Example: ssl.test-mtls-version --host example.com --port 443 --cert
     * client.pem --key client-key.pem --version tls1_2
     *
     * @param host     Server hostname or IP address
     * @param port     Server port (default: 443)
     * @param certFile Path to client certificate file
     * @param keyFile  Path to client private key file
     * @param version  TLS version (tls1, tls1_1, tls1_2, tls1_3)
     */
    @ShellMethod(key = "ssl.test-mtls-version", value = "Test mTLS with specific TLS version")
    @ShellMethodAvailability("availabilityCheck")
    protected void testMTLSVersion(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile,
            @ShellOption(value = { "--version", "-v" }) String version) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -%s -showcerts </dev/null 2>/dev/null",
                host, port, certFile, keyFile, version);
        scmd(command);
    }

    /**
     * Debug mTLS connection
     * Shows verbose debugging information for mTLS troubleshooting
     *
     * Example: ssl.debug-mtls --host example.com --port 443 --cert client.pem --key
     * client-key.pem
     *
     * @param host     Server hostname or IP address
     * @param port     Server port (default: 443)
     * @param certFile Path to client certificate file
     * @param keyFile  Path to client private key file
     */
    @ShellMethod(key = "ssl.debug-mtls", value = "Debug mTLS connection with verbose output")
    @ShellMethodAvailability("availabilityCheck")
    protected void debugMTLS(
            @ShellOption(value = { "--host" }) String host,
            @ShellOption(value = { "--port", "-p" }, defaultValue = "443") int port,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = RemoteFileCompleter.class) String certFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = RemoteFileCompleter.class) String keyFile) {
        String command = String.format(
                "openssl s_client -connect %s:%d -cert %s -key %s -state -msg -debug -showcerts 2>&1",
                host, port, certFile, keyFile);
        scmd(command);
    }

    // ==================== CUSTOM OPENSSL COMMAND ====================

    /**
     * Execute custom OpenSSL command
     * Allows running any OpenSSL command
     *
     * Example: ssl.custom "openssl version"
     *
     * @param opensslCommand The complete OpenSSL command to execute
     */
    @ShellMethod(key = "ssl.custom", value = "Execute custom OpenSSL command")
    @ShellMethodAvailability("availabilityCheck")
    protected void custom(String opensslCommand) {
        scmd(opensslCommand);
    }

    // ==================== TESTING WORKFLOW COMMANDS ====================

    /**
     * Complete workflow: Generate CA, leaf cert, and verify
     * Creates a complete test certificate chain
     * 
     * Example: ssl.test-workflow --prefix test
     * 
     * @param prefix Prefix for generated files (default: test)
     */
    @ShellMethod(key = "ssl.test-workflow", value = "Generate and verify complete certificate chain")
    @ShellMethodAvailability("availabilityCheck")
    protected void testWorkflow(
            @ShellOption(value = { "--prefix", "-p" }, defaultValue = "test") String prefix) {
        String command = String.format(
                "openssl req -x509 -newkey rsa:2048 -keyout %s_ca_key.pem -out %s_ca_cert.pem -days 36500 -nodes -subj '/CN=Test CA' && "
                        +
                        "openssl req -newkey rsa:2048 -keyout %s_leaf_key.pem -out %s_leaf_req.pem -nodes -subj '/CN=Test Leaf' && "
                        +
                        "openssl x509 -req -in %s_leaf_req.pem -CA %s_ca_cert.pem -CAkey %s_ca_key.pem -CAcreateserial -out %s_leaf_cert.pem -days 36500 && "
                        +
                        "openssl verify -CAfile %s_ca_cert.pem %s_leaf_cert.pem && " +
                        "echo '=== Certificate Dates ===' && " +
                        "openssl x509 -in %s_leaf_cert.pem -noout -dates",
                prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix);
        scmd(command);
    }
}
