/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import querqy.CompoundCharSequence;
import querqy.model.Term;
import eu.danieldk.dictomaton.PerfectHashDictionary;
import eu.danieldk.dictomaton.StateInfo;

/**
 * @author rene
 *
 */
public class PerfectHashDictionaryRulesCollection implements RulesCollection {

   final PerfectHashDictionary dictionary;

   /**
    * An element of this array represents the list of Instructions that are
    * triggered by the same input conditions:
    * <dl>
    * <dt>Instruction</dt>
    * <dd>A single action triggered by a single rule</dd>
    * <dt>Instructions</dt>
    * <dd>All Instructions triggered by a single rule</dd>
    * <dt>List<Instructions>[]</dt>
    * <dd>An array. An array element contains Instructions from different rules
    * that are triggered by the same condition.
    * <dd>
    * </dl>
    * 
    * The input (condition) is hashed using a perfect hash algorithm. The hash
    * code is used as the index into the instructions array.
    * 
    */
   final List<Instructions>[] instructions;
   
   final boolean ignoreCase;

   public PerfectHashDictionaryRulesCollection(PerfectHashDictionary dictionary, List<Instructions>[] instructions, boolean ignoreCase) {
      this.dictionary = dictionary;
      this.instructions = instructions;
      this.ignoreCase = ignoreCase;
   }

   /* (non-Javadoc)
 * @see querqy.rewrite.commonrules.model.RulesCollection#getRewriteActions(querqy.rewrite.commonrules.model.PositionSequence)
 */
@Override
public List<Action> getRewriteActions(PositionSequence<Term> sequence) {

       List<Action> result = new ArrayList<>();
       if (sequence.isEmpty()) {
           return result;
       }

       // We have a list of terms (resulting from DisMax alternatives) per
       // position. We now find all the combinations of terms in different 
       // positions and look them up as rules input in the dictionary
       // LinkedList<List<Term>> positions = sequence.getPositions();
       if (sequence.size() == 1) {
           for (Term term : sequence.getFirst()) {
               
               int num = dictionary.number(term.toCharSequenceWithField(ignoreCase));
               if (num > -1) {
                   result.add(new Action(instructions[num - 1], Arrays.asList(term), 0, 1));
               }
               
           }
       } else {

           List<Prefix> prefixes = new LinkedList<>();
           List<Prefix> newPrefixes = new LinkedList<>();

           int pos = 0;

           for (List<Term> position : sequence) {

               for (Term term : position) {

                   for (Prefix prefix : prefixes) {
                       
                       StateInfo stateInfo = dictionary.getStateInfo(
                               new CompoundCharSequence(null, " ", term.toCharSequenceWithField(ignoreCase)), prefix.stateInfo);
                       
                       if (stateInfo.isInKnownState()) {
                           if (stateInfo.isInFinalState()) {
                                List<Term> terms = new LinkedList<>(prefix.terms);
                                terms.add(term);
                                int hash = stateInfo.getHash() - 1;
                                result.add(new Action(instructions[hash], terms, pos - terms.size() + 1, pos + 1));
                           }
                           newPrefixes.add(new Prefix(prefix, term, stateInfo));
                       }
                   }

                   StateInfo stateInfo = dictionary.getStateInfo(term.toCharSequenceWithField(ignoreCase));
                   if (stateInfo.isInKnownState()) {
                       if (stateInfo.isInFinalState()) {
                           int hash = stateInfo.getHash() - 1;
                           result.add(new Action(instructions[hash], Arrays.asList(term), pos, pos + 1));
                       }
                       newPrefixes.add(new Prefix(term, stateInfo));
                   }

               }

               prefixes = newPrefixes;
               newPrefixes = new LinkedList<>();

               pos++;
           }

       }

       return result;
       
   }

   public static class Prefix {
      StateInfo stateInfo;
      List<Term> terms;

      public Prefix(Prefix prefix, Term term, StateInfo stateInfo) {
         terms = new LinkedList<>(prefix.terms);
         addTerm(term);
         this.stateInfo = stateInfo;
      }

      public Prefix(Term term, StateInfo stateInfo) {
         terms = new LinkedList<>();
         terms.add(term);
         this.stateInfo = stateInfo;
      }

      private void addTerm(Term term) {
         terms.add(term);
      }

   }

}
