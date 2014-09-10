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
 * @author René Kriegler, @renekrie
 *
 */
public class RulesCollectionBuilder {

   TreeMap<ComparableCharSequence, List<Instructions>> rules = new TreeMap<>();
   
   final boolean ignoreCase;
   
   public RulesCollectionBuilder(boolean ignoreCase) {
       this.ignoreCase = ignoreCase;
   }

   public void addRule(Input input, Instructions instructions) {
      
       for (ComparableCharSequence seq : input.getInputSequences(ignoreCase)) {
          
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
               rules.values().toArray(new List[rules.size()]),
               ignoreCase);

      } catch (DictionaryBuilderException e) {
         throw new RuntimeException(e);
      }
   }

}
