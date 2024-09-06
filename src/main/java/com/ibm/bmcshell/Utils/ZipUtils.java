package com.ibm.bmcshell.Utils;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class ZipUtils {

    public static void unzip(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists())
            dir.mkdirs();
        FileInputStream fis;
        // buffer for read and write data to file
        byte[] buffer = new byte[1024];
        fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(destDir + File.separator + fileName);
            System.out.println("Unzipping to " + newFile.getAbsolutePath());
            // create directories for sub directories in zip
            new File(newFile.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            // close this ZipEntry
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        // close last ZipEntry
        zis.closeEntry();
        zis.close();
        fis.close();
    }

    // Method to unzip a single .gz file
    public static void unzipGzFile(Path gzFilePath, Path destDir) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzFilePath.toFile()));
                FileOutputStream fos = new FileOutputStream(destDir.toFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    // Recursive method to traverse directories and unzip .gz files
    public static void unzipGzFilesRecursively(Path dir) throws IOException {
        // List all files and directories in the current directory
        List<Path> filesAndDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                filesAndDirs.add(entry);
            }
        }

        // Process directories first (leaf to root)
        for (Path entry : filesAndDirs) {
            if (Files.isDirectory(entry)) {
                unzipGzFilesRecursively(entry);
            }
        }

        // Process .gz files
        for (Path entry : filesAndDirs) {
            if (Files.isRegularFile(entry) && entry.toString().endsWith(".gz")) {
                Path destFile = Paths.get(entry.toString().replaceFirst("\\.gz$", ""));
                unzipGzFile(entry, destFile);
                Files.delete(entry); // Optionally delete the .gz file after extraction
            }
        }
    }
}