/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class FilterInstruction implements Instruction {

   final QuerqyQuery<?> filterQuery;

   public FilterInstruction(QuerqyQuery<?> filterQuery) {
      if (filterQuery == null) {
         throw new IllegalArgumentException("filterQuery must not be null");
      }
      this.filterQuery = filterQuery;
   }

   /* (non-Javadoc)
    * @see querqy.rewrite.commonrules.model.Instruction#apply(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.model.TermMatches, int, int, querqy.model.ExpandedQuery, java.util.Map)
    */
   @Override
   public void apply(PositionSequence<Term> sequence, TermMatches termMatches,
           int startPosition, int endPosition, ExpandedQuery expandedQuery,  Map<String, Object> context) {
      expandedQuery.addFilterQuery((QuerqyQuery<?>) filterQuery.clone(null, true));

   }
   
    @Override
    public Set<Term> getGenerableTerms() {
        return (filterQuery instanceof Query) 
            ?  TermsCollector.collect((Query) filterQuery)
            : QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((filterQuery == null) ? 0 : filterQuery.hashCode());
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
        FilterInstruction other = (FilterInstruction) obj;
        if (filterQuery == null) {
            if (other.filterQuery != null)
                return false;
        } else if (!filterQuery.equals(other.filterQuery))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FilterInstruction [filterQuery=" + filterQuery + "]";
    }

}
