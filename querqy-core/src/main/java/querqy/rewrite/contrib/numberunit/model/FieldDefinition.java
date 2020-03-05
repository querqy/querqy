package querqy.rewrite.contrib.numberunit.model;

public class FieldDefinition {

    public final String fieldName;
    public final int scale;

    public FieldDefinition(String fieldName, int scale) {
        this.fieldName = fieldName;
        this.scale = scale;
    }
}
