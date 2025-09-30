package com.ibm.bmcshell;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.truffle.regex.tregex.util.json.Json;

@ShellComponent
public class CertificateCommands extends CommonCommands {
    protected CertificateCommands() throws Exception {
        super();
    }

    void installCertificate(String taget, String fileName, String type) throws Exception {
        File file = new File(fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);
        fileInputStream.close();
        String encodedString = Base64.getEncoder().encodeToString(fileBytes);
        String body = "{\"CertificateString\":\"" + fileBytes + "\",\"CertificateType\":\"" + type + "\"}";
        makePostRequest(taget, body, "application/json");
    }

    @ShellMethod(key = "crt.installTrustore", value = "eg: crt.install path_to_crt_file")
    @ShellMethodAvailability("availabilityCheck")
    public void installCertificate(String fileName,
            @ShellOption(value = { "-f", "--format" }, defaultValue = "PEM") String type) throws Exception {

        String target = "/redfish/v1/Managers/bmc/Truststore/Certificates/";
        installCertificate(target, fileName, type);
    }

    @ShellMethod(key = "crt.installHttps", value = "List all certificates in truststore")
    @ShellMethodAvailability("availabilityCheck")
    public void installHttpsCertificate(String fileName,
            @ShellOption(value = { "-f", "--format" }, defaultValue = "PEM") String type) throws Exception {
        String target = "/redfish/v1/Managers/bmc/NetworkProtocol/HTTPS/Certificates/";
        installCertificate(target, fileName, type);
    }
}
