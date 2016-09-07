/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import querqy.lucene.rewrite.prms.PRMSFieldBoost;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class SearchFieldsAndBoosting {
    
    public enum FieldBoostModel {FIXED, PRMS}
    
    final float defaultGeneratedFieldBoostFactor;
    final Map<String, Float> queryFieldsAndBoostings;
    final Map<String, Float> generatedQueryFieldsAndBoostings;
    final FieldBoostModel fieldBoostModel;
    
    public SearchFieldsAndBoosting(FieldBoostModel fieldBoostModel, Map<String, Float> queryFieldsAndBoostings, Map<String, Float> generatedQueryFieldsAndBoostings, float defaultGeneratedFieldBoostFactor) {
        if (fieldBoostModel == null) {
            throw new IllegalArgumentException("FieldBoostModel must not be null");
        }
        this.fieldBoostModel = fieldBoostModel;
        this.queryFieldsAndBoostings = queryFieldsAndBoostings;
        this.generatedQueryFieldsAndBoostings = generatedQueryFieldsAndBoostings;
        this.defaultGeneratedFieldBoostFactor = defaultGeneratedFieldBoostFactor;
    }
    
    public boolean hasSearchField(String searchField, Term term) {
        String fieldname = term.getField();
        if (fieldname != null) {
            return term.isGenerated() || (fieldname.equals(searchField) && queryFieldsAndBoostings.containsKey(fieldname));
        } else {
            return term.isGenerated() ? generatedQueryFieldsAndBoostings.containsKey(searchField) : queryFieldsAndBoostings.containsKey(searchField);
        }
    }
    
    public Set<String> getSearchFields(Term term) {
        String fieldname = term.getField();
        if (fieldname != null) {
            if (term.isGenerated() || queryFieldsAndBoostings.containsKey(fieldname)) {
                return new HashSet<String>(Arrays.asList(fieldname));
            } else {
                return Collections.emptySet();
            }
        } else {
            return term.isGenerated() ? generatedQueryFieldsAndBoostings.keySet() : queryFieldsAndBoostings.keySet();
        }
    }
    
    public FieldBoost getFieldBoost(Term term) {
        String fieldname = term.getField();
        if (fieldname != null) {
            if (term.isGenerated() ) {
                return new IndependentFieldBoost(generatedQueryFieldsAndBoostings, defaultGeneratedFieldBoostFactor);
            }
            if (queryFieldsAndBoostings.containsKey(fieldname)) {
                return new IndependentFieldBoost(queryFieldsAndBoostings, defaultGeneratedFieldBoostFactor);
            }
        } else {
            switch (fieldBoostModel) {
            case PRMS: return new PRMSFieldBoost();
            case FIXED:
                return (term.isGenerated())
                     ? new IndependentFieldBoost(generatedQueryFieldsAndBoostings, defaultGeneratedFieldBoostFactor)
                     : new IndependentFieldBoost(queryFieldsAndBoostings, defaultGeneratedFieldBoostFactor);
            }
        }
        
        return null;
    }


    public SearchFieldsAndBoosting multiply(final float factor) {
        return new SearchFieldsAndMultipliedBoostings(this, factor);
    }

    class SearchFieldsAndMultipliedBoostings extends SearchFieldsAndBoosting {

        final float factor;

        public SearchFieldsAndMultipliedBoostings(SearchFieldsAndBoosting original, float factor) {
            super(original.fieldBoostModel, original.queryFieldsAndBoostings,
                    original.generatedQueryFieldsAndBoostings, original.defaultGeneratedFieldBoostFactor);
            this.factor = factor;

        }

        @Override
        public FieldBoost getFieldBoost(Term term) {
            return new MultipliedFieldBoost(super.getFieldBoost(term), factor);
        }
    }

     class MultipliedFieldBoost implements FieldBoost {

        final FieldBoost delegate;
        final float factor;

        public MultipliedFieldBoost(final FieldBoost delegate, final float factor) {
            this.delegate = delegate;
            this.factor = factor;
        }

        @Override
        public float getBoost(String fieldname, IndexSearcher searcher) throws IOException {
            return factor * delegate.getBoost(fieldname, searcher);
        }

        @Override
        public void registerTermSubQuery(String fieldname, TermSubQueryFactory termSubQueryFactory, Term sourceTerm) {
            delegate.registerTermSubQuery(fieldname, termSubQueryFactory, sourceTerm);
        }

        @Override
        public String toString(String fieldname) {
            return delegate.toString(fieldname) + "*" + factor;
        }

         @Override
         public boolean equals(Object o) {
             if (this == o) return true;
             if (o == null || getClass() != o.getClass()) return false;

             MultipliedFieldBoost that = (MultipliedFieldBoost) o;

             if (Float.compare(that.factor, factor) != 0) return false;
             return delegate.equals(that.delegate);

         }

         @Override
         public int hashCode() {
             int result = delegate.hashCode();
             result = 31 * result + (factor != +0.0f ? Float.floatToIntBits(factor) : 0);
             return result;
         }
     }
}
