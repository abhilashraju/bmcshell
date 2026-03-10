package com.ibm.bmcshell;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.math.BigInteger;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class OpenSslCommandsEnhanced extends CommonCommands {

    protected OpenSslCommandsEnhanced() throws IOException {
    }

    // ==================== KEY PAIR GENERATION COMMANDS (JAVA-BASED)
    // ====================

    /**
     * Generate RSA key pair using Java cryptography
     * Creates RSA private and public keys without calling OpenSSL
     * 
     * Example: ssl.java-gen-keypair --private private_key.pem --public
     * public_key.pem --bits 2048
     * 
     * @param privateKeyFile Output path for private key
     * @param publicKeyFile  Output path for public key
     * @param bits           Key size in bits (default: 2048)
     */
    @ShellMethod(key = "ssl.java-gen-keypair", value = "Generate RSA key pair using Java")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaGenKeyPair(
            @ShellOption(value = { "--private", "-priv" }, valueProvider = FileCompleter.class) String privateKeyFile,
            @ShellOption(value = { "--public", "-pub" }, valueProvider = FileCompleter.class) String publicKeyFile,
            @ShellOption(value = { "--bits", "-b" }, defaultValue = "2048") int bits) {
        try {
            // Generate RSA key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(bits, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Write private key in PKCS#8 format (OpenSSL compatible)
            writePrivateKeyPem(privateKeyFile, keyPair.getPrivate());
            System.out.println("✓ Private key written to: " + privateKeyFile);
            System.out.println("  Algorithm: " + keyPair.getPrivate().getAlgorithm());
            System.out.println("  Format: PKCS#8 (OpenSSL compatible)");

            // Write public key
            writePemFile(publicKeyFile, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            System.out.println("✓ Public key written to: " + publicKeyFile);
            System.out.println("  Algorithm: " + keyPair.getPublic().getAlgorithm());
            System.out.println("  Format: " + keyPair.getPublic().getFormat());

        } catch (Exception e) {
            System.err.println("✗ Error generating key pair: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate EC key pair using Java cryptography
     * Creates elliptic curve private and public keys
     * 
     * Example: ssl.java-gen-ec-keypair --private ec_private.pem --public
     * ec_public.pem --curve secp256r1
     * 
     * @param privateKeyFile Output path for private key
     * @param publicKeyFile  Output path for public key
     * @param curve          EC curve name (default: secp256r1)
     */
    @ShellMethod(key = "ssl.java-gen-ec-keypair", value = "Generate EC key pair using Java")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaGenEcKeyPair(
            @ShellOption(value = { "--private", "-priv" }, valueProvider = FileCompleter.class) String privateKeyFile,
            @ShellOption(value = { "--public", "-pub" }, valueProvider = FileCompleter.class) String publicKeyFile,
            @ShellOption(value = { "--curve" }, defaultValue = "secp256r1") String curve) {
        try {
            // Generate EC key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(curve);
            keyGen.initialize(ecSpec, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Write private key in PKCS#8 format
            writePrivateKeyPem(privateKeyFile, keyPair.getPrivate());
            System.out.println("✓ EC private key written to: " + privateKeyFile);
            System.out.println("  Curve: " + curve);
            System.out.println("  Algorithm: " + keyPair.getPrivate().getAlgorithm());

            // Write public key
            writePemFile(publicKeyFile, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            System.out.println("✓ EC public key written to: " + publicKeyFile);
            System.out.println("  Algorithm: " + keyPair.getPublic().getAlgorithm());

        } catch (Exception e) {
            System.err.println("✗ Error generating EC key pair: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate self-signed CA certificate using OpenSSL with existing key
     * Uses a pre-generated key to create a CA certificate
     * 
     * Example: ssl.java-gen-ca-with-key --key ca_key.pem --cert ca_cert.pem --cn
     * "My CA" --days 3650
     * 
     * @param keyFile    Path to existing private key (generate with
     *                   ssl.java-gen-keypair first)
     * @param certFile   Output path for certificate
     * @param commonName Common Name for the CA (default: "Test CA")
     * @param days       Validity period in days (default: 3650)
     */
    @ShellMethod(key = "ssl.java-gen-ca-with-key", value = "Generate self-signed CA certificate with existing key")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaGenCAWithKey(
            @ShellOption(value = { "--key", "-k" }, valueProvider = FileCompleter.class) String keyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--cn" }, defaultValue = "Test CA") String commonName,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "3650") int days) {

        String command = String.format(
                "openssl req -x509 -key %s -out %s -days %d -subj '/CN=%s' " +
                        "-addext 'basicConstraints=critical,CA:TRUE' " +
                        "-addext 'keyUsage=critical,keyCertSign,cRLSign'",
                keyFile, certFile, days, commonName);

        System.out.println("Generating CA certificate with existing key...");
        system(command);
        System.out.println("✓ CA certificate generated: " + certFile);
    }

    /**
     * Generate entity certificate signed by CA using existing keys
     * Creates an entity certificate using pre-generated keys
     * 
     * Example: ssl.java-gen-entity-with-key --ca-key ca_key.pem --ca-cert
     * ca_cert.pem --key entity_key.pem --cert entity_cert.pem --cn
     * "server.example.com" --days 365
     * 
     * @param caKeyFile  Path to CA private key
     * @param caCertFile Path to CA certificate
     * @param keyFile    Path to entity private key (generate with
     *                   ssl.java-gen-keypair first)
     * @param certFile   Output path for entity certificate
     * @param commonName Common Name for the entity (default: "Test Entity")
     * @param days       Validity period in days (default: 365)
     */
    @ShellMethod(key = "ssl.java-gen-entity-with-key", value = "Generate entity certificate with existing keys")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaGenEntityWithKey(
            @ShellOption(value = { "--ca-key" }, valueProvider = FileCompleter.class) String caKeyFile,
            @ShellOption(value = { "--ca-cert" }, valueProvider = FileCompleter.class) String caCertFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = FileCompleter.class) String keyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--cn" }, defaultValue = "Test Entity") String commonName,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "365") int days) {

        try {
            // Create CSR first
            String csrFile = certFile.replace(".pem", ".csr");
            String csrCommand = String.format(
                    "openssl req -new -key %s -out %s -subj '/CN=%s'",
                    keyFile, csrFile, commonName);

            System.out.println("Creating CSR...");
            system(csrCommand);

            // Sign with CA
            String signCommand = String.format(
                    "openssl x509 -req -in %s -CA %s -CAkey %s -CAcreateserial -out %s -days %d " +
                            "-extfile <(printf \"basicConstraints=CA:FALSE\\nkeyUsage=digitalSignature,keyEncipherment\")",
                    csrFile, caCertFile, caKeyFile, certFile, days);

            System.out.println("Signing certificate with CA...");
            system("bash -c '" + signCommand + "'");

            System.out.println("✓ Entity certificate generated: " + certFile);
            System.out.println("✓ CSR saved to: " + csrFile);

        } catch (Exception e) {
            System.err.println("✗ Error generating entity certificate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Complete workflow: Generate keys and CA certificate
     * Creates RSA key pair and self-signed CA certificate in one step
     * 
     * Example: ssl.java-workflow-ca --key ca_key.pem --cert ca_cert.pem --cn "My
     * CA" --bits 2048 --days 3650
     * 
     * @param keyFile    Output path for CA private key
     * @param certFile   Output path for CA certificate
     * @param commonName Common Name for the CA (default: "Test CA")
     * @param bits       Key size in bits (default: 2048)
     * @param days       Validity period in days (default: 3650)
     */
    @ShellMethod(key = "ssl.java-workflow-ca", value = "Complete workflow: Generate CA key and certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaWorkflowCA(
            @ShellOption(value = { "--key", "-k" }, valueProvider = FileCompleter.class) String keyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--cn" }, defaultValue = "Test CA") String commonName,
            @ShellOption(value = { "--bits", "-b" }, defaultValue = "2048") int bits,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "3650") int days) {

        try {
            System.out.println("=== CA Generation Workflow ===");
            System.out.println("Step 1: Generating RSA key pair...");

            // Generate key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(bits, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Write private key in PKCS#8 format
            writePrivateKeyPem(keyFile, keyPair.getPrivate());
            System.out.println("✓ CA private key: " + keyFile);

            System.out.println("\nStep 2: Generating self-signed CA certificate...");

            // Generate CA certificate using OpenSSL
            String command = String.format(
                    "openssl req -x509 -key %s -out %s -days %d -subj '/CN=%s' " +
                            "-addext 'basicConstraints=critical,CA:TRUE' " +
                            "-addext 'keyUsage=critical,keyCertSign,cRLSign'",
                    keyFile, certFile, days, commonName);

            system(command);
            System.out.println("✓ CA certificate: " + certFile);

            System.out.println("\n=== CA Generation Complete ===");
            System.out.println("You can now use this CA to sign entity certificates.");

        } catch (Exception e) {
            System.err.println("✗ Error in CA workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Complete workflow: Generate entity key and certificate signed by CA
     * Creates RSA key pair and entity certificate in one step
     * 
     * Example: ssl.java-workflow-entity --ca-key ca_key.pem --ca-cert ca_cert.pem
     * --key entity_key.pem --cert entity_cert.pem --cn "server.example.com" --bits
     * 2048 --days 365
     * 
     * @param caKeyFile  Path to CA private key
     * @param caCertFile Path to CA certificate
     * @param keyFile    Output path for entity private key
     * @param certFile   Output path for entity certificate
     * @param commonName Common Name for the entity (default: "Test Entity")
     * @param bits       Key size in bits (default: 2048)
     * @param days       Validity period in days (default: 365)
     */
    @ShellMethod(key = "ssl.java-workflow-entity", value = "Complete workflow: Generate entity key and certificate")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaWorkflowEntity(
            @ShellOption(value = { "--ca-key" }, valueProvider = FileCompleter.class) String caKeyFile,
            @ShellOption(value = { "--ca-cert" }, valueProvider = FileCompleter.class) String caCertFile,
            @ShellOption(value = { "--key", "-k" }, valueProvider = FileCompleter.class) String keyFile,
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--cn" }, defaultValue = "Test Entity") String commonName,
            @ShellOption(value = { "--bits", "-b" }, defaultValue = "2048") int bits,
            @ShellOption(value = { "--days", "-d" }, defaultValue = "365") int days) {

        try {
            System.out.println("=== Entity Certificate Generation Workflow ===");
            System.out.println("Step 1: Generating RSA key pair...");

            // Generate key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(bits, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Write private key in PKCS#8 format
            writePrivateKeyPem(keyFile, keyPair.getPrivate());
            System.out.println("✓ Entity private key: " + keyFile);

            System.out.println("\nStep 2: Creating CSR...");
            String csrFile = certFile.replace(".pem", ".csr");
            String csrCommand = String.format(
                    "openssl req -new -key %s -out %s -subj '/CN=%s'",
                    keyFile, csrFile, commonName);
            system(csrCommand);
            System.out.println("✓ CSR: " + csrFile);

            System.out.println("\nStep 3: Signing certificate with CA...");
            String signCommand = String.format(
                    "openssl x509 -req -in %s -CA %s -CAkey %s -CAcreateserial -out %s -days %d",
                    csrFile, caCertFile, caKeyFile, certFile, days);
            system(signCommand);
            System.out.println("✓ Entity certificate: " + certFile);

            System.out.println("\n=== Entity Certificate Generation Complete ===");

        } catch (Exception e) {
            System.err.println("✗ Error in entity workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * List supported EC curves
     * Shows available elliptic curve names for key generation
     * 
     * Example: ssl.java-list-curves
     */
    @ShellMethod(key = "ssl.java-list-curves", value = "List supported EC curves")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaListCurves() {
        System.out.println("Commonly supported EC curves:");
        String[] curves = {
                "secp256r1 (prime256v1, P-256) - Most common",
                "secp384r1 (P-384)",
                "secp521r1 (P-521)",
                "secp256k1 - Used in Bitcoin",
                "sect283r1",
                "sect409r1",
                "sect571r1"
        };
        for (String curve : curves) {
            System.out.println("  • " + curve);
        }
        System.out.println("\nNote: Availability depends on your Java security provider.");
    }

    /**
     * Show key information
     * Displays details about a private key file
     * 
     * Example: ssl.java-key-info --key private_key.pem
     * 
     * @param keyFile Path to the private key file
     */
    @ShellMethod(key = "ssl.java-key-info", value = "Show key information")
    @ShellMethodAvailability("availabilityCheck")
    protected void javaKeyInfo(
            @ShellOption(value = { "--key", "-k" }, valueProvider = FileCompleter.class) String keyFile) {
        String command = String.format("openssl rsa -in %s -text -noout", keyFile);
        system(command);
    }

    // Helper method to write PEM files
    private void writePemFile(String filename, String type, byte[] content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(("-----BEGIN " + type + "-----\n").getBytes());
            String base64 = Base64.getEncoder().encodeToString(content);
            // Write in 64-character lines
            for (int i = 0; i < base64.length(); i += 64) {
                int end = Math.min(i + 64, base64.length());
                fos.write(base64.substring(i, end).getBytes());
                fos.write('\n');
            }
            fos.write(("-----END " + type + "-----\n").getBytes());
        }
    }

    // Helper method to write private key in PKCS#8 format (OpenSSL compatible)
    private void writePrivateKeyPem(String filename, PrivateKey privateKey) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            // Use PKCS#8 format which OpenSSL can read
            byte[] encoded = privateKey.getEncoded();
            fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
            String base64 = Base64.getEncoder().encodeToString(encoded);
            // Write in 64-character lines
            for (int i = 0; i < base64.length(); i += 64) {
                int end = Math.min(i + 64, base64.length());
                fos.write(base64.substring(i, end).getBytes());
                fos.write('\n');
            }
            fos.write("-----END PRIVATE KEY-----\n".getBytes());
        }
    }
}

// Made with Bob