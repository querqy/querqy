package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.FieldBoost;
import querqy.lucene.rewrite.TermQueryBuilder;

import java.util.Optional;

public class LuceneTermQueryBuilder implements TermQueryBuilder {
    @Override
    public Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection() {
        return Optional.empty();
    }

    @Override
    public TermQuery createTermQuery(final Term term, final FieldBoost boost) {
        return new TermQuery(term);
    }
}
