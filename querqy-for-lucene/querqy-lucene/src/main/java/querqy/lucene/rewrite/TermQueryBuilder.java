package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import java.util.Optional;

public interface TermQueryBuilder {

    Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection();
    TermQuery createTermQuery(Term term, FieldBoost boost);

}
