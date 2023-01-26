/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.*;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DeleteInstruction implements Instruction {

    private static final InputSequenceNormalizer LOWERCASE = new InputSequenceNormalizer(LookupPreprocessorFactory
            .lowercase());
    
    protected final List<? extends Term> termsToDelete;
    protected final Set<CharSequence> charSequencesToDelete;
    protected final List<PrefixTerm> prefixesToDeleted;

    private final InstructionDescription instructionDescription;

    /**
      * @param termsToDelete The terms to delete from the query
      */
    @Deprecated // use only for test purposes
    public DeleteInstruction(final List<? extends Term> termsToDelete) {
        this(termsToDelete, InstructionDescription.empty());
    }

    public DeleteInstruction(final List<? extends Term> termsToDelete, final InstructionDescription instructionDescription) {
        this.termsToDelete = termsToDelete;
        charSequencesToDelete = new HashSet<>();
        final List<PrefixTerm> prefixes = new ArrayList<>();
        for (Term term : termsToDelete) {
            if (term instanceof PrefixTerm) {
                prefixes.add((PrefixTerm) term);
            } else {
                charSequencesToDelete.addAll(LOWERCASE.getTermCharSequences(term));
            }
        }
        prefixesToDeleted = prefixes.isEmpty() ? null : prefixes;
        this.instructionDescription = instructionDescription;
    }

   /* (non-Javadoc)
    * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
    */
   @Override
   public void apply(final TermMatches termMatches, final ExpandedQuery expandedQuery,
                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {

       for (final TermMatch termMatch : termMatches) {
           final querqy.model.Term term = termMatch.getQueryTerm();
           if (isToBeDeleted(term)) {
               term.delete();
           }
       }
   }

   public boolean isToBeDeleted(final querqy.model.Term term) {
       if (prefixesToDeleted != null) {
           for (final PrefixTerm prefixTerm: prefixesToDeleted) {
               if (prefixTerm.isPrefixOf(term)) {
                   return true;
               }
           }
       }
       return charSequencesToDelete.contains(term.toCharSequenceWithField(true));
   }

   @Override
   public Set<querqy.model.Term> getGenerableTerms() {
       return QueryRewriter.EMPTY_GENERABLE_TERMS;
   }

    @Override
    public InstructionDescription getInstructionDescription() {
        return instructionDescription;
    }

    @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
           + ((termsToDelete == null) ? 0 : termsToDelete.hashCode());
      return result;
   }

   @Override
   public boolean equals(final Object obj) {
       if (this == obj)
           return true;
       if (obj == null)
           return false;
       if (getClass() != obj.getClass())
           return false;
       final DeleteInstruction other = (DeleteInstruction) obj;
       if (termsToDelete == null) {
           if (other.termsToDelete != null)
               return false;
       } else if (!termsToDelete.equals(other.termsToDelete))
           return false;
       return true;
   }

   @Override
   public String toString() {
       return "DeleteInstruction [termsToDelete=" + termsToDelete + "]";
   }

}
