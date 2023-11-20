package com.ibm.bmcshell.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SSHShellClient {
    public static int port=22;
    public static void runShell(String host,String user,String password){

        runShell(host,user,password,port);
    }
    public static void runShell(String host, String user, String password,int port)
    {
        try {
            JSch jsch = new JSch();



            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("shell");
            channel.setInputStream(System.in,true);
            channel.setOutputStream(System.out,true);

            channel.connect();
            while (!channel.isClosed()) {
                Thread.sleep(1000);
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void runCommand(String host, String user, String password, String command)
    {
        try {
            JSch jsch = new JSch();



            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");


            channel.setCommand(command);
            channel.setInputStream(System.in,true);
            channel.setOutputStream(System.out,true);
            InputStream in = channel.getInputStream();

            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("\nStandard Error: ");
            while ((line = errorReader.readLine()) != null) {
                System.out.println(line);
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {

        }
    }
}

