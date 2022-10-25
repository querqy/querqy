package querqy.model.logging;

public class MatchLogging {

    private final String term;
    private final String type;

    private MatchLogging(final String term, final String type) {
        this.term = term;
        this.type = type;
    }

    public String getTerm() {
        return term;
    }

    public String getType() {
        return type;
    }

    public enum MatchType {
        EXACT("exact"), AFFIX("affix");

        private final String typeName;

        MatchType(final String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

    public static MatchLoggingBuilder builder() {
        return new MatchLoggingBuilder();
    }

    public static class MatchLoggingBuilder {

        private String term;
        private MatchType type;

        public MatchLoggingBuilder term(final String term) {
            this.term = term;
            return this;
        }

        public MatchLoggingBuilder type(final MatchType type) {
            this.type = type;
            return this;
        }

        public MatchLogging build() {
            return new MatchLogging(term, type.getTypeName());
        }
    }

}
