/**
 * 
 */
package querqy.rewrite.commonrules.model;

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
    private final String matchExpression;

    /**
     * Same as {@link #Input(List, boolean, boolean, String)} with both boundaries not required (set to false)
     * @param inputTerms The sequence of terms to match
     * @param matchExpression A string that represents the input match condition (i.e. the left-hand side in rules.txt)
     */
    public Input(final List<Term> inputTerms, final String matchExpression) {
        this(inputTerms, false, false, matchExpression);
    }

    /**
     *
     * @param inputTerms The sequence of terms to match
     * @param requiresLeftBoundary true iff the first input term must be the first term in the query
     * @param requiresRightBoundary true iff the last input term must be the last term in the query
     * @param matchExpression A string that represents the input match condition (i.e. the left hand-side in rules.txt)
     */
    public Input(final List<Term> inputTerms, final boolean requiresLeftBoundary, final boolean requiresRightBoundary,
                 final String matchExpression) {
        if (matchExpression == null) {
            throw new IllegalArgumentException("matchExpression must not be null");
        }
        this.inputTerms = inputTerms;
        this.requiresLeftBoundary = requiresLeftBoundary;
        this.requiresRightBoundary = requiresRightBoundary;
        this.matchExpression = matchExpression;
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
      collectTails(new LinkedList<>(), slots, seqs);
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

   public boolean requiresLeftBoundary() {
       return requiresLeftBoundary;
   }

   public boolean requiresRightBoundary() {
       return requiresRightBoundary;
   }

   public String getMatchExpression() {
      return matchExpression;
   }
}
