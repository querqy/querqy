/**
 * 
 */
package querqy.lucene.rewrite;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import querqy.lucene.rewrite.prms.PRMSFieldBoost;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class SearchFieldsAndBoosting {

    public enum FieldBoostModel {FIXED, PRMS, NONE}
    
    final float defaultGeneratedFieldBoostFactor;
    final Map<String, Float> queryFieldsAndBoostings;
    final Map<String, Float> generatedQueryFieldsAndBoostings;
    final FieldBoostModel fieldBoostModel;
    
    public SearchFieldsAndBoosting(final FieldBoostModel fieldBoostModel,
                                   final Map<String, Float> queryFieldsAndBoostings,
                                   final Map<String, Float> generatedQueryFieldsAndBoostings,
                                   final float defaultGeneratedFieldBoostFactor) {
        if (fieldBoostModel == null) {
            throw new IllegalArgumentException("FieldBoostModel must not be null");
        }
        this.fieldBoostModel = fieldBoostModel;
        this.queryFieldsAndBoostings = queryFieldsAndBoostings;
        this.generatedQueryFieldsAndBoostings = generatedQueryFieldsAndBoostings;
        this.defaultGeneratedFieldBoostFactor = defaultGeneratedFieldBoostFactor;
    }

    public SearchFieldsAndBoosting withFieldBoostModel(final FieldBoostModel newModel) {
        return new SearchFieldsAndBoosting(newModel, queryFieldsAndBoostings, generatedQueryFieldsAndBoostings,
                defaultGeneratedFieldBoostFactor);
    }
    
    public boolean hasSearchField(String searchField, Term term) {
        String fieldname = term.getField();
        if (fieldname != null) {
            return term.isGenerated() || (fieldname.equals(searchField) && queryFieldsAndBoostings.containsKey(fieldname));
        } else {
            return term.isGenerated() ? generatedQueryFieldsAndBoostings.containsKey(searchField) : queryFieldsAndBoostings.containsKey(searchField);
        }
    }
    
    public Set<String> getSearchFields(final Term term) {
        final String fieldname = term.getField();
        if (fieldname != null) {
            if (term.isGenerated() || queryFieldsAndBoostings.containsKey(fieldname)) {
                return Collections.singleton(fieldname);
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

                case FIXED:
                    return (term.isGenerated())
                            ? new IndependentFieldBoost(generatedQueryFieldsAndBoostings, defaultGeneratedFieldBoostFactor)
                            : new IndependentFieldBoost(queryFieldsAndBoostings, defaultGeneratedFieldBoostFactor);

                case NONE: return ConstantFieldBoost.NORM_BOOST;

                case PRMS: return new PRMSFieldBoost();

                default: throw new IllegalStateException("Unknown FieldBoostModel: " + fieldBoostModel);

            }
        }

        return null;
    }

    

}
