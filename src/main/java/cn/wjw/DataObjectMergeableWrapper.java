package cn.wjw;

import org.jdom2.Element;

import java.util.List;

public interface DataObjectMergeableWrapper extends DataObject {

    DataObjectMergeableWrapper insert(DataObject dataObject, List<String> fieldsList);

    void update(DataObject dataObject, List<String> fieldsList);

    void delete(DataObject child);
}
