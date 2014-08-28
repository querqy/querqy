/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rene
 *
 */
public class PositionSequence<T> extends LinkedList<List<T>> {

   /**
     * 
     */
   private static final long serialVersionUID = 1L;

   public PositionSequence() {
   }

   public void nextPosition() {
      super.add(new LinkedList<T>());
   }

   /**
    * 
    * Adds an element at the current position
    * 
    * @param element
    */
   public void addElement(T element) {
      getLast().add(element);
   }

}
