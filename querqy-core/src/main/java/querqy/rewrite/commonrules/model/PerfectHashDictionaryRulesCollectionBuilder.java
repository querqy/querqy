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
public class PerfectHashDictionaryRulesCollectionBuilder implements RulesCollectionBuilder {

   TreeMap<ComparableCharSequence, List<Instructions>> rules = new TreeMap<>();
   
   final boolean ignoreCase;
   
   public PerfectHashDictionaryRulesCollectionBuilder(boolean ignoreCase) {
       this.ignoreCase = ignoreCase;
   }

   /* (non-Javadoc)
 * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#addRule(querqy.rewrite.commonrules.model.Input, querqy.rewrite.commonrules.model.Instructions)
 */
@Override
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

   /* (non-Javadoc)
 * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#build()
 */
@Override
@SuppressWarnings("unchecked")
   public RulesCollection build() {
      DictionaryBuilder builder = new DictionaryBuilder();
      try {
         return new PerfectHashDictionaryRulesCollection(
               builder.addAll(rules.keySet()).buildPerfectHash(),
               rules.values().toArray(new List[rules.size()]),
               ignoreCase);

      } catch (DictionaryBuilderException e) {
         throw new RuntimeException(e);
      }
   }

}
