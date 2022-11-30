package querqy.rewrite.logging;

public class InstructionLog {

    private final String type;
    private final String param;
    private final String value;

    private InstructionLog(final String type, final String param, final String value) {
        this.type = type;
        this.param = param;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Object getParam() {
        return param;
    }

    public String getValue() {
        return value;
    }

    public static InstructionLogBuilder builder() {
        return new InstructionLogBuilder();
    }

    public static class InstructionLogBuilder {

        private String type;
        private String param;
        private String value;

        public InstructionLogBuilder type(final String type) {
            this.type = type;
            return this;
        }

        public InstructionLogBuilder param(final String param) {
            this.param = param;
            return this;
        }

        public InstructionLogBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public InstructionLog build() {
            return new InstructionLog(type, param, value);
        }
    }
}
