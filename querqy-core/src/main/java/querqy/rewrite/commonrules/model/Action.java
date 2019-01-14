/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An Action represents all Instructions triggered for specific input positions.
 * It references the sequence of query terms that matched the rule. As there can
 * be more than one term in a single position it's possible that more than one
 * sequence matched the input.
 * 
 * @author rene
 *
 */
public class Action {

   final List<Properties> properties;
   final TermMatches termMatches;
   final int startPosition;
   final int endPosition; // exclusive

   public Action(List<Instructions> instructions, TermMatches termMatches, int startPosition, int endPosition) {
      this.properties = instructionsToPropertiesMap(instructions);
      this.termMatches = termMatches;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
   }

   public Action(List<Properties> properties, TermMatches termMatches, int startPosition, int endPosition, boolean onlyProperty) {
      this.properties = properties;
      this.termMatches = termMatches;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
   }


   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + endPosition;
      result = prime * result
            + ((properties == null) ? 0 : properties.hashCode());
      result = prime * result + startPosition;
      result = prime * result + ((termMatches == null) ? 0 : termMatches.hashCode());
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
      Action other = (Action) obj;
      if (endPosition != other.endPosition)
         return false;
      if (properties == null) {
         if (other.properties != null)
            return false;
      } else if (!properties.equals(other.properties))
         return false;
      if (startPosition != other.startPosition)
         return false;
      if (termMatches == null) {
         if (other.termMatches != null)
            return false;
      } else if (!termMatches.equals(other.termMatches))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Action [properties=" + properties + ", terms=" + termMatches
            + ", startPosition=" + startPosition + ", endPosition="
            + endPosition + "]";
   }

   public List<Instructions> getInstructions() {
      return properties.stream().map(property->property.getInstructions()).collect(Collectors.toList());
   }

   public List<Properties> getProperties() {
      return properties;
   }

   public TermMatches getTermMatches() {
      return termMatches;
   }

   public int getStartPosition() {
      return startPosition;
   }

   public int getEndPosition() {
      return endPosition;
   }

   private static List<Properties> instructionsToPropertiesMap(List<Instructions> instructionsList) {
      return
              instructionsList.stream().
                      map(instructions-> new Properties(instructions)).
                      collect(Collectors.toList());
   }

}
