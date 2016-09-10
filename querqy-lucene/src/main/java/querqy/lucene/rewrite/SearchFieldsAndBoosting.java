/**
 * 
 */
package querqy.lucene.rewrite;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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



}
