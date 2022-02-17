/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class NeverMatchQueryFactory implements LuceneQueryFactory<Query> {
    
    public static final NeverMatchQueryFactory FACTORY = new NeverMatchQueryFactory();

    @Override
    public void prepareDocumentFrequencyCorrection(final DocumentFrequencyCorrection dfc, final boolean isBelowDMQ) {
        // nothing to do
    }

    @Override
    public Query createQuery(final FieldBoost boostFactor, final TermQueryBuilder termQueryBuilder) {
        return new MatchNoDocsQuery();
    }

    @Override
    public <R> R accept(final LuceneQueryFactoryVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
