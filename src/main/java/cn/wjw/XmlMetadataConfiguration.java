package cn.wjw;

import java.util.ArrayList;
import java.util.List;

public class XmlMetadataConfiguration {

    List<Row> rows = new ArrayList<>();

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public static class Row {

        private String productObjectPath;

        private int min;

        private int max;

        private String fields;

        public String getProductObjectPath() {
            return productObjectPath;
        }

        public void setProductObjectPath(String productObjectPath) {
            this.productObjectPath = productObjectPath;
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

        public String getFields() {
            return fields;
        }

        public void setFields(String fields) {
            this.fields = fields;
        }

    }
}
