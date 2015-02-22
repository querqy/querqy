/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.LinkedList;
import java.util.List;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;

/**
 * @author rene
 *
 */
public class Input {

   final List<Term> inputTerms;
   final boolean requiresLeftBoundary;
   final boolean requiresRightBoundary;

   public Input(List<Term> inputTerms) {
       this(inputTerms, false, false);
   }
   
   public Input(List<Term> inputTerms, boolean requiresLeftBoundary, boolean requiresRightBoundary) {
      this.inputTerms = inputTerms;
      this.requiresLeftBoundary = requiresLeftBoundary;
      this.requiresRightBoundary = requiresRightBoundary;
   }

   public boolean isEmpty() {
      return inputTerms == null || inputTerms.isEmpty();
   }

   public List<ComparableCharSequence> getInputSequences(boolean lowerCaseValues) {

      if (inputTerms.size() == 1) {
         return inputTerms.get(0).getCharSequences(lowerCaseValues);
      }

      LinkedList<List<ComparableCharSequence>> slots = new LinkedList<>();

      for (Term inputTerm : inputTerms) {
         slots.add(inputTerm.getCharSequences(lowerCaseValues));
      }

      List<ComparableCharSequence> seqs = new LinkedList<>();
      collectTails(new LinkedList<ComparableCharSequence>(), slots, seqs);
      return seqs;

   }

   void collectTails(List<ComparableCharSequence> prefix, List<List<ComparableCharSequence>> tailSlots,
         List<ComparableCharSequence> result) {
      if (tailSlots.size() == 1) {
         for (ComparableCharSequence sequence : tailSlots.get(0)) {
            List<ComparableCharSequence> combined = new LinkedList<>(prefix);
            combined.add(sequence);
            result.add(new CompoundCharSequence(" ", combined));
         }
      } else {

         List<List<ComparableCharSequence>> newTail = tailSlots.subList(1, tailSlots.size());
         for (ComparableCharSequence sequence : tailSlots.get(0)) {
            List<ComparableCharSequence> newPrefix = new LinkedList<>(prefix);
            newPrefix.add(sequence);
            collectTails(newPrefix, newTail, result);
         }
      }
   }

   public List<Term> getInputTerms() {
      return inputTerms;
   }

   @Override
   public String toString() {
      return "Input [inputTerms=" + inputTerms + "]";
   }

}
