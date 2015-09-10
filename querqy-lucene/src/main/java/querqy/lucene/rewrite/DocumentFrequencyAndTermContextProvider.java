package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;

import querqy.lucene.rewrite.DocumentFrequencyCorrection.DocumentFrequencyAndTermContext;

public interface DocumentFrequencyAndTermContextProvider {

    int registerTermQuery(DependentTermQuery tq);

    DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(
            int tqIndex, IndexSearcher searcher) throws IOException;

}