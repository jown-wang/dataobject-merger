package cn.wjw;

import org.jdom2.Content;
import org.jdom2.Element;

import java.util.List;

public interface DataObject {

    String getName();

    List<DataObject> getChildren(final String cname);

    String getAttributeValue(final String attname);

    void setAttribute(final String name, final String value);

    DataObject addContent(final String str);

    Element getElement();
}
