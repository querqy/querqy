/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;

/**
 * An input object representing the left-hand side of a rewriting rule
 *
 * @author rene
 *
 */
public class Input {

    final List<Term> inputTerms;
    final boolean requiresLeftBoundary;
    final boolean requiresRightBoundary;

    /**
     * Same as {@link #Input(List, boolean, boolean)} with both boundaries not required (set to false)
     * @param inputTerms The sequence of terms to match
     */
    public Input(final List<Term> inputTerms) {
        this(inputTerms, false, false);
    }

    /**
     *
     * @param inputTerms The sequence of terms to match
     * @param requiresLeftBoundary true iff the first input term must be the first term in the query
     * @param requiresRightBoundary true iff the last input term must be the last term in the query
     */
    public Input(final List<Term> inputTerms, final boolean requiresLeftBoundary, final boolean requiresRightBoundary) {
        this.inputTerms = inputTerms == null ? Collections.emptyList() : inputTerms;
        this.requiresLeftBoundary = requiresLeftBoundary;
        this.requiresRightBoundary = requiresRightBoundary;
    }

   public boolean isEmpty() {
      return inputTerms.isEmpty();
   }

   public List<ComparableCharSequence> getInputSequences(final boolean lowerCaseValues) {

      if (inputTerms.size() == 1) {
         return inputTerms.get(0).getCharSequences(lowerCaseValues);
      }

      LinkedList<List<ComparableCharSequence>> slots = new LinkedList<>();

      for (final Term inputTerm : inputTerms) {
         slots.add(inputTerm.getCharSequences(lowerCaseValues));
      }

      final List<ComparableCharSequence> seqs = new LinkedList<>();
      collectTails(new LinkedList<>(), slots, seqs);
      return seqs;

   }

   void collectTails(final List<ComparableCharSequence> prefix, List<List<ComparableCharSequence>> tailSlots,
         final List<ComparableCharSequence> result) {
      if (tailSlots.size() == 1) {
         for (final ComparableCharSequence sequence : tailSlots.get(0)) {
            final List<ComparableCharSequence> combined = new LinkedList<>(prefix);
            combined.add(sequence);
            result.add(new CompoundCharSequence(" ", combined));
         }
      } else {

         final List<List<ComparableCharSequence>> newTail = tailSlots.subList(1, tailSlots.size());
         for (final ComparableCharSequence sequence : tailSlots.get(0)) {
            final List<ComparableCharSequence> newPrefix = new LinkedList<>(prefix);
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

   public boolean requiresLeftBoundary() {
       return requiresLeftBoundary;
   }

   public boolean requiresRightBoundary() {
       return requiresRightBoundary;
   }

}
