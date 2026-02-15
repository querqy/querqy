package querqy.rewrite.logging;

public class MatchLog {

    private final String term;
    private final String type;

    private MatchLog(final String term, final String type) {
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
        EXACT("exact"), AFFIX("affix"), REGEX("regex");

        private final String typeName;

        MatchType(final String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

    public static MatchLogBuilder builder() {
        return new MatchLogBuilder();
    }

    public static class MatchLogBuilder {

        private String term;
        private MatchType type;

        public MatchLogBuilder term(final String term) {
            this.term = term;
            return this;
        }

        public MatchLogBuilder type(final MatchType type) {
            this.type = type;
            return this;
        }

        public MatchLog build() {
            return new MatchLog(term, type.getTypeName());
        }
    }

}
