package querqy.lucene.rewrite;

import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rene on 10/09/2016.
 */
public abstract class AbstractDocumentFrequencyAndTermContextProvider implements DocumentFrequencyAndTermContextProvider {

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


    protected abstract TermStats doCalculateTermContexts(IndexReaderContext indexReaderContext) throws IOException;

    /* (non-Javadoc)
     * @see querqy.lucene.rewrite.DocumentFrequencyAndTermContextProvider#prepareTerm(org.apache.lucene.index.Term)
     */
    @Override
    public void prepareTerm(Term term) {
        terms.add(term);
    }

    @Override
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
    @Override
    public DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(final int tqIndex,
                                                                              final IndexReaderContext indexReaderContext)
            throws IOException {

        TermStats ts = termStats;
        if (ts == null || ts.topReaderContext != indexReaderContext) {
            ts = calculateTermContexts(indexReaderContext);
        }

        return new DocumentFrequencyAndTermContext(ts.documentFrequencies[tqIndex], ts.termContexts[tqIndex]);
    }

    protected TermStats calculateTermContexts(final IndexReaderContext indexReaderContext)
            throws IOException {

        return setTermStats(doCalculateTermContexts(indexReaderContext));

    }

    private synchronized TermStats setTermStats(final TermStats ts) {
        this.termStats = ts;
        return this.termStats;
    }

    @Override
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
        final AbstractDocumentFrequencyAndTermContextProvider other =
                (AbstractDocumentFrequencyAndTermContextProvider) obj;
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

    public static class TermStats {
        final int[] documentFrequencies;
        final TermContext[] termContexts;
        final IndexReaderContext topReaderContext;

        public TermStats(final int[] documentFrequencies, final TermContext[] termContexts,
                         final IndexReaderContext topReaderContext) {
            this.documentFrequencies = documentFrequencies;
            this.termContexts = termContexts;
            this.topReaderContext = topReaderContext;
        }
    }

}
