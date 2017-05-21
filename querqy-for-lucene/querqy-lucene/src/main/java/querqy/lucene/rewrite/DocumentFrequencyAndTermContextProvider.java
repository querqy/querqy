package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;

public interface DocumentFrequencyAndTermContextProvider {

    void prepareTerm(Term term);
    int termIndex();

    DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(final int tqIndex,
                                                                       final IndexReaderContext indexReaderContext)
            throws IOException;

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