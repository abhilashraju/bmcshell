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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify", value = "Verify certificate against CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void verify(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-verbose", value = "Verify certificate with verbose output")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyVerbose(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
        String command = String.format("openssl verify -verbose -CAfile %s %s", caFile, certFile);
        scmd(command);
    }

    /**
     * Verify certificate chain with intermediate CA
     * Validates complete certificate chain
     * 
     * Example: ssl.verify-chain --cert leaf_cert.pem --ca root_ca.pem --intermediate intermediate_ca.pem
     * 
     * @param certFile Path to the leaf certificate file
     * @param caFile Path to the root CA certificate file
     * @param intermediateFile Path to the intermediate CA certificate file
     */
    @ShellMethod(key = "ssl.verify-chain", value = "Verify certificate chain with intermediate CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyChain(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile,
            @ShellOption(value = { "--intermediate", "-i" }) String intermediateFile) {
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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.show-chain", value = "Show complete certificate chain")
    @ShellMethodAvailability("availabilityCheck")
    protected void showChain(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
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
     * @param seconds Number of seconds to check ahead
     */
    @ShellMethod(key = "ssl.check-expiry-in", value = "Check if certificate expires within specified seconds")
    @ShellMethodAvailability("availabilityCheck")
    protected void checkExpiryIn(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-server", value = "Verify certificate for SSL server purpose")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyServer(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-client", value = "Verify certificate for SSL client purpose")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyClient(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
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
     * @param certFile Path to the certificate file
     * @param caFile Path to the CA certificate file
     * @param timestamp Unix timestamp (seconds since epoch)
     */
    @ShellMethod(key = "ssl.verify-at-time", value = "Verify certificate at specific time")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyAtTime(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile,
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
     * @param caFile Path to the CA certificate file
     */
    @ShellMethod(key = "ssl.verify-at-epoch", value = "Verify certificate at Unix epoch")
    @ShellMethodAvailability("availabilityCheck")
    protected void verifyAtEpoch(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--ca" }) String caFile) {
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
     * @param keyFile Output path for private key
     * @param certFile Output path for certificate
     * @param days Validity period in days (default: 36500)
     */
    @ShellMethod(key = "ssl.gen-ca", value = "Generate self-signed CA certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void genCA(
            @ShellOption(value = { "--key", "-k" }) String keyFile,
            @ShellOption(value = { "--cert", "-c" }) String certFile,
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
            @ShellOption(value = { "--key", "-k" }) String keyFile,
            @ShellOption(value = { "--csr" }) String csrFile) {
        String command = String.format(
                "openssl req -newkey rsa:2048 -keyout %s -out %s -nodes -subj '/CN=Test Leaf'",
                keyFile, csrFile);
        scmd(command);
    }

    /**
     * Sign certificate with CA
     * Signs a CSR with CA certificate and key
     * 
     * Example: ssl.sign-cert --csr leaf_req.pem --ca-cert ca_cert.pem --ca-key ca_key.pem --cert leaf_cert.pem --days 36500
     * 
     * @param csrFile Path to the CSR file
     * @param caCertFile Path to the CA certificate file
     * @param caKeyFile Path to the CA private key file
     * @param certFile Output path for signed certificate
     * @param days Validity period in days (default: 36500)
     */
    @ShellMethod(key = "ssl.sign-cert", value = "Sign certificate with CA")
    @ShellMethodAvailability("availabilityCheck")
    protected void signCert(
            @ShellOption(value = { "--csr" }) String csrFile,
            @ShellOption(value = { "--ca-cert" }) String caCertFile,
            @ShellOption(value = { "--ca-key" }) String caKeyFile,
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "36500") int days) {
        String command = String.format(
                "openssl x509 -req -in %s -CA %s -CAkey %s -CAcreateserial -out %s -days %d",
                csrFile, caCertFile, caKeyFile, certFile, days);
        scmd(command);
    }

    // ==================== PUBLIC KEY OPERATIONS ====================

    /**
     * Extract public key from certificate
     * Outputs the public key in PEM format
     * 
     * Example: ssl.extract-pubkey --cert cert.pem --pubkey pubkey.pem
     * 
     * @param certFile Path to the certificate file
     * @param pubkeyFile Output path for public key
     */
    @ShellMethod(key = "ssl.extract-pubkey", value = "Extract public key from certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void extractPubkey(
            @ShellOption(value = { "--cert", "-c" }) String certFile,
            @ShellOption(value = { "--pubkey", "-p" }) String pubkeyFile) {
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
            @ShellOption(value = { "--cert", "-c" }) String certFile) {
        String command = String.format("openssl x509 -in %s -pubkey -noout", certFile);
        scmd(command);
    }

    // ==================== FORMAT CONVERSION COMMANDS ====================

    /**
     * Convert PEM certificate to DER format
     * 
     * Example: ssl.pem-to-der --in cert.pem --out cert.der
     * 
     * @param inFile Input PEM file
     * @param outFile Output DER file
     */
    @ShellMethod(key = "ssl.pem-to-der", value = "Convert PEM certificate to DER")
    @ShellMethodAvailability("availabilityCheck")
    protected void pemToDer(
            @ShellOption(value = { "--in", "-i" }) String inFile,
            @ShellOption(value = { "--out", "-o" }) String outFile) {
        String command = String.format("openssl x509 -in %s -outform DER -out %s", inFile, outFile);
        scmd(command);
    }

    /**
     * Convert DER certificate to PEM format
     * 
     * Example: ssl.der-to-pem --in cert.der --out cert.pem
     * 
     * @param inFile Input DER file
     * @param outFile Output PEM file
     */
    @ShellMethod(key = "ssl.der-to-pem", value = "Convert DER certificate to PEM")
    @ShellMethodAvailability("availabilityCheck")
    protected void derToPem(
            @ShellOption(value = { "--in", "-i" }) String inFile,
            @ShellOption(value = { "--out", "-o" }) String outFile) {
        String command = String.format("openssl x509 -in %s -inform DER -out %s", inFile, outFile);
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
                "openssl req -x509 -newkey rsa:2048 -keyout %s_ca_key.pem -out %s_ca_cert.pem -days 36500 -nodes -subj '/CN=Test CA' && " +
                "openssl req -newkey rsa:2048 -keyout %s_leaf_key.pem -out %s_leaf_req.pem -nodes -subj '/CN=Test Leaf' && " +
                "openssl x509 -req -in %s_leaf_req.pem -CA %s_ca_cert.pem -CAkey %s_ca_key.pem -CAcreateserial -out %s_leaf_cert.pem -days 36500 && " +
                "openssl verify -CAfile %s_ca_cert.pem %s_leaf_cert.pem && " +
                "echo '=== Certificate Dates ===' && " +
                "openssl x509 -in %s_leaf_cert.pem -noout -dates",
                prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix, prefix);
        scmd(command);
    }
}

// Made with Bob