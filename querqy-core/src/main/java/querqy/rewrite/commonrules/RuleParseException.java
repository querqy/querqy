/**
 * 
 */
package querqy.rewrite.commonrules;

/**
 * @author rene
 *
 */
public class RuleParseException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public RuleParseException() {
    }

    /**
     * @param message
     */
    public RuleParseException(int lineNumber, String message) {
        super("Line " + lineNumber + ": " + message);
    }

    /**
     * @param cause
     */
    public RuleParseException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RuleParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public RuleParseException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
