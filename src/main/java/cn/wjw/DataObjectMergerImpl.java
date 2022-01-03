package cn.wjw;

import java.util.*;
import java.util.stream.Collectors;

public class DataObjectMergerImpl implements DataObjectMerger {

    private final XmlMetadataContext xmlMetadataContext;

    public DataObjectMergerImpl(XmlMetadataContext xmlMetadataContext) {
        this.xmlMetadataContext = xmlMetadataContext;
    }

    @Override
    public void merge(DataObject source, DataObject target) {
        String pwsInputName = source.getName();
        ObjectDefinition rootObjectDefinition = xmlMetadataContext.get(pwsInputName);
        String rootTableName = rootObjectDefinition.getTableName();

        // 源数据、目标数据的根节点，与元数据的根节点必须匹配
        if (!source.getName().equals(rootTableName) || !target.getName().equals(rootTableName) ) {
            throw new RuntimeException();
        }

        DataObjectMergeableWrapper targetWrapper = new DataObjectMergeableWrapperImpl(target);
        this.mergeInternal(source, targetWrapper, rootObjectDefinition);
    }

    private void mergeInternal(DataObject source, DataObjectMergeableWrapper target, ObjectDefinition parentObjectDefinition) {

        Map<String, TableDefinition> childrenTableGroupMap = parentObjectDefinition.getChildrenTableGroupMap();
        // 遍历子表元数据
        for (String tableName : childrenTableGroupMap.keySet()) {

            // 将源数据、目标数据按ProductBaseId分组
            Map<String, List<DataObject>> sourceProductObjectListMap = getChildrenProductObjectListMap(source, tableName);
            Map<String, List<DataObjectMergeableWrapper>> targetProductObjectListMap = target.getChildren(tableName).stream()
                    .map(DataObjectMergeableWrapperImpl::new)
                    .collect(Collectors.groupingBy(this::getProductBaseId));

            TableDefinition tableDefinition = childrenTableGroupMap.get(tableName);
            Map<String, ObjectDefinition> objectGroupMap = tableDefinition.getObjectGroupMap();
            // 遍历源数据产品对象
            for (String productBaseId: objectGroupMap.keySet()) {

                boolean existSourceProductObject = sourceProductObjectListMap.containsKey(productBaseId);
                boolean existTargetProductObject = targetProductObjectListMap.containsKey(productBaseId);
                ObjectDefinition objectDefinition = objectGroupMap.get(productBaseId);
                int min = objectDefinition.getMin();
                // 如果产品对象定义的最小值大于0，执行非空校验
                if (min > 0 && !existSourceProductObject) {
                    throw new RuntimeException();
                }

                // 如果源产品对象和目标产品对象都为空，则不需要执行合并
                if (!existSourceProductObject && !existTargetProductObject) {
                    continue;
                }

                // 如果源产品对象为空，且目标产品对象不为空，则对所有目标产品对象执行递归删除
                if (!existSourceProductObject) {
                    for (DataObjectMergeableWrapper targetProductObject: targetProductObjectListMap.get(productBaseId)) {
                        target.delete(targetProductObject);
                        this.removeInternal(targetProductObject, objectDefinition);
                    }
                    continue;
                }

                int max = objectDefinition.getMax();
                List<DataObject> sourceProductObjectList = sourceProductObjectListMap.get(productBaseId);
                // 源数据的产品对象的数量，在产品对象定义的数量区间之外时，抛出异常
                if (sourceProductObjectList.size() < min || sourceProductObjectList.size() > max) {
                    throw new RuntimeException();
                }

                List<String> fieldsList = objectDefinition.getFieldsList();
                // 如果源产品对象不为空，目标产品对象为空，则对所有源产品对象执行递归插入
                if (!existTargetProductObject) {
                    for (DataObject sourceProductObject: sourceProductObjectListMap.get(productBaseId)) {
                        DataObjectMergeableWrapper targetProductObject = target.insert(sourceProductObject, fieldsList);
                        this.appendInternal(sourceProductObject, targetProductObject, objectDefinition);
                    }
                    continue;
                }

                // 源产品对象和目标产品对象都不为空时，执行合并
                List<DataObjectMergeableWrapper> targetProductObjectList = targetProductObjectListMap.get(productBaseId);

                /*
                 * 产品对象定义的最大值等于1时，如果要删除，则源产品对象必定为空;如果要插入，则目标产品对象必定为空.
                 * 这两种情况在之前的处理中已经包含了，此处只需要执行更新即可
                 * 因为最大数量是1，能直接确定1对1的匹配关系，所以直接执行更新
                 */
                if (max == 1) {
                    DataObject sourceProductObject = sourceProductObjectList.get(0);
                    DataObjectMergeableWrapper targetProductObject = targetProductObjectList.get(0);
                    // 更新处理
                    targetProductObject.update(sourceProductObject, fieldsList);
                    // 递归遍历，更新下一层
                    this.mergeInternal(sourceProductObject, targetProductObject, objectDefinition);
                    continue;
                }

                /*
                 * 产品对象定义的最大值大于1时，需要根据以下条件判断执行哪种处理：
                 * 源产品对象的PK_ID等于-1时，视作插入对象
                 * 源产品对象的PK_ID不等于1时，视作更新对象
                 * 除去被更新的产品对象，剩余的目标产品对象视作删除对象
                 */
                Map<String, DataObjectMergeableWrapper> targetProductObjectMap = null;
                for (DataObject sourceProductObject: sourceProductObjectList) {
                    String pkId = this.getPkId(sourceProductObject);

                    // PK_ID等于-1时，作为插入对象
                    if ("-1".equals(pkId)) {
                        // 递归插入源产品对象及其子产品对象
                        DataObjectMergeableWrapper targetProductObject = target.insert(sourceProductObject, fieldsList);
                        this.appendInternal(sourceProductObject, targetProductObject, objectDefinition);
                        continue;
                    }

                    // 源数据中存在需要更新的产品对象时，创建目标数据的PK_ID与产品对象的Map
                    if (Objects.isNull(targetProductObjectMap)) {
                        targetProductObjectMap = targetProductObjectList.stream()
                                .collect(Collectors.toMap(this::getPkId, productObject -> productObject));
                    }

                    if (!targetProductObjectMap.containsKey(pkId)) {
                        throw new RuntimeException();
                    }

                    // 取得被更新的产品对象，并将其从Map中删除。最后剩余的Map元素就是需要被删除的产品对象
                    DataObjectMergeableWrapper targetProductObject = targetProductObjectMap.remove(pkId);
                    // 递归更新源产品对象及其子产品对象
                    targetProductObject.update(sourceProductObject, fieldsList);
                    this.mergeInternal(sourceProductObject, targetProductObject, objectDefinition);
                }

                // 递归删除目标产品对象及其子产品对象
                if (Objects.nonNull(targetProductObjectMap)) {
                    for (DataObjectMergeableWrapper targetProductObject: targetProductObjectMap.values()) {
                        target.delete(targetProductObject);
                        this.removeInternal(targetProductObject, objectDefinition);
                    }
                }
            }
        }
    }

    @Override
    public void append(DataObject source, DataObject target) {
        String pwsInputName = target.getName();
        ObjectDefinition rootTableMetadata = xmlMetadataContext.get(pwsInputName);
        String rootTableName = rootTableMetadata.getTableName();

        if (!source.getName().equals(rootTableName)) {
            throw new RuntimeException();
        }

        DataObjectMergeableWrapper targetWrapper = new DataObjectMergeableWrapperImpl(target);
        this.appendInternal(source, targetWrapper, rootTableMetadata);
    }

    private void appendInternal(DataObject source, DataObjectMergeableWrapper target, ObjectDefinition parentObjectDefinition) {
        Map<String, TableDefinition> childrenTableGroupMap = parentObjectDefinition.getChildrenTableGroupMap();
        for(String tableName: childrenTableGroupMap.keySet()) {

            // 将源产品对象按ProductBaseId分组
            Map<String, List<DataObject>> sourceProductObjectListMap = getChildrenProductObjectListMap(source, tableName);

            TableDefinition tableDefinition = childrenTableGroupMap.get(tableName);
            Map<String, ObjectDefinition> objectGroupMap = tableDefinition.getObjectGroupMap();
            for(String productBaseId: objectGroupMap.keySet()) {

                ObjectDefinition objectDefinition = objectGroupMap.get(productBaseId);
                int min = objectDefinition.getMin();
                boolean existSourceProductObject = sourceProductObjectListMap.containsKey(productBaseId);

                if(min > 0 && !existSourceProductObject) {
                    throw new RuntimeException();
                }

                if (!existSourceProductObject) {
                    continue;
                }

                // 递归删除目标产品对象及其子产品对象
                List<String> fieldsList = objectDefinition.getFieldsList();
                for (DataObject sourceProductObject: sourceProductObjectListMap.get(productBaseId)) {
                    DataObjectMergeableWrapper targetProductObject = target.insert(sourceProductObject, fieldsList);
                    this.appendInternal(sourceProductObject, targetProductObject, objectDefinition);
                }

            }
        }
    }

    private void removeInternal(DataObjectMergeableWrapper target, ObjectDefinition parentObjectDefinition) {
        Map<String, TableDefinition> childrenTableGroupMap = parentObjectDefinition.getChildrenTableGroupMap();
        for(String tableName: childrenTableGroupMap.keySet()) {

            Map<String, List<DataObjectMergeableWrapper>> targetProductObjectListMap = target.getChildren(tableName).stream()
                    .map(DataObjectMergeableWrapperImpl::new)
                    .collect(Collectors.groupingBy(this::getProductBaseId));

            Map<String, ObjectDefinition> objectGroupMap = childrenTableGroupMap.get(tableName).getObjectGroupMap();
            for(String productBaseId: objectGroupMap.keySet()) {

                boolean existTargetProductObject = targetProductObjectListMap.containsKey(productBaseId);

                // 如果目标产品对象不存在，则跳过产品对象及其子产品对象的删除处理
                if(!existTargetProductObject) {
                    continue;
                }

                ObjectDefinition objectDefinition = objectGroupMap.get(productBaseId);
                // 递归删除
                for (DataObjectMergeableWrapper targetProductObject: targetProductObjectListMap.get(productBaseId)) {
                    target.delete(targetProductObject);
                    this.removeInternal(targetProductObject, objectDefinition);
                }
            }
        }
    }

    private Map<String, List<DataObject>> getChildrenProductObjectListMap(DataObject dataObject, String tableName) {
        return dataObject.getChildren(tableName).stream().collect(Collectors.groupingBy(this::getProductBaseId));
    }

    private String getPkId(DataObject productObject) {
        return productObject.getAttributeValue("PK_ID");
    }

    private String getProductBaseId(DataObject productObject) {
        String productBaseId = productObject.getAttributeValue("PRODUCT_BASE_ID");
        return productBaseId == null || "".equals(productBaseId) ? XmlMetadataContext.UNDEFINED_VALUE: productBaseId;
    }

}
