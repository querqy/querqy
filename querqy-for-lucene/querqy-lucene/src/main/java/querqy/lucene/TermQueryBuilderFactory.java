package querqy.lucene;

import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.TermQueryBuilder;

public interface TermQueryBuilderFactory {

    TermQueryBuilder createTermQueryBuilder(DocumentFrequencyCorrection dfc);

}
