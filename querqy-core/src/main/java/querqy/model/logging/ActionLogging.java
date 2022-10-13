package querqy.model.logging;

public class ActionLogging {

    private final String message;
    private final MatchLogging match;
    private final RuleLogging rule;

    public ActionLogging(final String message, final MatchLogging match, final RuleLogging rule) {
        this.message = message;
        this.match = match;
        this.rule = rule;
    }

    public String getMessage() {
        return message;
    }

    public MatchLogging getMatch() {
        return match;
    }

    public RuleLogging getRule() {
        return rule;
    }
}
