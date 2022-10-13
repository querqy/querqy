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
        EXACT("exact");

        private final String name;

        MatchType(final String name) {
            this.name = name;
        }
    }

}
