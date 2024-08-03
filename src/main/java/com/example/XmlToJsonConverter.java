package com.example;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class XmlToJsonConverter {

    private static final Logger logger = LoggerFactory.getLogger(XmlToJsonConverter.class);

    public static void convert(String xmlFilePath, String outputDir) throws Exception {
        logger.info("Converting XML file: {}", xmlFilePath);
        File xmlFile = new File(xmlFilePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList levelNodes = doc.getElementsByTagName("level");
        Map<String, Level> levels = new HashMap<>();
        for (int i = 0; i < levelNodes.getLength(); i++) {
            Node node = levelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element levelElement = (Element) node;
                Level level = new Level();
                level.LevelId = levelElement.getElementsByTagName("level_id").item(0).getTextContent();
                level.Length = Integer.parseInt(levelElement.getElementsByTagName("length").item(0).getTextContent());
                NodeList descriptionNodes = levelElement.getElementsByTagName("level_description");
                for (int j = 0; j < descriptionNodes.getLength(); j++) {
                    Element descriptionElement = (Element) descriptionNodes.item(j);
                    String language = descriptionElement.getElementsByTagName("language").item(0).getTextContent();
                    String text = descriptionElement.getElementsByTagName("text").item(0).getTextContent();
                    level.Descriptions.put(language, text);
                }
                levels.put(level.LevelId, level);
            }
        }

        NodeList numberDataNodes = doc.getElementsByTagName("number_data");
        List<CommodityCode> commodityCodeList = new ArrayList<>();
        for (int i = 0; i < numberDataNodes.getLength(); i++) {
            Node node = numberDataNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element numberDataElement = (Element) node;
                String levelId = numberDataElement.getElementsByTagName("level_id").item(0).getTextContent();
                if ("60".equals(levelId)) {
                    CommodityCode commodityCode = new CommodityCode();
                    commodityCode.CommodityCode = numberDataElement.getElementsByTagName("number").item(0).getTextContent();
                    commodityCode.CommodityCodeUniqueID = numberDataElement.getElementsByTagName("id").item(0).getTextContent();
                    commodityCode.ValidityStartDate = numberDataElement.getElementsByTagName("validity_begin").item(0).getTextContent();
                    commodityCode.ValidityEndDate = numberDataElement.getElementsByTagName("validity_end").item(0).getTextContent();
                    commodityCode.SupplementaryUnit = "KGM";  // Assuming

                    NodeList textNodes = numberDataElement.getElementsByTagName("official_description");
                    for (int j = 0; j < textNodes.getLength(); j++) {
                        Element textElement = (Element) textNodes.item(j);
                        String language = textElement.getElementsByTagName("language").item(0).getTextContent();
                        String text = textElement.getElementsByTagName("text").item(0).getTextContent();
                        commodityCode.CommodityCodeDescriptionList.add(new CommodityCodeDescription(language, text));
                    }

                    concatenateTexts(commodityCode, levels, numberDataElement.getElementsByTagName("parent_id").item(0).getTextContent());

                    commodityCodeList.add(commodityCode);
                }
            }
        }

        JsonStructure jsonStructure = new JsonStructure();
        jsonStructure.TrdClassfctnCntntVersion = 54;
        jsonStructure.TrdClassfctnCntntRevisionVers = 1;
        jsonStructure.IsInitialVersion = true;
        jsonStructure.IsLastDataPackage = true;
        jsonStructure.DataPackageOrdinalNumber = 1;
        jsonStructure.CommodityCodeLength = 11;
        jsonStructure.CommodityCodeList = commodityCodeList;

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(JsonStructure.class, new UppercaseKeysSerializer()).create();
        String json = gson.toJson(jsonStructure);
        FileWriter writer = new FileWriter(outputDir + "/HSCTWXXNUM_I_00054" + commodityCodeList.get(0).CommodityCode + ".json");
        writer.write(json);
        writer.close();
        logger.info("Converted XML to JSON: {}", outputDir + "/HSCTWXXNUM_I_00054" + commodityCodeList.get(0).CommodityCode + ".json");
    }

    private static void concatenateTexts(CommodityCode commodityCode, Map<String, Level> levels, String parentId) {
        StringBuilder enConcatenation = new StringBuilder();
        StringBuilder zhConcatenation = new StringBuilder();
        boolean first = true;
        while (parentId != null && !parentId.isEmpty()) {
            Level level = levels.get(parentId.substring(0, 2));
            if (level != null) {
                if (first) {
                    enConcatenation.append(level.Descriptions.get("EN"));
                    zhConcatenation.append(level.Descriptions.get("ZH"));
                    first = false;
                } else {
                    enConcatenation.insert(0, ", " + level.Descriptions.get("EN"));
                    zhConcatenation.insert(0, ", " + level.Descriptions.get("ZH"));
                }
            }
            parentId = parentId.substring(2);
        }

        for (CommodityCodeDescription description : commodityCode.CommodityCodeDescriptionList) {
            if ("EN".equals(description.LanguageISOCode)) {
                description.ConcatenatedTariffNumberName = enConcatenation.append(", ").append(description.CommodityCodeName).toString();
            } else if ("ZH".equals(description.LanguageISOCode)) {
                description.ConcatenatedCommodityCodeName = zhConcatenation.append(", ").append(description.CommodityCodeName).toString();
            }
        }
    }

    static class Level {
        String LevelId;
        int Length;
        Map<String, String> Descriptions = new HashMap<>();
    }

    static class JsonStructure {
        int TrdClassfctnCntntVersion;
        int TrdClassfctnCntntRevisionVers;
        boolean IsInitialVersion;
        boolean IsLastDataPackage;
        int DataPackageOrdinalNumber;
        int CommodityCodeLength;
        List<CommodityCode> CommodityCodeList;
    }

    static class CommodityCode {
        String CommodityCode;
        String CommodityCodeUniqueID;
        String ValidityStartDate;
        String ValidityEndDate;
        String SupplementaryUnit;
        List<CommodityCodeDescription> CommodityCodeDescriptionList = new ArrayList<>();
    }

    static class CommodityCodeDescription {
        String LanguageISOCode;
        String CommodityCodeName;
        String ConcatenatedTariffNumberName;
        String ConcatenatedCommodityCodeName;

        public CommodityCodeDescription(String languageISOCode, String commodityCodeName) {
            this.LanguageISOCode = languageISOCode;
            this.CommodityCodeName = commodityCodeName;
        }
    }

    static class UppercaseKeysSerializer implements JsonSerializer<Object> {
        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            Field[] fields = src.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String name = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                    jsonObject.add(name, context.serialize(field.get(src)));
                } catch (IllegalAccessException e) {
                    logger.error("Error serializing field: {}", field.getName(), e);
                }
            }
            return jsonObject;
        }
    }
}
