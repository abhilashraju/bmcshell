package com.ibm.bmcshell.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import com.github.luben.zstd.ZstdInputStream;

public class EbmcDumpUtil {

    public static void listArchive(String archivePath, String compression) throws IOException {
        try (InputStream fileIn = Files.newInputStream(Paths.get(archivePath));
             InputStream compIn = getCompressorStream(fileIn, compression);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(compIn)) {

            System.out.println("Contents of archive:");
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                System.out.println(entry.getName() + (entry.isDirectory() ? "/" : ""));
            }
        }
    }

    public static void extractArchive(String archivePath, String compression, String outputDir) throws IOException {
        Path outDir = Paths.get(outputDir);
        Files.createDirectories(outDir);

        try (InputStream fileIn = Files.newInputStream(Paths.get(archivePath));
             InputStream compIn = getCompressorStream(fileIn, compression);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(compIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                Path entryPath = outDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(outDir)) {
                    throw new IOException("Bad entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        tarIn.transferTo(out);
                    }
                }
            }
        }
        System.out.println("Extraction complete to: " + outputDir);
    }

    private static InputStream getCompressorStream(InputStream in, String compression) throws IOException {
        switch (compression) {
            case "xz":
                return new XZCompressorInputStream(in);
            case "zstd":
                return new ZstdInputStream(in);
            default:
                throw new IllegalArgumentException("Unsupported compression: " + compression);
        }
    }

    // Example main for testing
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java EbmcDumpUtil <list|extract> <archive.tar.xz|archive.tar.zst> <xz|zstd> [outputDir]");
            return;
        }
        String action = args[0];
        String archive = args[1];
        String compression = args[2];
        if ("list".equals(action)) {
            listArchive(archive, compression);
        } else if ("extract".equals(action)) {
            String outDir = args.length > 3 ? args[3] : archive + "_out";
            extractArchive(archive, compression, outDir);
        }
    }
}