/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DocumentFrequencyCorrection extends AbstractDocumentFrequencyAndTermContextProvider {

    protected TermStats doCalculateTermContexts(IndexSearcher searcher) throws IOException {
        IndexReaderContext topReaderContext = searcher.getTopReaderContext();

        int[] dfs = new int[termQueries.size()];
        TermContext[] contexts = new TermContext[dfs.length];

        for (int i = 0; i < dfs.length; i++) {

            Term term = termQueries.get(i).getTerm();

            contexts[i] = new TermContext(topReaderContext);

            for (final LeafReaderContext ctx : topReaderContext.leaves()) {
                final Fields fields = ctx.reader().fields();
                if (fields != null) {
                    final Terms terms = fields.terms(term.field());
                    if (terms != null) {
                        final TermsEnum termsEnum = terms.iterator(null);
                        if (termsEnum.seekExact(term.bytes())) {
                            final TermState termState = termsEnum.termState();
                            int df = termsEnum.docFreq();
                            dfs[i] = dfs[i] + df;
                            contexts[i].register(termState, ctx.ord, df, -1);
                        }
                    }
                }
            }
        }

        for (int i = 0, last = clauseOffsets.size() - 1; i <= last; i++) {
            int start = clauseOffsets.get(i);
            int end = (i == last) ? termQueries.size() : clauseOffsets.get(i + 1);
            int pos = start;
            if (pos < end) {
                int max = dfs[pos++];
                while (pos < end) {
                    max = Math.max(max, dfs[pos++]);
                }
                if (start < endUserQuery) {
                    if (max > maxInUserQuery) {
                        maxInUserQuery = max;
                    }
                } else {
                    max += (maxInUserQuery - 1);
                }
                pos = start;

                while (pos < end) {
                    if (dfs[pos] > 0) {
                        contexts[pos].setDocFreq(max);
                    }
                    pos++;
                }
            }
        }

        return new TermStats(dfs, contexts, topReaderContext);


    }



}
