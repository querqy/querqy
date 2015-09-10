package querqy.lucene.rewrite.prms;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * A query in the 'Probabilistic Retrieval Model for Semi-structured Data'
 *  
 * @author rene
 *
 */

public interface PRMSQuery {
    
    public static PRMSQuery NEVER_MATCH_PRMS_QUERY = new PRMSQuery() {
        @Override
        public double calculateLikelihood(IndexReader indexReader)
                throws IOException {
            return 0.0;
        }
    };
    
    /**
     * Calculate the probability of the query for a given index
     * @param indexReader
     * @return
     */
    double calculateLikelihood(IndexReader indexReader) throws IOException;

}
