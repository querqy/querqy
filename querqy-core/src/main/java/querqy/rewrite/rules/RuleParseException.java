package querqy.rewrite.rules;

public class RuleParseException extends RuntimeException {

    public RuleParseException(final String message) {
        super(message);
    }

    public RuleParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
