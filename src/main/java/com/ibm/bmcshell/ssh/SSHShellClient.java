package com.ibm.bmcshell.ssh;

import com.ibm.bmcshell.ColorPrinter;
import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static int connectionTimeout = 15000; // 30 seconds default timeout
    public static JSch jsch = new JSch();
    
    // Cache for SSH sessions keyed by connection string (host:port:user)
    private static Map<String, Session> sessionCache = new HashMap<>();
    
    // Currently selected session key for use with commands
    private static String activeSessionKey = null;
    
    /**
     * Generate a unique cache key for a connection
     */
    private static String getCacheKey(String host, String user, int port) {
        return host + ":" + port + ":" + user;
    }
    
    /**
     * Get or create a cached SSH session
     */
    static Session getSession(String host, String user, String password, int port){
        String cacheKey = getCacheKey(host, user, port);
        Session session = sessionCache.get(cacheKey);
        
        // Check if session exists and is still connected
        if(session != null && session.isConnected()){
            return session;
        }
        
        // Create new session if not cached or disconnected
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(connectionTimeout);
            
            // Cache the new session
            sessionCache.put(cacheKey, session);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
        
        return session;
    }
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
            session.connect(connectionTimeout);

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

               

                Session session = getSession(host, user, password, port);
                Channel channel = session.openChannel("shell");
                channel.setInputStream(System.in,true);
                channel.setOutputStream(System.out,true);

                channel.connect();
                while (!channel.isClosed()) {
                    Thread.sleep(1000);
                }
                channel.disconnect();
            } catch (Exception e) {
                e.printStackTrace();

            }


    }
    public static void runCommand(String host, String user, String password, String command, StringBuffer buffer)
    {
        try {

  

            Session session = getSession(host, user, password, port);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void runCommand(String host, String user, String password, String command)
    {
        try {

            Session session = getSession(host, user, password, port);
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
            if((line = errorReader.readLine())  != null){
                System.out.println("\nStandard Error: ");
                System.out.println(ColorPrinter.red(line));
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(ColorPrinter.red(line));
                }
            }
            channel.setInputStream(null);
            channel.disconnect();
        } catch (Exception e) {

        }
    }
    public static void runCommandShort(ByteArrayOutputStream out,String host, String user, String password, String command)
    {
        try {

            
//            jsch.setConfig("kex", "hmac-sha2-256");

            Session session = getSession(host, user, password,port);
           
            ChannelExec channel = (ChannelExec) session.openChannel("exec");


            channel.setCommand(command);
//            channel.setInputStream(System.in,true);
            channel.setOutputStream(out);
             // Set error stream to a separate stream to capture errors
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            channel.setErrStream(errStream);
            channel.connect();
 
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            if(errStream.size() > 0){
                System.out.println("\nStandard Error: ");
                System.out.println(ColorPrinter.red(errStream.toString()));
            }
            channel.setInputStream(null);
            channel.disconnect();
        } catch (Exception e) {

        }
    }
    public static void setPort(int port2) {
        if(port2 > 0 && port2 != port) {
            port = port2;
            // No need to clear sessions when port changes - cache handles it
        }
    }
    
    public static void setConnectionTimeout(int timeoutMs) {
        if(timeoutMs > 0) {
            connectionTimeout = timeoutMs;
        }
    }
    
    public static int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    /**
     * Clear a specific cached session
     */
    public static void clearSession(String host, String user, int port) {
        String cacheKey = getCacheKey(host, user, port);
        Session session = sessionCache.get(cacheKey);
        if(session != null && session.isConnected()){
            session.disconnect();
        }
        sessionCache.remove(cacheKey);
    }
    
    /**
     * Clear all cached sessions
     */
    public static void clearAllSessions() {
        for(Session session : sessionCache.values()) {
            if(session != null && session.isConnected()){
                session.disconnect();
            }
        }
        sessionCache.clear();
    }
    
    /**
     * Get all session cache keys
     * @return List of all session cache keys
     */
    public static List<String> getSessionKeys() {
        return new java.util.ArrayList<>(sessionCache.keySet());
    }
    
    /**
     * Get a session by its cache key
     * @param cacheKey The cache key (format: host:port:user)
     * @return The session if found, null otherwise
     */
    public static Session getSessionByKey(String cacheKey) {
        return sessionCache.get(cacheKey);
    }
    
    /**
     * List all active SSH sessions with their cache keys and connection status
     * @return A formatted string containing all session information
     */
    public static String listActiveSessions() {
        if(sessionCache.isEmpty()) {
            return "No active SSH sessions cached.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Active SSH Sessions:\n");
        sb.append("===================\n");
        
        int index = 1;
        for(Map.Entry<String, Session> entry : sessionCache.entrySet()) {
            String key = entry.getKey();
            Session session = entry.getValue();
            
            sb.append(index++).append(". ");
            sb.append("Key: ").append(key);
            
            if(session != null) {
                sb.append(" | Status: ");
                if(session.isConnected()) {
                    sb.append("CONNECTED");
                    try {
                        sb.append(" | Server: ").append(session.getHost());
                        sb.append(":").append(session.getPort());
                    } catch (Exception e) {
                        // Ignore if we can't get host/port
                    }
                } else {
                    sb.append("DISCONNECTED");
                }
            } else {
                sb.append(" | Status: NULL");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Set the active session for subsequent commands
     * @param cacheKey The cache key (format: host:port:user)
     * @return Status message
     */
    public static String setActiveSession(String cacheKey) {
        Session session = sessionCache.get(cacheKey);
        if(session == null) {
            return "Session not found: " + cacheKey;
        }
        
        if(!session.isConnected()) {
            return "Session is disconnected: " + cacheKey;
        }
        
        activeSessionKey = cacheKey;
        
        // Parse the cache key to extract host, port, and user
        String[] parts = cacheKey.split(":");
        if(parts.length == 3) {
            String host = parts[0];
            int sessionPort = Integer.parseInt(parts[1]);
            String user = parts[2];
            
            // Update the global port if different
            if(sessionPort != port) {
                port = sessionPort;
            }
            
            return String.format("Active session set to: %s (host=%s, port=%d, user=%s)",
                                cacheKey, host, sessionPort, user);
        }
        
        return "Active session set to: " + cacheKey;
    }
    
    /**
     * Get the currently active session key
     * @return The active session key, or null if none is set
     */
    public static String getActiveSessionKey() {
        return activeSessionKey;
    }
    
    /**
     * Clear the active session
     */
    public static void clearActiveSession() {
        activeSessionKey = null;
    }
    
    /**
     * Get information about the active session
     * @return Status message about the active session
     */
    public static String getActiveSessionInfo() {
        if(activeSessionKey == null) {
            return "No active session selected. Use 'select-ssh-session' to select one.";
        }
        
        Session session = sessionCache.get(activeSessionKey);
        if(session == null) {
            activeSessionKey = null;
            return "Active session no longer exists in cache.";
        }
        
        if(!session.isConnected()) {
            return String.format("Active session '%s' is DISCONNECTED", activeSessionKey);
        }
        
        try {
            return String.format("Active session: %s | Server: %s:%d | Status: CONNECTED",
                               activeSessionKey, session.getHost(), session.getPort());
        } catch (Exception e) {
            return String.format("Active session: %s | Status: CONNECTED", activeSessionKey);
        }
    }
}
