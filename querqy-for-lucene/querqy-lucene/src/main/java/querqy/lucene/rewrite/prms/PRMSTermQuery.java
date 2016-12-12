/**
 * 
 */
package querqy.lucene.rewrite.prms;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

/**
 * @author rene
 *
 */
public class PRMSTermQuery implements PRMSQuery {
    
    final Term term;
    
    private Double likelihood = null;
    
    public PRMSTermQuery(Term term) {
        this.term = term;
    }
    
    /**
     * Calculates the likelihood of the term query for the given index
     */
    @Override
    public double calculateLikelihood(IndexReader indexReader) throws IOException {
        
        if (likelihood == null) {
        
            long totalTermsInField = indexReader.getSumTotalTermFreq(term.field());
            
            if (totalTermsInField == -1L) {
                throw new UnsupportedOperationException("Codec does not support IndexReader.getSumTotalTermFreq(field)");
            }
            if (totalTermsInField < 1L) {
                return 0.0;
            }
            long totalTf = indexReader.totalTermFreq(term);
            if (totalTf == -1L) {
                throw new UnsupportedOperationException("Codec does not support IndexReader.totalTermFreq(term)");
            }
            
            likelihood = ((double) totalTf) / (double) totalTermsInField;
            
        }
        
        return likelihood;
    }

    public Term getTerm() {
        return term;
    }

}
