package com.ibm.bmcshell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ShellComponent
public class AccountServiceCommand extends CommonCommands {
    @Component
    public static class AcfFilePathProvider implements ValueProvider {
       
        @Override
        public List<CompletionProposal> complete(CompletionContext context) {
            var fileNames=getAcfFiles(libPath+"/acffiles",".acf");
            String userInput = context.currentWordUpToCursor();
            return fileNames
                    .stream()
                    .filter(name -> name.startsWith(userInput))
                    .map(CompletionProposal::new)
                    .collect(Collectors.toList());

        }

        public static List<String> getAcfFiles(String directoryPath,String currentExtension) {
            List<String> acfFiles = new ArrayList<>();
            try {
                acfFiles = Files.list(Paths.get(directoryPath))
                        .filter(Files::isRegularFile) // only files
                        .filter(path -> path.toString().endsWith(currentExtension)) // filter by extension
                        .map(path -> path.toAbsolutePath().toString()) // absolute path
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return acfFiles;
        }
    }

    protected AccountServiceCommand(DbusCommnads dbusCommnads) throws IOException {
        this.dbusCommnads = dbusCommnads;
    }

    private final DbusCommnads dbusCommnads;

    @ShellMethod(key = "as.create_user")
    @ShellMethodAvailability("availabilityCheck")
    public void createUser(@ShellOption(value = { "--name", "-n" }) String name,
            @ShellOption(value = { "--privilege", "-p" }, defaultValue = "Administrator") String privilege)
            throws URISyntaxException, IOException {

        // String args = String.format("%s %d %s %s true", name, 2, "ssh redfish",
        // privilege);
        // dbusCommnads.call("xyz.openbmc_project.User.Manager",
        // "/xyz/openbmc_project/user",
        // "xyz.openbmc_project.User.Manager",
        // "CreateUser",
        // "sassb", args);
        post("/redfish/v1/AccountService/Accounts",
                String.format(
                        "{\"UserName\": \"%s\", \"Password\": \"0penBmc0\", \"RoleId\": \"%s\", \"Enabled\": true}",
                        name, privilege),
                false);

    }

    @ShellMethod(key = "as.change_password")
    @ShellMethodAvailability("availabilityCheck")
    public void changePassword(@ShellOption(value = { "--name", "-n" }) String name,
            @ShellOption(value = { "--password", "-p" }) String passwString) throws URISyntaxException, IOException {
        String data = String.format("{\"Password\":\"%s\"}", passwString);
        patch(String.format("AccountService/Accounts/%s", name), data);

    }

    @ShellMethod(key = "as.install_acf")
    @ShellMethodAvailability("availabilityCheck")
    public void install_acf(@ShellOption(value = { "--file", "-f" },valueProvider = AcfFilePathProvider.class) String filepath)
            throws URISyntaxException, IOException {

        String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filepath)));
        ObjectMapper objectMapper = new ObjectMapper();
        String encodedContent = java.util.Base64.getEncoder().encodeToString(fileContent.getBytes());

        com.fasterxml.jackson.databind.node.ObjectNode acfFileNode = objectMapper.createObjectNode();
        acfFileNode.put("ACFFile", encodedContent);
        com.fasterxml.jackson.databind.node.ObjectNode oemNode = objectMapper.createObjectNode();
        oemNode.set("ACF", acfFileNode);
        com.fasterxml.jackson.databind.node.ObjectNode ibmNode = objectMapper.createObjectNode();
        ibmNode.set("IBM", oemNode);
        com.fasterxml.jackson.databind.node.ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("Oem", ibmNode);

        String data = objectMapper.writeValueAsString(rootNode);
        patch("/redfish/v1/AccountService/Accounts/service", data);
    }

    @ShellMethod(key = "as.delete_user")
    @ShellMethodAvailability("availabilityCheck")
    public String delete_user(@ShellOption(value = { "--name", "-n" }) String name)
            throws URISyntaxException, IOException {

        dbusCommnads.call("xyz.openbmc_project.User.Manager",
                "/xyz/openbmc_project/user/" + name,
                "xyz.openbmc_project.Object.Delete",
                "Delete", "", "");
        return "Finished";
    }

    @ShellMethod(key = "as.enablemfa")
    @ShellMethodAvailability("availabilityCheck")
    public void enablemfa(boolean enable) throws URISyntaxException, IOException {

        patch("/redfish/v1/AccountService/", toJson(new ObjectMapper().createObjectNode(),
                "MultiFactorAuth/GoogleAuthenticator/Enabled", (node, key) -> node.put(key, enable)));
    }

    @ShellMethod(key = "as.generateSecretKey")
    @ShellMethodAvailability("availabilityCheck")
    public void generateSecretKey(boolean enable) throws URISyntaxException, IOException {

        post(String.format("/redfish/v1/AccountService/Accounts/%s/Actions/ManagerAccount.GenerateSecretKey",
                getUserName()), "", false);

    }

    @ShellMethod(key = "as.password_expire", value = "eg: as.password_expire username ")
    void password_expire(String id) throws IOException, URISyntaxException {
        scmd(String.format("passwd --expire %s", id));

    }

    @ShellMethod(key = "as.delete_session", value = "eg: delete_session session-id . Deletes the session")
    void delete_session(String id) throws IOException, URISyntaxException {
        delete(String.format("SessionService/Session/%s", id));
    }

    @ShellMethod(key = "as.delete_all_sessions", value = "eg: as.delete_all_sessions ")
    void delete_all_sessions() throws IOException, URISyntaxException {
        scmd("systemctl stop bmcweb");
        scmd("rm /home/root/bmcweb_persistent_data.json");
        scmd("systemctl start bmcweb");
    }

    @ShellMethod(key = "as.verify_secret_key")
    @ShellMethodAvailability("availabilityCheck")
    public void verifySecretKey(String secret)
            throws URISyntaxException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String totpString = new TotpService().loadSecretString(secret).now(0);
        String data = String.format("{\"TimeBasedOneTimePassword\":\"%s\"}", totpString);
        post(String.format("AccountService/Accounts/%s/Actions/ManagerAccount.VerifyTimeBasedOneTimePassword",
                getUserName()), data, false);
        secretkey(secret);
    }

    @ShellMethod(key = "as.bypass_mfa")
    @ShellMethodAvailability("availabilityCheck")
    public void bypassMfa(@ShellOption(value = { "--bypass", "-b" }) boolean bypass)
            throws URISyntaxException, IOException {
        String data = String.format("{\"MFABypass\":{\"BypassTypes\":[\"%s\"]}}",
                bypass ? "GoogleAuthenticator"
                        : "None");
        patch(String.format("/redfish/v1/AccountService/Accounts/%s", getUserName()), data);
    }

    @ShellMethod(key = "as.clear_secret_key")
    @ShellMethodAvailability("availabilityCheck")
    public void clearSecretKey() throws URISyntaxException, IOException {
        post(String.format("/redfish/v1/AccountService/Accounts/%s/Actions/ManagerAccount.ClearSecretKey",
                getUserName()), "", false);
    }

    @ShellMethod(key = "as.acfupload", value = "eg: acfupload acfpath ")
    void acfupload(@ShellOption(value = { "--file", "-f" },valueProvider = AcfFilePathProvider.class) String imagepath) throws URISyntaxException {
        try {
            File file = new File(imagepath);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fileInputStream.read(fileBytes);
            fileInputStream.close();
            String encodedString = Base64.getEncoder().encodeToString(fileBytes);
            System.out.println("Base64 Encoded String: " + encodedString);
            patch("AccountService/Accounts/service",
                    String.format("{\"Oem\":{\"IBM\":{\"ACF\":{\"ACFFile\":\"%s\"}}}}", encodedString));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
