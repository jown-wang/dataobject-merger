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

        // key是path的产品对象定义的map，在构造父子关系时，通过父节点的path从map中获取父节点。
        Map<String, ObjectDefinition> pathObjectDefinitionMap = new HashMap<>();

        List<XmlMetadataConfiguration.Row> rows = new ArrayList<>(xmlMetadataConfiguration.getRows());

        XmlMetadataConfiguration.Row firstRow = rows.remove(0);
        ObjectDefinition root = this.createRootObjectDefinition(firstRow);
        pathObjectDefinitionMap.put(firstRow.getProductObjectPath(), root);

        for (XmlMetadataConfiguration.Row row : rows) {

            ObjectDefinition objectDefinition = new ObjectDefinition();

            String path = row.getProductObjectPath();
            // 获取最后一个"."的位置
            int lastObjectDelimiterIndex = path.lastIndexOf(PRODUCT_OBJECT_DELIMITER);
            // 获取最后一个"."之后的字符串，即当前产品对象的表名和productBaseId;
            String productObjectStr = path.substring(lastObjectDelimiterIndex + 1);
            // 如果存在productBaseId，分割表名和productBaseId
            if (productObjectStr.contains("[")) {
                String[] productObjectArray = productObjectStr.split("\\[");
                objectDefinition.setTableName(productObjectArray[0]);
                objectDefinition.setProductBaseId(productObjectArray[1].replace("]", ""));
            } else {
                objectDefinition.setTableName(productObjectStr);
                objectDefinition.setProductBaseId(UNDEFINED_VALUE);
            }
            objectDefinition.setMin(row.getMin());
            objectDefinition.setMax(row.getMax());
            objectDefinition.setFieldsList(this.splitFields(row.getFields()));

            // 截取最后一个"."之前的字符串，即父产品对象的path
            String parentPath = path.substring(0, lastObjectDelimiterIndex);
            if (!pathObjectDefinitionMap.containsKey(parentPath)) {
                throw new RuntimeException();
            }
            // 用父产品对象的path获取到父产品对象
            ObjectDefinition parent = pathObjectDefinitionMap.get(parentPath);
            Map<String, TableDefinition> tableGroupMap = parent.getChildrenTableGroupMap();
            if (tableGroupMap.containsKey(objectDefinition.getTableName())) {
                TableDefinition tableDefinition = tableGroupMap.get(objectDefinition.getTableName());
                Map<String, ObjectDefinition> objectGroupMap = tableDefinition.getObjectGroupMap();

                if (objectGroupMap.containsKey(objectDefinition.getProductBaseId())) {
                    throw new RuntimeException();
                }
                objectGroupMap.put(objectDefinition.getProductBaseId(), objectDefinition);
            } else {
                TableDefinition tableDefinition = new TableDefinition();
                tableDefinition.getObjectGroupMap().put(objectDefinition.getProductBaseId(), objectDefinition);
                tableGroupMap.put(objectDefinition.getTableName(), tableDefinition);
            }

            pathObjectDefinitionMap.put(path, objectDefinition);
        }

        return root;
    }

    private ObjectDefinition createRootObjectDefinition(XmlMetadataConfiguration.Row rootRow) {
        ObjectDefinition root = new ObjectDefinition();
        root.setTableName(rootRow.getProductObjectPath());
        root.getFieldsList().addAll(this.splitFields(rootRow.getFields()));
        return root;
    }

    private List<String> splitFields(String fieldsStr) {

        if ("".equals(fieldsStr) || Objects.isNull(fieldsStr)) {
            return Collections.emptyList();
        }

        return List.of(fieldsStr.split(","));
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
