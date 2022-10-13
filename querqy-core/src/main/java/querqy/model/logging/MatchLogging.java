package querqy.model.logging;

public class MatchLogging {

    private final String term;
    private final MatchType type;

    public MatchLogging(final String term, final MatchType type) {
        this.term = term;
        this.type = type;
    }

    public String getTerm() {
        return term;
    }

    public MatchType getType() {
        return type;
    }

    public enum MatchType {
        EXACT("exact"), PREFIX("prefix");

        private final String typeName;

        MatchType(final String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

}
