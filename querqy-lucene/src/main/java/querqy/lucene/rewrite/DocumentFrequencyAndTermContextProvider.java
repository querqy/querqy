package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;

public interface DocumentFrequencyAndTermContextProvider {

    int registerTermQuery(DependentTermQuery tq);

    DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(
            int tqIndex, IndexSearcher searcher) throws IOException;

    void newClause();

    class DocumentFrequencyAndTermContext {

        public final int df;
        public final TermContext termContext;

        public DocumentFrequencyAndTermContext(int df, TermContext termContext) {
            this.df = df;
            this.termContext = termContext;
        }


    }
}