/**
 *
 */
package querqy.rewrite.commonrules.model;

import java.util.Objects;

/**
 * An Action represents all Instructions triggered for specific input positions.
 * It references the sequence of query terms that matched the rule. If there is
 * more than one term in a single position it's possible that more than one
 * sequence matched the input. In that case a separate Action could be created
 * for the same position.
 *
 * @author rene
 *
 */
public class Action {

   final Instructions instructions;
   final TermMatches termMatches;

   @Deprecated
   public Action(final Instructions instructions, final TermMatches termMatches, final int startPosition,
                 final int endPosition) {
      this(instructions, termMatches);
   }

   public Action(final Instructions instructions, final TermMatches termMatches) {
      this.instructions = Objects.requireNonNull(instructions, "instructions must not be null");
      this.termMatches = termMatches;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Action action = (Action) o;
      return Objects.equals(instructions, action.instructions) && Objects.equals(termMatches, action.termMatches);
   }

   @Override
   public int hashCode() {
      return Objects.hash(instructions, termMatches);
   }

   @Override
   public String toString() {
      return "Action{" +
              "instructions=" + instructions +
              ", termMatches=" + termMatches +
              '}';
   }

   public Instructions getInstructions() {
      return instructions;
   }

   public TermMatches getTermMatches() {
      return termMatches;
   }

}
