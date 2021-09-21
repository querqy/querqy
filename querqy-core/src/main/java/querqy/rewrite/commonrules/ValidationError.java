/**
 * 
 */
package querqy.rewrite.commonrules;

/**
 * @author René Kriegler, @renekrie
 *
 */
@Deprecated
public class ValidationError {

   final String message;

   public ValidationError(String message) {
      this.message = message;
   }

   public String getMessage() {
      return message;
   }

   @Override
   public String toString() {
      return "ValidationError[message='" + message + "']";
   }

   @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
   }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidationError other = (ValidationError) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
    }
   
   
}
