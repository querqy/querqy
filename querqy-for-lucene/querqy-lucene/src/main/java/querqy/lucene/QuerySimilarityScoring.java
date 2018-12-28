package querqy.lucene;

import querqy.lucene.rewrite.DependentTermQueryBuilder;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.FieldBoostTermQueryBuilder;
import querqy.lucene.rewrite.SimilarityTermQueryBuilder;
import querqy.lucene.rewrite.TermQueryBuilder;

public enum QuerySimilarityScoring {

    DFC(dfc -> dfc == null
            ? new DependentTermQueryBuilder(new DocumentFrequencyCorrection())
            : new DependentTermQueryBuilder(dfc)),

    SIMILARITY_SCORE_OFF(dfc ->  new FieldBoostTermQueryBuilder()),

    SIMILARITY_SCORE_ON(dfc -> new SimilarityTermQueryBuilder());

    private TermQueryBuilderFactory termQueryBuilderFactory;

    QuerySimilarityScoring(final TermQueryBuilderFactory termQueryBuilderFactory) {
        this.termQueryBuilderFactory = termQueryBuilderFactory;
    }

    TermQueryBuilder createTermQueryBuilder(final DocumentFrequencyCorrection dfc) {
        return termQueryBuilderFactory.createTermQueryBuilder(dfc);
    }
}
