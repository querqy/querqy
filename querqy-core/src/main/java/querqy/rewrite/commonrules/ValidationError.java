/**
 * 
 */
package querqy.rewrite.commonrules;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class ValidationError {

   final String message;

   public ValidationError(String message) {
      this.message = message;
   }

   public String getMessage() {
      return message;
   }

}
