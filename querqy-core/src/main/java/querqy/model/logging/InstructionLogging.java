package querqy.model.logging;

public class InstructionLogging {

    private final String type;
    private final String param;
    private final String value;

    public InstructionLogging(final String type, final String param, final String value) {
        this.type = type;
        this.param = param;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getParam() {
        return param;
    }

    public String getValue() {
        return value;
    }
}
