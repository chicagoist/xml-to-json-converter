package com.example;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveDownloader {

    private static final String ZIP_URL = "https://www.awrportal.de/temp/HSCTWXX_I_00054_new-schema.zip";
    private static final String DOWNLOAD_DIR = "src/main/resources/downloads";
    private static final String EXTRACT_DIR = "src/main/resources/extracted";
    private static final String JSON_OUTPUT_DIR = "src/main/resources/json_output";

    public static void main(String[] args) {
        try {
            downloadZip(ZIP_URL, DOWNLOAD_DIR);
            unzipFile(DOWNLOAD_DIR + "/HSCTWXX_I_00054_new-schema.zip", EXTRACT_DIR);
            createDirectoryIfNotExists(JSON_OUTPUT_DIR);  // Ensure the JSON output directory exists
            processXmlFiles(EXTRACT_DIR, JSON_OUTPUT_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadZip(String urlString, String downloadDir) throws IOException {
        URL url = new URL(urlString);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        File downloadDirectory = new File(downloadDir);
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs();
        }
        FileOutputStream fis = new FileOutputStream(downloadDir + "/HSCTWXX_I_00054_new-schema.zip");
        byte[] buffer = new byte[1024];
        int count;
        while ((count = bis.read(buffer, 0, 1024)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }

    private static void unzipFile(String zipFilePath, String destDir) throws IOException {
        File destDirectory = new File(destDir);
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }
        ZipFile zipFile = new ZipFile(zipFilePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                entryDestination.getParentFile().mkdirs();
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                FileOutputStream fos = new FileOutputStream(entryDestination);
                byte[] buffer = new byte[1024];
                int count;
                while ((count = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                bis.close();
            }
        }
        zipFile.close();
    }

    private static void processXmlFiles(String extractDir, String outputDir) throws Exception {
        Files.walk(Paths.get(extractDir))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml"))
                .forEach(path -> {
                    try {
                        XmlToJsonConverter.convert(path.toString(), outputDir);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void createDirectoryIfNotExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
