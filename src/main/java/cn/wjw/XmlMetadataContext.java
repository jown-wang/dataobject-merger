package cn.wjw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XmlMetadataContext {

    public static final String UNDEFINED_VALUE = "UNDEFINED";

    private static final String PRODUCT_OBJECT_DELIMITER = ".";

    private final Map<String, ObjectDefinition> cache = new ConcurrentHashMap<>();

    public ObjectDefinition get(String pwsInOutName) {
        if(cache.containsKey(pwsInOutName)) {
            return cache.get(pwsInOutName);
        }

        // TODO 从Pro获取配置，并保存到缓存中
        ObjectMapper mapper = new ObjectMapper();
        File configurationFile = new File("src\\main\\java\\cn\\wjw\\TestRequestConfiguration.json");
        XmlMetadataConfiguration xmlMetadataConfiguration;
        try {
            xmlMetadataConfiguration = mapper.readValue(configurationFile, XmlMetadataConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ObjectDefinition xmlDefinition = createXmlDefinition(xmlMetadataConfiguration);
        cache.put(pwsInOutName, xmlDefinition);

        return xmlDefinition;
    }

    public void reload() {
        cache.clear();
    }

    private ObjectDefinition createXmlDefinition(XmlMetadataConfiguration xmlMetadataConfiguration) {

        Map<String, ObjectDefinition> pathObjectDefinitionMap = new HashMap<>();
        ObjectDefinition root = null;

        for (XmlMetadataConfiguration.Row row : xmlMetadataConfiguration.getRows()) {

            String path = row.getProductObjectPath();
            // 取得父产品对象和当前产品对象的字符串值
            String parentPath = null;
            String productObjectStr;

            // 如果存在父产品对象
            if (path.contains(PRODUCT_OBJECT_DELIMITER)) {

                // 获取最后一个"."的位置
                int lastObjectDelimiterIndex = path.lastIndexOf(PRODUCT_OBJECT_DELIMITER);
                // 获取最后一个"."之后的字符串，即当前产品对象的表名和productBaseId;
                productObjectStr = path.substring(lastObjectDelimiterIndex + 1);
                // 截取最后一个"."之前的字符串，即父产品对象的path
                parentPath = path.substring(0, lastObjectDelimiterIndex);
            } else {
                productObjectStr = path;
            }

            // 取得表名和productBaseId
            String tableName;
            String productBaseId = null;
            // 如果存在productBaseId，分割表名和productBaseId
            if (productObjectStr.contains("[")) {
                String[] productObjectArray = productObjectStr.split("\\[");
                tableName = productObjectArray[0];
                productBaseId = productObjectArray[1].replace("]", "");
            } else {
                tableName = productObjectStr;
            }

            ObjectDefinition objectDefinition = new ObjectDefinition.Builder()
                    .tableName(tableName)
                    .productBaseId(productBaseId == null || "".equals(productBaseId) ? UNDEFINED_VALUE: productBaseId)
                    .max(row.getMax())
                    .min(row.getMin())
                    .fields(row.getFields())
                    .build();

            if(Objects.nonNull(parentPath)) {

                if (!pathObjectDefinitionMap.containsKey(parentPath)) {
                    throw new RuntimeException();
                }
                // 用父产品对象的path获取到父产品对象
                ObjectDefinition parentObjectDefinition = pathObjectDefinitionMap.get(parentPath);
                Map<String, TableDefinition> tableGroupMap = parentObjectDefinition.getChildrenTableGroupMap();
                if (!tableGroupMap.containsKey(tableName)) {
                    tableGroupMap.put(tableName, new TableDefinition());
                }
                TableDefinition tableDefinition = tableGroupMap.get(tableName);
                tableDefinition.getObjectGroupMap().put(objectDefinition.getProductBaseId(), objectDefinition);
            } else {
                root = objectDefinition;
            }

            pathObjectDefinitionMap.put(path, objectDefinition);
        }

        return root;
    }

    public static void main(String[] args) throws Exception {

        SAXBuilder saxBuilder = new SAXBuilder();
        Element source = saxBuilder.build(new File("src\\main\\java\\cn\\wjw\\testData1.xml")).getRootElement();
        Element target = saxBuilder.build(new File("src\\main\\java\\cn\\wjw\\testData2.xml")).getRootElement();

        XmlMetadataContext xmlMetadataContext = new XmlMetadataContext();
        DataObjectMerger dataObjectMerger = new DataObjectMergerImpl(xmlMetadataContext);
        dataObjectMerger.merge(new DataObjectImpl(source), new DataObjectMergeableWrapperImpl(new DataObjectImpl(target)));

        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.output(target, new FileOutputStream("src\\main\\java\\cn\\wjw\\output.xml"));
    }
}
