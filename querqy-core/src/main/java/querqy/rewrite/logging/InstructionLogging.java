package querqy.rewrite.logging;

public class InstructionLogging {

    private final String type;
    private final String param;
    private final String value;

    private InstructionLogging(final String type, final String param, final String value) {
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

    public static InstructionLoggingBuilder builder() {
        return new InstructionLoggingBuilder();
    }

    public static class InstructionLoggingBuilder {

        private String type;
        private String param;
        private String value;

        public InstructionLoggingBuilder type(final String type) {
            this.type = type;
            return this;
        }

        public InstructionLoggingBuilder param(final String param) {
            this.param = param;
            return this;
        }

        public InstructionLoggingBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public InstructionLogging build() {
            return new InstructionLogging(type, param, value);
        }
    }
}
