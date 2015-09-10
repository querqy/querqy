/**
 * 
 */
package querqy.lucene.rewrite.prms;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;

/**
 * @author rene
 *
 */
public class PRMSDisjunctionMaxQuery implements PRMSQuery {
    
    final List<PRMSQuery> disjuncts;
    
    private Double probability = null;
    
    public PRMSDisjunctionMaxQuery(List<PRMSQuery> disjuncts) {
        if (disjuncts.isEmpty()) {
            throw new IllegalArgumentException("disjuncts.size() > 0 expected");
        }
        this.disjuncts = disjuncts;
    }

    /* (non-Javadoc)
     * @see querqy.lucene.rewrite.prms.PRMSQuery#calculateProbability(org.apache.lucene.index.IndexReader)
     */
    @Override
    public double calculateLikelihood(IndexReader indexReader)
            throws IOException {
        
        if (probability == null) {
            
            double max = 0.0;
            for (PRMSQuery clause: disjuncts) {
                max = Math.max(max, clause.calculateLikelihood(indexReader));
            }
            
            probability = max;
        
        }
        
        return probability;
    }

    public List<PRMSQuery> getDisjuncts() {
        return disjuncts;
    }
    

}
