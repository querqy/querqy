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
    * @param message
    */
   public RuleParseException(final int lineNumber, final String message) {
      super("Line " + lineNumber + ": " + message);
   }

   /**
    * @param cause
    */
   public RuleParseException(final Throwable cause) {
      super(cause);
   }

   /**
    * @param message
    * @param cause
    */
   public RuleParseException(final String message, final Throwable cause) {
      super(message, cause);
   }

   /**
    * @param message
    * @param cause
    * @param enableSuppression
    * @param writableStackTrace
    */
   public RuleParseException(final String message, final Throwable cause, final boolean enableSuppression,
                             final boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
