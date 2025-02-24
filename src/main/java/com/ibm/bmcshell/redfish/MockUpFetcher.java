package com.ibm.bmcshell.redfish;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



public class MockUpFetcher {
    public static Map<String, JsonNode> fetch(String regex) {
        Map<String, JsonNode> schemaFiles = new HashMap<>();
        Pattern pattern = regex != null && !regex.equals("*") ? Pattern.compile(regex) : null;
     
        ClassLoader classLoader = MockUpFetcher.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream("DSP2043_2024.1.zip");
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                if (!zipEntry.isDirectory() && fileName.endsWith(".json")
                && (pattern == null || pattern.matcher(fileName).matches())) {
                    
                    StringBuilder fileContent = new StringBuilder();
                    Scanner scanner = new Scanner(zis);
                    while (scanner.hasNextLine()) {
                        fileContent.append(scanner.nextLine());
                    }
                    try{
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(fileContent.toString());
                        if(jsonNode == null){
                            continue;
                        }
                        schemaFiles.put(fileName, jsonNode);
                    }
                    catch (IOException e){
                       //ignore parsing error
                    }
                    
                    // scanner.close();
                }
                
            }
            zis.closeEntry();
        } catch (IOException e) {
           e.printStackTrace();
        }

        return schemaFiles;
    }
}
