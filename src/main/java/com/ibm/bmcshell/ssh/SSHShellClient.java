package com.ibm.bmcshell.ssh;

import com.ibm.bmcshell.ColorPrinter;
import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SSHShellClient {
    static {
        JSch.setLogger(new Logger() {
            public boolean isEnabled(int level) {
                return true;
            }

            public void log(int level, String message) {
//                System.out.println(message);
            }
        });


    }
    public static int port=22;
    public static void runShell(String host,String user,String password){

        runShell(host,user,password,port);
    }
    static void  printSession(String host, String user, String password,int port)
    {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            var serverHostKey = session.getHostKey();
            System.out.println(serverHostKey);

            session.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }
    public static void runShell(String host, String user, String password,int port)
    {
        try{

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
    public static void runCommand(String host, String user, String password, String command, StringBuffer buffer)
    {
        try {

            JSch jsch = new JSch();
//            jsch.setConfig("kex", "hmac-sha2-256");


            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");


            channel.setCommand(command);
//            channel.setInputStream(System.in,true);
            channel.setOutputStream(System.out);
            InputStream in = channel.getInputStream();

            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            channel.setInputStream(null);
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
//            jsch.setConfig("kex", "hmac-sha2-256");


            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");


            channel.setCommand(command);
//            channel.setInputStream(System.in,true);
            channel.setOutputStream(System.out);
            InputStream in = channel.getInputStream();

            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(ColorPrinter.addColor(line, "green"));
            }
            System.out.println("\nStandard Error: ");
            while ((line = errorReader.readLine()) != null) {
                System.out.println(ColorPrinter.red(line));
            }
            channel.setInputStream(null);
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {

        }
    }
}

