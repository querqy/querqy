/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.*;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DeleteInstruction implements Instruction {
    
    protected final List<? extends Term> termsToDelete;
    protected final Set<CharSequence> charSequencesToDelete;
    protected final List<PrefixTerm> prefixesToDeleted;

    /**
      * @param termsToDelete The terms to delete from the query
      */
    public DeleteInstruction(final List<? extends Term> termsToDelete) {
        this.termsToDelete = termsToDelete;
        charSequencesToDelete = new HashSet<>();
        final List<PrefixTerm> prefixes = new ArrayList<>();
        for (Term term : termsToDelete) {
            if (term instanceof PrefixTerm) {
                prefixes.add((PrefixTerm) term);
            } else {
                charSequencesToDelete.addAll(term.getCharSequences(true));
            }
        }
        prefixesToDeleted = prefixes.isEmpty() ? null : prefixes;
    }

    public List<? extends Term> getTermsToDelete() {
        return termsToDelete;
    }


   /* (non-Javadoc)
    * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
    */
   @Override
   public void apply(PositionSequence<querqy.model.Term> sequence, TermMatches termMatches,
                     int startPosition, int endPosition, ExpandedQuery expandedQuery, SearchEngineRequestAdapter searchEngineRequestAdapter) {

      int pos = 0;

      for (List<querqy.model.Term> position : sequence) {
          
         for (querqy.model.Term term : position) {
             if (pos >= startPosition && pos < endPosition && isToBeDeleted(term)) {
                 term.delete();
             }
         }
         pos++;
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
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
           + ((termsToDelete == null) ? 0 : termsToDelete.hashCode());
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
       DeleteInstruction other = (DeleteInstruction) obj;
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
