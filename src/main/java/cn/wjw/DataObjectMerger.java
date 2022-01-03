package cn.wjw;

import org.jdom2.Element;

public interface DataObjectMerger {

    void merge(DataObject source, DataObject target);

    void append(DataObject source, DataObject target);

}
