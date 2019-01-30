/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.*;

import querqy.ComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
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
      * 
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
      // make sure that at least one term will be left in the query after we
      // apply this instruction

      int pos = 0;

      boolean hasRemaining = false;

      List<querqy.model.Term> toBeDeleted = new LinkedList<>();

      for (List<querqy.model.Term> position : sequence) {
          
         for (querqy.model.Term term : position) {

            if (pos >= startPosition && pos < endPosition && isToBeDeleted(term)) {
               // TODO: check whether it would be faster to use a LinkedHashMap
               // for toBeDeleted and then check whether .add(term) returns true
               if (hasRemaining) {
                  toBeDeleted.add(term);
               } else {
                  if (toBeDeleted.contains(term)) { // same term twice - we keep
                                                    // a copy
                     hasRemaining = true;
                  } else {
                     toBeDeleted.add(term);
                  }
               }
            } else {
               hasRemaining = true;
               // TODO: optimise: we can go to the next position in the sequence
               // if
               // we got here via pos < startPosition or pos >= endPosition (no
               // need to check further terms at
               // this position)
            }
         }
         pos++;
      }

      if (hasRemaining) {

         for (querqy.model.Term term : toBeDeleted) {
            // remove the term from its parent. If the parent doesn't have any
            // further child,
            // remove the parent from the grand-parent. If this also hasn't any
            // further child,
            // do not remove anything
            DisjunctionMaxQuery parentQuery = term.getParent();
            BooleanQuery grandParent = null;

            if (parentQuery.getClauses().size() < 2) {
               grandParent = parentQuery.getParent();
               if (grandParent.getClauses().size() < 2) {
                  continue;
               }
            }

            parentQuery.removeClause(term);
            if (grandParent != null) {
               grandParent.removeClause(parentQuery);
            }

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
