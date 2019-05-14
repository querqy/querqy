package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

/**
 * Created by rene on 10/09/2016.
 */
public class DocumentFrequencyCorrection {

    final List<Term> terms = new ArrayList<>(16);
    enum Status {
        USER_QUERY, OTHER_QUERY
    }

    protected final List<Integer> clauseOffsets = new ArrayList<>();
    TermStats termStats = null;
    protected int endUserQuery = -1;
    protected Status status = Status.USER_QUERY;
    protected int maxInClause = -1;
    protected int maxInUserQuery = -1;
    int termIndex = -1;


    protected TermStats doCalculateTermContexts(final IndexReaderContext indexReaderContext)
            throws IOException {

        final int[] dfs = new int[terms.size()];
        final TermStates[] states = new TermStates[dfs.length];

        for (int i = 0; i < dfs.length; i++) {

            final Term term = terms.get(i);

            states[i] = new TermStates(indexReaderContext);

            for (final LeafReaderContext ctx : indexReaderContext.leaves()) {

                final Terms terms = ctx.reader().terms(term.field());
                if (terms != null) {
                    final TermsEnum termsEnum = terms.iterator();
                    if (termsEnum.seekExact(term.bytes())) {
                        final TermState termState = termsEnum.termState();
                        final int df = termsEnum.docFreq();
                        dfs[i] = dfs[i] + df;
                        states[i].register(termState, ctx.ord, df, df);
                    }
                }

            }
        }

        for (int i = 0, last = clauseOffsets.size() - 1; i <= last; i++) {
            final int start = clauseOffsets.get(i);
            final int end = (i == last) ? terms.size() : clauseOffsets.get(i + 1);
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
                        int delta = max - dfs[pos];
                        if (delta > 0) {
                            states[pos].accumulateStatistics(delta, delta);
                        }
                    }
                    pos++;
                }
            }
        }

        return new TermStats(dfs, states, indexReaderContext);

    }


    public void prepareTerm(Term term) {
        terms.add(term);
    }

    public int termIndex() {
        if (termIndex < terms.size() - 1) {
            termIndex++;
            return termIndex;
        } else {
            throw new IllegalStateException("termIndex already at last position: " + termIndex);

        }
    }


    /* (non-Javadoc)
         * @see querqy.lucene.rewrite.DocumentFrequencyAndTermContextProvider#getDocumentFrequencyAndTermContext(int,
         * org.apache.lucene.search.IndexReaderContext)
         */
    public DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(final int tqIndex,
                                                                              final IndexReaderContext indexReaderContext)
            throws IOException {

        TermStats ts = termStats;
        if (ts == null || ts.topReaderContext != indexReaderContext) {
            ts = calculateTermContexts(indexReaderContext);
        }

        return new DocumentFrequencyAndTermContext(ts.documentFrequencies[tqIndex], ts.termStates[tqIndex]);
    }

    protected TermStats calculateTermContexts(final IndexReaderContext indexReaderContext)
            throws IOException {

        return setTermStats(doCalculateTermContexts(indexReaderContext));

    }

    private synchronized TermStats setTermStats(final TermStats ts) {
        this.termStats = ts;
        return this.termStats;
    }

    public void newClause() {
       if (status == Status.USER_QUERY) {
          maxInUserQuery = Math.max(maxInClause, maxInUserQuery);
       }
       maxInClause = -1;
       clauseOffsets.add(terms.size());
    }

    public void finishedUserQuery() {
       status = Status.OTHER_QUERY;
       maxInUserQuery = Math.max(maxInClause, maxInUserQuery);

       endUserQuery = terms.size();
    }



    @Override
    public int hashCode() {

        final int prime = 31;

        int result = prime + ((clauseOffsets == null) ? 0 : clauseOffsets.hashCode());

        for (final Term term: terms) {
            result = prime * result + term.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DocumentFrequencyCorrection other =
                (DocumentFrequencyCorrection) obj;
        if (clauseOffsets == null) {
            if (other.clauseOffsets != null)
                return false;
        } else if (!clauseOffsets.equals(other.clauseOffsets))
            return false;
        if (terms == null) {
            if (other.terms != null)
                return false;
        } else if (terms.size() != other.terms.size())
            return false;
        for (int i = 0, len = terms.size(); i < len; i++) {
            if (!terms.get(i).equals(other.terms.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static class DocumentFrequencyAndTermContext {

        public final int df;
        public final TermStates termStates;

        public DocumentFrequencyAndTermContext(int df, TermStates termStates) {
            this.df = df;
            this.termStates = termStates;
        }


    }

    public static class TermStats {
        final int[] documentFrequencies;
        final TermStates[] termStates;
        final IndexReaderContext topReaderContext;

        public TermStats(final int[] documentFrequencies, final TermStates[] termStates,
                         final IndexReaderContext topReaderContext) {
            this.documentFrequencies = documentFrequencies;
            this.termStates = termStates;
            this.topReaderContext = topReaderContext;
        }
    }

}
