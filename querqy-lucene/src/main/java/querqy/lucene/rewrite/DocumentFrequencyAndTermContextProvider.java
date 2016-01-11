package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import querqy.lucene.rewrite.DocumentFrequencyCorrection.DocumentFrequencyAndTermContext;

public interface DocumentFrequencyAndTermContextProvider {

    void prepareTerm(Term term);
    int termIndex();

    DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(
            int tqIndex, IndexSearcher searcher) throws IOException;

}