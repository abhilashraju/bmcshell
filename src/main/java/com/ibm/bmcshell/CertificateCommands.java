package com.ibm.bmcshell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Certificate Management Commands for BMC Redfish API
 *
 * Provides comprehensive certificate management including:
 * - HTTPS certificates
 * - LDAP certificates
 * - TrustStore certificates
 * - Certificate Service operations
 * - CSR generation
 */
@ShellComponent
public class CertificateCommands extends CommonCommands {
    
    private static final String MANAGER_ID = "bmc";
    
    protected CertificateCommands() throws Exception {
        super();
    }

    // ==================== CERTIFICATE SERVICE ====================

    /**
     * Get Certificate Service information
     *
     * Example: crt.service
     */
    @ShellMethod(key = "crt.service", value = "Get Certificate Service information")
    @ShellMethodAvailability("availabilityCheck")
    public void getCertificateService() throws Exception {
        String target = "/redfish/v1/CertificateService/";
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Get all certificate locations
     *
     * Example: crt.locations
     */
    @ShellMethod(key = "crt.locations", value = "Get all certificate locations")
    @ShellMethodAvailability("availabilityCheck")
    public void getCertificateLocations() throws Exception {
        String target = "/redfish/v1/CertificateService/CertificateLocations/";
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Replace an existing certificate
     *
     * Example: crt.replace --cert /path/to/cert.pem --cert-uri /redfish/v1/Managers/bmc/NetworkProtocol/HTTPS/Certificates/1
     *
     * @param certFile Path to the certificate file
     * @param certificateUri URI of the certificate to replace
     * @param type Certificate type (default: PEM)
     */
    @ShellMethod(key = "crt.replace", value = "Replace an existing certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void replaceCertificate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--cert-uri", "-u" }) String certificateUri,
            @ShellOption(value = { "--type", "-t" }, defaultValue = "PEM") String type) throws Exception {
        
        String certString = readCertificateFile(certFile);
        String body = String.format(
            "{\"CertificateString\":\"%s\",\"CertificateType\":\"%s\",\"CertificateUri\":{\"@odata.id\":\"%s\"}}",
            escapeCertString(certString), type, certificateUri);
        
        String target = "/redfish/v1/CertificateService/Actions/CertificateService.ReplaceCertificate/";
        System.out.println(makePostRequest(target, body, "application/json"));
    }

    /**
     * Generate a Certificate Signing Request (CSR)
     *
     * Example: crt.generate-csr --common-name mybmc.example.com --country US --state CA --city SanJose --org MyOrg --org-unit IT
     *
     * @param commonName Common Name (CN)
     * @param country Country (C)
     * @param state State or Province (ST)
     * @param city City or Locality (L)
     * @param organization Organization (O)
     * @param organizationalUnit Organizational Unit (OU)
     * @param certCollection Certificate collection URI
     * @param keyPairAlgorithm Key pair algorithm (default: RSA)
     * @param keyBitLength Key bit length (default: 2048)
     * @param keyCurveId Key curve ID for EC keys
     */
    @ShellMethod(key = "crt.generate-csr", value = "Generate a Certificate Signing Request")
    @ShellMethodAvailability("availabilityCheck")
    public void generateCSR(
            @ShellOption(value = { "--common-name", "-cn" }) String commonName,
            @ShellOption(value = { "--country", "-c" }) String country,
            @ShellOption(value = { "--state", "-s" }) String state,
            @ShellOption(value = { "--city", "-l" }) String city,
            @ShellOption(value = { "--org", "-o" }) String organization,
            @ShellOption(value = { "--org-unit", "-ou" }) String organizationalUnit,
            @ShellOption(value = { "--cert-collection", "-cc" }, defaultValue = "/redfish/v1/Managers/bmc/NetworkProtocol/HTTPS/Certificates/") String certCollection,
            @ShellOption(value = { "--key-algorithm", "-ka" }, defaultValue = "RSA") String keyPairAlgorithm,
            @ShellOption(value = { "--key-length", "-kl" }, defaultValue = "2048") int keyBitLength,
            @ShellOption(value = { "--key-curve", "-kc" }, defaultValue = "") String keyCurveId) throws Exception {
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        
        body.put("CommonName", commonName);
        body.put("Country", country);
        body.put("State", state);
        body.put("City", city);
        body.put("Organization", organization);
        body.put("OrganizationalUnit", organizationalUnit);
        
        ObjectNode certCollectionNode = mapper.createObjectNode();
        certCollectionNode.put("@odata.id", certCollection);
        body.set("CertificateCollection", certCollectionNode);
        
        body.put("KeyPairAlgorithm", keyPairAlgorithm);
        body.put("KeyBitLength", keyBitLength);
        
        if (!keyCurveId.isEmpty()) {
            body.put("KeyCurveId", keyCurveId);
        }
        
        String target = "/redfish/v1/CertificateService/Actions/CertificateService.GenerateCSR/";
        System.out.println(makePostRequest(target, body.toString(), "application/json"));
    }

    // ==================== HTTPS CERTIFICATES ====================

    /**
     * List all HTTPS certificates
     *
     * Example: crt.https.list
     */
    @ShellMethod(key = "crt.https.list", value = "List all HTTPS certificates")
    @ShellMethodAvailability("availabilityCheck")
    public void listHttpsCertificates() throws Exception {
        String target = String.format("/redfish/v1/Managers/%s/NetworkProtocol/HTTPS/Certificates/", MANAGER_ID);
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Get a specific HTTPS certificate
     *
     * Example: crt.https.get --id 1
     *
     * @param certId Certificate ID
     */
    @ShellMethod(key = "crt.https.get", value = "Get a specific HTTPS certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void getHttpsCertificate(
            @ShellOption(value = { "--id", "-i" }) String certId) throws Exception {
        String target = String.format("/redfish/v1/Managers/%s/NetworkProtocol/HTTPS/Certificates/%s/",
                                      MANAGER_ID, certId);
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Install a new HTTPS certificate
     *
     * Example: crt.https.install --cert /path/to/cert.pem
     *
     * @param certFile Path to the certificate file
     * @param type Certificate type (default: PEM)
     */
    @ShellMethod(key = "crt.https.install", value = "Install a new HTTPS certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void installHttpsCertificate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--type", "-t" }, defaultValue = "PEM") String type) throws Exception {
        
        String target = String.format("/redfish/v1/Managers/%s/NetworkProtocol/HTTPS/Certificates/", MANAGER_ID);
        installCertificate(target, certFile, type);
    }

    // ==================== LDAP CERTIFICATES ====================

    /**
     * List all LDAP certificates
     *
     * Example: crt.ldap.list
     */
    @ShellMethod(key = "crt.ldap.list", value = "List all LDAP certificates")
    @ShellMethodAvailability("availabilityCheck")
    public void listLdapCertificates() throws Exception {
        String target = "/redfish/v1/AccountService/LDAP/Certificates/";
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Get a specific LDAP certificate
     *
     * Example: crt.ldap.get --id 1
     *
     * @param certId Certificate ID
     */
    @ShellMethod(key = "crt.ldap.get", value = "Get a specific LDAP certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void getLdapCertificate(
            @ShellOption(value = { "--id", "-i" }) String certId) throws Exception {
        String target = String.format("/redfish/v1/AccountService/LDAP/Certificates/%s/", certId);
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Install a new LDAP certificate
     *
     * Example: crt.ldap.install --cert /path/to/cert.pem
     *
     * @param certFile Path to the certificate file
     * @param type Certificate type (default: PEM)
     */
    @ShellMethod(key = "crt.ldap.install", value = "Install a new LDAP certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void installLdapCertificate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--type", "-t" }, defaultValue = "PEM") String type) throws Exception {
        
        String target = "/redfish/v1/AccountService/LDAP/Certificates/";
        installCertificate(target, certFile, type);
    }

    /**
     * Delete an LDAP certificate
     *
     * Example: crt.ldap.delete --id 1
     *
     * @param certId Certificate ID to delete
     */
    @ShellMethod(key = "crt.ldap.delete", value = "Delete an LDAP certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void deleteLdapCertificate(
            @ShellOption(value = { "--id", "-i" }) String certId) throws Exception {
        String target = String.format("/redfish/v1/AccountService/LDAP/Certificates/%s/", certId);
        System.out.println(makeDeleteRequest(target));
    }

    // ==================== TRUSTSTORE CERTIFICATES ====================

    /**
     * List all TrustStore certificates
     *
     * Example: crt.truststore.list
     */
    @ShellMethod(key = "crt.truststore.list", value = "List all TrustStore certificates")
    @ShellMethodAvailability("availabilityCheck")
    public void listTruststoreCertificates() throws Exception {
        String target = String.format("/redfish/v1/Managers/%s/Truststore/Certificates/", MANAGER_ID);
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Get a specific TrustStore certificate
     *
     * Example: crt.truststore.get --id 1
     *
     * @param certId Certificate ID
     */
    @ShellMethod(key = "crt.truststore.get", value = "Get a specific TrustStore certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void getTruststoreCertificate(
            @ShellOption(value = { "--id", "-i" }) String certId) throws Exception {
        String target = String.format("/redfish/v1/Managers/%s/Truststore/Certificates/%s/",
                                      MANAGER_ID, certId);
        System.out.println(makeGetRequest(target, ""));
    }

    /**
     * Install a new TrustStore certificate
     *
     * Example: crt.truststore.install --cert /path/to/cert.pem
     *
     * @param certFile Path to the certificate file
     * @param type Certificate type (default: PEM)
     */
    @ShellMethod(key = "crt.truststore.install", value = "Install a new TrustStore certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void installTruststoreCertificate(
            @ShellOption(value = { "--cert", "-c" }, valueProvider = FileCompleter.class) String certFile,
            @ShellOption(value = { "--type", "-t" }, defaultValue = "PEM") String type) throws Exception {
        
        String target = String.format("/redfish/v1/Managers/%s/Truststore/Certificates/", MANAGER_ID);
        installCertificate(target, certFile, type);
    }

    /**
     * Delete a TrustStore certificate
     *
     * Example: crt.truststore.delete --id 1
     *
     * @param certId Certificate ID to delete
     */
    @ShellMethod(key = "crt.truststore.delete", value = "Delete a TrustStore certificate")
    @ShellMethodAvailability("availabilityCheck")
    public void deleteTruststoreCertificate(
            @ShellOption(value = { "--id", "-i" }) String certId) throws Exception {
        String target = String.format("/redfish/v1/Managers/%s/Truststore/Certificates/%s/",
                                      MANAGER_ID, certId);
        System.out.println(makeDeleteRequest(target));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Common method to install a certificate
     */
    private void installCertificate(String target, String certFile, String type) throws Exception {
        String certString = readCertificateFile(certFile);
        String body = String.format("{\"CertificateString\":\"%s\",\"CertificateType\":\"%s\"}",
                                   escapeCertString(certString), type);
        System.out.println(makePostRequest(target, body, "application/json"));
    }

    /**
     * Read certificate file content
     */
    private String readCertificateFile(String filePath) throws IOException {
        File file = new File(filePath);
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Escape certificate string for JSON
     */
    private String escapeCertString(String certString) {
        return certString.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
    }
}
