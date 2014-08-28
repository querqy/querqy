/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

/**
 * @author rene
 *
 */
public class RulesDeclaration {

   final Input input;
   final List<Instructions> instructions;

   public RulesDeclaration(Input input, List<Instructions> instructions) {
      if (input == null) {
         throw new IllegalArgumentException("Input required");
      }
      if (instructions == null || instructions.isEmpty()) {
         throw new IllegalArgumentException("Instructions required");
      }
      this.input = input;
      this.instructions = instructions;
   }

   public void addInstructions(Instructions instructions) {
      this.instructions.add(instructions);
   }

}
