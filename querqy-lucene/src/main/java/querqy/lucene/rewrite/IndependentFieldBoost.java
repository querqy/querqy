/**
 * 
 */
package querqy.lucene.rewrite;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.ToStringUtils;

import querqy.model.Term;

/**
 * In this FieldBoost implementation, the boost factors of the fields do not depend on each other.
 * 
 * @author rene
 *
 */
public class IndependentFieldBoost implements FieldBoost {
    
    final Map<String, Float> queryFieldsAndBoostings;
    final float defaultGeneratedFieldBoostFactor;
    final Set<String> generatedFields;
    
    public IndependentFieldBoost(Map<String, Float> queryFieldsAndBoostings, float defaultGeneratedFieldBoostFactor) {
        this.queryFieldsAndBoostings = queryFieldsAndBoostings;
        this.defaultGeneratedFieldBoostFactor = defaultGeneratedFieldBoostFactor;
        generatedFields = new HashSet<>();
    }
    
    @Override
    public float getBoost(String fieldname, IndexSearcher searcher) {
        return getBoost(fieldname);
    }
    
    public float getBoost(String fieldname) {
        
        Float boost = queryFieldsAndBoostings.get(fieldname);
        
        return boost != null 
                ? boost : (generatedFields.contains(fieldname) 
                            ? defaultGeneratedFieldBoostFactor 
                            : 1f
                          ) ;
    }

    

    @Override
    public void registerTermSubQuery(String fieldname,
            TermSubQueryFactory termSubQueryFactory, Term sourceTerm) {
        if (sourceTerm.isGenerated()) {
            generatedFields.add(fieldname);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = 31
                + Float.floatToIntBits(defaultGeneratedFieldBoostFactor);
        result = prime
                * result
                + ((queryFieldsAndBoostings == null) ? 0
                        : queryFieldsAndBoostings.hashCode());
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
        IndependentFieldBoost other = (IndependentFieldBoost) obj;
        if (Float.floatToIntBits(defaultGeneratedFieldBoostFactor) != Float
                .floatToIntBits(other.defaultGeneratedFieldBoostFactor))
            return false;
        if (queryFieldsAndBoostings == null) {
            if (other.queryFieldsAndBoostings != null)
                return false;
        } else if (!queryFieldsAndBoostings
                .equals(other.queryFieldsAndBoostings))
            return false;
        return true;
    }

    @Override
    public String toString(String fieldname) {
        return ToStringUtils.boost(getBoost(fieldname));
    }

   



   
    
}
