package com.ibm.bmcshell.rest;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

public class PemUtils {

    public static Certificate readPemFile(String filePath) throws Exception {
        // Read the file
        String pemFile = new String(Files.readAllBytes(Paths.get(filePath)));

        // Remove the first and last lines
        String encoded = pemFile
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", ""); // This will remove newlines and spaces

        // Base64 decode the result
        byte[] decoded = Base64.getDecoder().decode(encoded);

        // Generate a certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(new ByteArrayInputStream(decoded));
    }
}
