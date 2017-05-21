/**
 * 
 */
package querqy.lucene.rewrite;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;

import java.io.IOException;


/**
 * A TermQuery that depends on other term queries for the calculation of the document frequency
 * and/or the boost factor (field weight). 
 * 
 * @author René Kriegler, @renekrie
 *
 */
public class DependentTermQuery extends TermQuery {

    final int tqIndex;
    final DocumentFrequencyAndTermContextProvider dftcp;
    final FieldBoost fieldBoost;

    public DependentTermQuery(final Term term, final DocumentFrequencyAndTermContextProvider dftcp,
                              final FieldBoost fieldBoost) {
        this(term, dftcp, dftcp.termIndex(), fieldBoost);
    }

    protected DependentTermQuery(final Term term, final DocumentFrequencyAndTermContextProvider dftcp,
                                 final int tqIndex, final FieldBoost fieldBoost) {

        super(term);

        if (fieldBoost == null) {
            throw new IllegalArgumentException("FieldBoost must not be null");
        }

        if (dftcp == null) {
            throw new IllegalArgumentException("DocumentFrequencyAndTermContextProvider must not be null");
        }

        if (term == null) {
            throw new IllegalArgumentException("Term must not be null");
        }

        this.tqIndex  = tqIndex;
        this.dftcp = dftcp;
        this.fieldBoost = fieldBoost;
    }

    @Override
    public Weight createWeight(final IndexSearcher searcher, final boolean needsScores) throws IOException {
        throw new UnsupportedOperationException(DependentTermQuery.class.getName() +
                " does not implement createWeight. Call rewrite() to get an executable query.");
    }

    @Override
    public Query rewrite(final IndexReader reader) throws IOException {

        final DocumentFrequencyAndTermContextProvider.DocumentFrequencyAndTermContext dftc =
                dftcp.getDocumentFrequencyAndTermContext(tqIndex, reader.getContext());

        if (dftc.df < 1) {
            return new MatchNoDocsQuery();
        }

        return new BoostQuery(new TermQuery(getTerm(), dftc.termContext),
                fieldBoost.getBoost(getTerm().field(), reader)).rewrite(reader);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime  + tqIndex;
        result = prime * result + fieldBoost.hashCode();
       // result = prime * result + getTerm().hashCode(); handled in super class
        return super.hashCode() ^ result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (!super.equals(obj)) {
            return false;
        }

        final DependentTermQuery other = (DependentTermQuery) obj;
        if (tqIndex != other.tqIndex)
            return false;
        if (!fieldBoost.equals(other.fieldBoost))
            return false;

        return true; // getTerm().equals(other.getTerm());  already assured in super class

    }
    
    @Override
    public String toString(final String field) {
        final Term term = getTerm();
        final StringBuilder buffer = new StringBuilder();
        if (!term.field().equals(field)) {
          buffer.append(term.field());
          buffer.append(":");
        }
        buffer.append(term.text());
        buffer.append(fieldBoost.toString(term.field()));
        return buffer.toString();
        
    }
    
    public FieldBoost getFieldBoost() {
        return fieldBoost;
    }
    

    
}
