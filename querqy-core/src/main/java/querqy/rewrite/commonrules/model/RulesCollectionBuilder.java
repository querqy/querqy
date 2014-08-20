/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import querqy.ComparableCharSequence;
import eu.danieldk.dictomaton.DictionaryBuilder;
import eu.danieldk.dictomaton.DictionaryBuilderException;

/**
 * @author rene
 *
 */
public class RulesCollectionBuilder {

   TreeMap<ComparableCharSequence, List<Instructions>> rules = new TreeMap<>();

   public void addRule(Input input, Instructions instructions) {
      for (ComparableCharSequence seq : input.getInputSequences()) {
         List<Instructions> instructionsList = rules.get(seq);
         if (instructionsList == null) {
            instructionsList = new LinkedList<>();
            rules.put(seq, instructionsList);
         }
         instructionsList.add(instructions);
      }
   }

   @SuppressWarnings("unchecked")
   public RulesCollection build() {
      DictionaryBuilder builder = new DictionaryBuilder();
      try {
         return new RulesCollection(
               builder.addAll(rules.keySet()).buildPerfectHash(),
               rules.values().toArray(new List[rules.size()]));

      } catch (DictionaryBuilderException e) {
         throw new RuntimeException(e);
      }
   }

}
