package cn.wjw;

import org.jdom2.Content;
import org.jdom2.Element;

import java.util.List;
import java.util.stream.Collectors;

public class DataObjectImpl implements DataObject {

    private final Element element;

    public DataObjectImpl(Element element) {
        this.element = element;
    }

    @Override
    public String getName() {
        return element.getName();
    }

    @Override
    public List<DataObject> getChildren(String cname) {
        return element.getChildren(cname).stream()
                .map(DataObjectImpl::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getAttributeValue(String attname) {
        return element.getChildText(attname);
    }

    @Override
    public void setAttribute(String name, String value) {

        Element child = this.element.getChild(name);

        if(child == null) {
            child = new Element(name);
            this.element.addContent(child);
        }

        child.setText(value);
    }

    @Override
    public DataObject addContent(String str) {
        Element child = new Element(str);
        this.element.addContent(child);
        return new DataObjectImpl(child);
    }

    @Override
    public Element getElement() {
        return this.element;
    }
}
