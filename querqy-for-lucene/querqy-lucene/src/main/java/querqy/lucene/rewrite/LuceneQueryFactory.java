/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.Query;

/**
 * @author rene
 *
 */
public interface LuceneQueryFactory<T extends Query> {

    void prepareDocumentFrequencyCorrection(DocumentFrequencyAndTermContextProvider dftcp, boolean isBelowDMQ);

    T createQuery(FieldBoost boost, float dmqTieBreakerMultiplier, DocumentFrequencyAndTermContextProvider dftcp) throws IOException;

}
