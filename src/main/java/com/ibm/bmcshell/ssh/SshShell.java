package com.ibm.bmcshell.ssh;


import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
@ShellComponent
public class SshShell {
    public static void main(String[] args) throws Exception {
        String username = "service";
        String password = "0penBmc0";
        String host = "rain104bmc.aus.stglabs.ibm.com";
        int port = 22;
        long defaultTimeoutSeconds = 10000l;
        String command = "ls\n";

        listFolderStructure(username, password, host, port, defaultTimeoutSeconds, command);
    }

    public static String listFolderStructure(String username, String password, String host, int port, long defaultTimeoutSeconds, String command) throws Exception {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        try (ClientSession session = client.connect(username, host, port)
                .verify(defaultTimeoutSeconds)
                .getSession()) {
            session.addPasswordIdentity(password);
//            session.auth()
//                    .verify(defaultTimeoutSeconds);
            try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                 ByteArrayOutputStream errorResponseStream = new ByteArrayOutputStream();
                 ClientChannel channel = session.createChannel(Channel.CHANNEL_SHELL)) {
                channel.setOut(responseStream);
                channel.setErr(errorResponseStream);
                try {
                    channel.open()
                            .verify(defaultTimeoutSeconds);
                    try (OutputStream pipedIn = channel.getInvertedIn()) {
                        pipedIn.write(command.getBytes());
                        pipedIn.flush();
                    }
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), defaultTimeoutSeconds*1000);
                    String errorString = new String(errorResponseStream.toByteArray());
                    if(!errorString.isEmpty()) {
                        throw new Exception(errorString);
                    }
                    String responseString = new String(responseStream.toByteArray());
                    return responseString;
                } finally {
                    channel.close(false);
                }
            }
        } finally {
            client.stop();
        }
    }
    @ShellMethod(key="ssh")
    String ssh() throws Exception {
        String username = "demo";
        String password = "password";
        String host = "test.rebex.net";
        int port = 22;
        long defaultTimeoutSeconds = 10l;
        String command = "ls\n";
        String responseString = listFolderStructure(username, password, host, port, defaultTimeoutSeconds, command);
        return responseString;
    }
}
