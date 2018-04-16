package querqy.lucene.rewrite;

import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

/**
 * Created by rene on 10/09/2016.
 */
public class StandardDocumentFrequencyAndTermContextProvider extends AbstractDocumentFrequencyAndTermContextProvider {

    @Override
    protected TermStats doCalculateTermContexts(final IndexReaderContext indexReaderContext) throws IOException {

        final int[] dfs = new int[terms.size()];
        final TermContext[] contexts = new TermContext[dfs.length];

        for (int i = 0; i < dfs.length; i++) {

            final Term term = terms.get(i);

            contexts[i] = new TermContext(indexReaderContext);

            for (final LeafReaderContext ctx : indexReaderContext.leaves()) {

                final Terms terms = ctx.reader().terms(term.field());

                if (terms != null) {
                    final TermsEnum termsEnum = terms.iterator();
                    if (termsEnum.seekExact(term.bytes())) {
                        final TermState termState = termsEnum.termState();
                        final int df = termsEnum.docFreq();
                        dfs[i] = dfs[i] + df;
                        contexts[i].register(termState, ctx.ord, df, -1);
                    }

                }
            }
        }

        return new TermStats(dfs, contexts, indexReaderContext);
    }
}
