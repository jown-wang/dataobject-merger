package cn.wjw;

import org.jdom2.Content;
import org.jdom2.Element;

import java.util.List;

public class DataObjectMergeableWrapperImpl implements DataObjectMergeableWrapper {

    private final DataObject dataObject;

    public DataObjectMergeableWrapperImpl(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    @Override
    public DataObjectMergeableWrapper insert(DataObject dataObject, List<String> fieldsList) {

        DataObject child = this.dataObject.addContent(dataObject.getName());
        for (String field : fieldsList) {
            child.setAttribute(field, dataObject.getAttributeValue(field));
        }
        return new DataObjectMergeableWrapperImpl(child);
    }

    @Override
    public void update(DataObject dataObject, List<String> fieldsList) {
        for (String field : fieldsList) {
            this.dataObject.setAttribute(field, dataObject.getAttributeValue(field));
        }
    }

    @Override
    public void delete(DataObject child) {
        Element element = this.getElement();
        element.removeContent(child.getElement());
    }

    @Override
    public String getName() {
        return this.dataObject.getName();
    }

    @Override
    public List<DataObject> getChildren(final String cname) {
        return this.dataObject.getChildren(cname);
    }

    @Override
    public String getAttributeValue(final String attName) {
        return this.dataObject.getAttributeValue(attName);
    }

    @Override
    public void setAttribute(String name, String value) {
        this.dataObject.setAttribute(name, value);
    }

    @Override
    public DataObject addContent(String str) {
        return this.dataObject.addContent(str);
    }

    @Override
    public Element getElement() {
        return this.dataObject.getElement();
    }
}
