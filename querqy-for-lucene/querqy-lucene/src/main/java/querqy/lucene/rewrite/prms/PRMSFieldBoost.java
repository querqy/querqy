/**
 * 
 */
package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import querqy.lucene.rewrite.FieldBoost;
import querqy.lucene.rewrite.TermSubQueryFactory;
import querqy.model.Term;

/**
 * @author rene
 *
 */
public class PRMSFieldBoost implements FieldBoost {
    
    Map<String, PRMSQuery> fieldPRMSQueries = new HashMap<>();
    Map<String, Float> probabilities = null;

    /* (non-Javadoc)
     * @see querqy.lucene.rewrite.TermQueryBoost#getBoost()
     */
    @Override
    public float getBoost(String fieldname, IndexReader indexReader) throws IOException {
        if (probabilities == null) {
            calculateProbabilities(indexReader);
        }
        Float p = probabilities.get(fieldname);
        return p == null ? 0f : p;
    }
    
    protected void calculateProbabilities(IndexReader indexReader) throws IOException {
        Map<String, Float> probs = new HashMap<>();
        switch (fieldPRMSQueries.size()) {
        case 0 : break;
        case 1 : 
            {
                Map.Entry<String, PRMSQuery> entry = fieldPRMSQueries.entrySet().iterator().next();
                double l = entry.getValue().calculateLikelihood(indexReader);
                probs.put(entry.getKey(), l == 0.0 ? 0f : 1f);  
            }
            break;
        default: 
            double sum = 0.0;
            Map<String, Double> likelihoods = new HashMap<String, Double>();
            for (Map.Entry<String, PRMSQuery> entry: fieldPRMSQueries.entrySet()) {
                double l = entry.getValue().calculateLikelihood(indexReader);
                sum += l;
                likelihoods.put(entry.getKey(), l);
            }
            for (Map.Entry<String, Double> entry: likelihoods.entrySet()) {
                probs.put(entry.getKey(), (float) (entry.getValue() / sum));
            }
            probabilities = probs;
        }
    }

    @Override
    public void registerTermSubQuery(String fieldname, TermSubQueryFactory termSubQueryFactory, Term sourceTerm) {
        
        if (!termSubQueryFactory.isNeverMatchQuery()) {
            if (fieldPRMSQueries.put(fieldname, termSubQueryFactory.prmsQuery) != null) {
                throw new IllegalStateException("A PRMSQuery has already been registered for field " + fieldname);
            }
        }
        
    }
    
    @Override
    public String toString(String fieldname) {
        StringBuilder sb = new StringBuilder();
        sb.append("^PRMS(");
        if (probabilities != null) {
            Float p = probabilities.get(fieldname);
            if (p == null) {
                p = 0f;
            }
            sb.append(p);
        } else {
            int numFields = fieldPRMSQueries.size();
            switch (numFields) {
            case 0 : break;
            case 1 : sb.append(fieldPRMSQueries.keySet().iterator().next()); break;
            default: 
                int i = 0;
                for (String field: fieldPRMSQueries.keySet()) {
                    if (i++ > 0) {
                        sb.append(',');
                    }
                    sb.append(field);
                }
            }
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((fieldPRMSQueries == null) ? 0 : fieldPRMSQueries.keySet().hashCode());
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
        PRMSFieldBoost other = (PRMSFieldBoost) obj;
        if (fieldPRMSQueries == null) {
            if (other.fieldPRMSQueries != null)
                return false;
        } else if (!fieldPRMSQueries.keySet().equals(other.fieldPRMSQueries.keySet()))
            return false;
        return true;
    }


    
    

}
