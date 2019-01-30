package querqy.rewrite.commonrules;

import querqy.rewrite.commonrules.model.Instructions;

import java.util.Map;

public class Properties {
    private Instructions instructions;
    private Map<String, String> propertyMap;

    public Properties() {
    }

    public Properties(Instructions instructions) {
        this.instructions = instructions;
    }

    public Properties(Instructions instructions, Map<String, String> propertyMap) {
        this.instructions = instructions;
        this.propertyMap = propertyMap;
    }

    public Instructions getInstructions() {
        return instructions;
    }

    public void setInstructions(Instructions instructions) {
        this.instructions = instructions;
    }

    public Map<String, String> getPropertyMap() {
        return propertyMap;
    }

    public void setPropertyMap(Map<String, String> propertyMap) {
        this.propertyMap = propertyMap;
    }
}
