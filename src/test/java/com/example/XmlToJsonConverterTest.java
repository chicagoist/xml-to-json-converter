package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlToJsonConverterTest {

    private static final String TEST_RESOURCES_DIR = "src/test/resources";

    @BeforeEach
    public void setUp() throws Exception {
        // Ensure directories are clean before each test
        Files.createDirectories(Paths.get(TEST_RESOURCES_DIR));
    }

    @Test
    public void testConcatenateTexts() throws Exception {
        XmlToJsonConverter.CommodityCode commodityCode = new XmlToJsonConverter.CommodityCode();
        commodityCode.CommodityCodeDescriptionList.add(new XmlToJsonConverter.CommodityCodeDescription("EN", "Example EN"));
        commodityCode.CommodityCodeDescriptionList.add(new XmlToJsonConverter.CommodityCodeDescription("ZH", "Example ZH"));

        Map<String, XmlToJsonConverter.Level> levels = new HashMap<>();
        XmlToJsonConverter.Level level = new XmlToJsonConverter.Level();
        level.LevelId = "01";
        level.Descriptions.put("EN", "Level EN");
        level.Descriptions.put("ZH", "Level ZH");
        levels.put("01", level);

        XmlToJsonConverter instance = new XmlToJsonConverter();
        Method method = XmlToJsonConverter.class.getDeclaredMethod("concatenateTexts", XmlToJsonConverter.CommodityCode.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(instance, commodityCode, levels, "01");

        assertEquals("Level EN, Example EN", commodityCode.CommodityCodeDescriptionList.get(0).ConcatenatedTariffNumberName);
        assertEquals("Level ZH, Example ZH", commodityCode.CommodityCodeDescriptionList.get(1).ConcatenatedCommodityCodeName);
    }

    @Test
    public void testConvert() throws Exception {
        String xmlContent = "<root><level><level_id>01</level_id><length>10</length><level_description><language>EN</language><text>Level EN</text></level_description><level_description><language>ZH</language><text>Level ZH</text></level_description></level><number_data><level_id>60</level_id><number>12345</number><id>1</id><validity_begin>2024-01-01</validity_begin><validity_end>2024-12-31</validity_end><official_description><language>EN</language><text>Example EN</text></official_description><official_description><language>ZH</language><text>Example ZH</text></official_description><parent_id>01</parent_id></number_data></root>";
        String xmlFilePath = TEST_RESOURCES_DIR + "/test.xml";
        Files.write(Paths.get(xmlFilePath), xmlContent.getBytes());

        String outputDir = TEST_RESOURCES_DIR + "/output";
        Files.createDirectories(Paths.get(outputDir));

        XmlToJsonConverter.convert(xmlFilePath, outputDir);

        File outputFile = new File(outputDir + "/HSCTWXXNUM_I_0005412345.json");
        assertTrue(outputFile.exists());

    }
}
