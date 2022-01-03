package cn.wjw;

import java.util.HashMap;
import java.util.Map;

public class TableDefinition {

    /** 用product_base_id分组的产品对象Map */
    private Map<String, ObjectDefinition> objectGroupMap = new HashMap<>();

    public Map<String, ObjectDefinition> getObjectGroupMap() {
        return objectGroupMap;
    }

    public void setObjectGroupMap(Map<String, ObjectDefinition> objectGroupMap) {
        this.objectGroupMap = objectGroupMap;
    }
}