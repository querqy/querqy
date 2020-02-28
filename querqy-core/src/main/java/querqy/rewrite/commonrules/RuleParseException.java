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

   public RuleParseException(final String message) {
       super(message);
   }

   /**
    * @param lineNumber The line number at which the parsing error occurred.
    * @param message The error message.
    */
   public RuleParseException(final int lineNumber, final String message) {
      super("Line " + lineNumber + ": " + message);
   }

   /**
    * @param cause The root cause
    */
   public RuleParseException(final Throwable cause) {
      super(cause);
   }

   /**
    * @param message The error message
    * @param cause The root cause
    */
   public RuleParseException(final String message, final Throwable cause) {
      super(message, cause);
   }

   public RuleParseException(final String message, final Throwable cause, final boolean enableSuppression,
                             final boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
