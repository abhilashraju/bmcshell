package com.ibm.bmcshell;

/**
 * Utility class for certificate operations
 */
public class CertUtil {

    /**
     * Build an OpenSSL command with proper quoting and format fallback
     * Uses sh -c to handle shell operators and redirections
     * Tries PEM first, then falls back to DER if PEM fails
     *
     * @param certFile  Path to certificate file
     * @param operation OpenSSL operation (e.g., "-subject -noout", "-dates -noout")
     * @return Complete shell command with fallback
     */
    public static String buildCertCommand(String certFile, String operation) {
        String cmd = String.format(
                "openssl x509 -in '%s' %s 2>/dev/null || openssl x509 -in '%s' -inform DER %s",
                certFile, operation, certFile, operation);
        return String.format("sh -c \"%s\"", cmd.replace("\"", "\\\""));
    }

    /**
     * Build an OpenSSL command for operations that don't support -inform
     *
     * @param certFile  Path to certificate file
     * @param operation OpenSSL operation
     * @return Complete OpenSSL command
     */
    public static String buildSimpleCertCommand(String certFile, String operation) {
        return String.format("openssl x509 -in '%s' %s", certFile, operation);
    }
}
