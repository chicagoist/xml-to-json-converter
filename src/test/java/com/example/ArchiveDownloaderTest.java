package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArchiveDownloaderTest {

    private static final String ZIP_URL = "https://www.awrportal.de/temp/HSCTWXX_I_00054_new-schema.zip";
    private static final String DOWNLOAD_DIR = "src/test/resources/downloads";
    private static final String EXTRACT_DIR = "src/test/resources/extracted";
    private static final String JSON_OUTPUT_DIR = "src/test/resources/json_output";

    @BeforeEach
    public void setUp() throws IOException {
        cleanDirectory(Paths.get(DOWNLOAD_DIR).toFile());
        cleanDirectory(Paths.get(EXTRACT_DIR).toFile());
        cleanDirectory(Paths.get(JSON_OUTPUT_DIR).toFile());
    }

    @Test
    public void testDownloadZip() throws Exception {
        Method method = ArchiveDownloader.class.getDeclaredMethod("downloadZip", String.class, String.class);
        method.setAccessible(true);

        method.invoke(null, ZIP_URL, DOWNLOAD_DIR);

        File downloadedFile = new File(DOWNLOAD_DIR + "/HSCTWXX_I_00054_new-schema.zip");
        assertTrue(downloadedFile.exists(), "The ZIP file should be downloaded.");
    }

    @Test
    public void testUnzipFile() throws Exception {
        testDownloadZip();

        Method method = ArchiveDownloader.class.getDeclaredMethod("unzipFile", String.class, String.class);
        method.setAccessible(true);

        method.invoke(null, DOWNLOAD_DIR + "/HSCTWXX_I_00054_new-schema.zip", EXTRACT_DIR);

        File extractedDir = new File(EXTRACT_DIR);
        assertTrue(extractedDir.listFiles().length > 0, "The ZIP file should be extracted.");
    }

    @Test
    public void testCreateDirectoryIfNotExists() throws Exception {
        Method method = ArchiveDownloader.class.getDeclaredMethod("createDirectoryIfNotExists", String.class);
        method.setAccessible(true);

        String testDirPath = JSON_OUTPUT_DIR + "/testDir";
        File testDir = new File(testDirPath);
        if (testDir.exists()) {
            testDir.delete();
        }

        method.invoke(null, testDirPath);

        assertTrue(testDir.exists(), "The directory should be created.");
    }

    @Test
    public void testProcessXmlFiles() throws Exception {
        testUnzipFile();

        File dummyXmlFile = new File(EXTRACT_DIR + "/dummy.xml");
        dummyXmlFile.createNewFile();

        Method method = ArchiveDownloader.class.getDeclaredMethod("processXmlFiles", String.class, String.class);
        method.setAccessible(true);

        method.invoke(null, EXTRACT_DIR, JSON_OUTPUT_DIR);

        File outputDir = new File(JSON_OUTPUT_DIR);
        assertTrue(outputDir.listFiles().length > 0, "The XML files should be processed to JSON.");
    }

    private void cleanDirectory(File directory) throws IOException {
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
                file.delete();
            }
        } else {
            directory.mkdirs();
        }
    }
}
