package cn.wjw;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectDefinition {

    private String tableName;

    private String productBaseId;

    private int min;

    private int max;

    private List<String> fieldsList = new LinkedList<>();

    /** 用表名分组的子产品对象定义Map */
    private Map<String, TableDefinition> childrenTableGroupMap = new HashMap<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getProductBaseId() {
        return productBaseId;
    }

    public void setProductBaseId(String productBaseId) {
        this.productBaseId = productBaseId;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public List<String> getFieldsList() {
        return fieldsList;
    }

    public void setFieldsList(List<String> fieldsList) {
        this.fieldsList = fieldsList;
    }

    public Map<String, TableDefinition> getChildrenTableGroupMap() {
        return childrenTableGroupMap;
    }

    public void setChildrenTableGroupMap(Map<String, TableDefinition> childrenTableGroupMap) {
        this.childrenTableGroupMap = childrenTableGroupMap;
    }
}
