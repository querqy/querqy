/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.search.Query;
import querqy.model.NodeVisitor;

/**
 * @author rene
 *
 */
public interface LuceneQueryFactory<T extends Query> {

    void prepareDocumentFrequencyCorrection(DocumentFrequencyCorrection dfc, boolean isBelowDMQ);

    T createQuery(FieldBoost boost, TermQueryBuilder termQueryBuilder);

    <R> R accept(LuceneQueryFactoryVisitor<R> visitor);


}
