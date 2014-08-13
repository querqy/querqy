/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

/**
 * @author rene
 *
 */
public class TermQueryFactory implements LuceneQueryFactory<TermQuery> {
    
    protected final Term term; 
    protected final float boost;
    protected int myDf = -1;
    protected final IndexSearcher indexSearcher;
    
    public TermQueryFactory(Term term, float boost, IndexSearcher indexSearcher) {
        this.term = term;
        this.boost = boost;
        this.indexSearcher = indexSearcher;
    }

    @Override
    public TermQuery createQuery(DocumentFrequencyCorrection dfc, boolean isBelowDMQ) throws IOException {
    	
    	if (!isBelowDMQ) {
    		// a TQ might end up directly under a BQ as an optimisation
    		// make sure, we collect the df
    		dfc.newClause();
    		collectMaxDocFreqInSubtree(dfc);
    	}
    	
    	int dfToSet = dfc.getDocumentFrequencyToSet();
        TermQuery tq = (dfToSet == myDf) || (dfToSet < 1) ? new TermQuery(term) : makeTermQuery(dfToSet);
        tq.setBoost(boost);
        return tq;
        
    }
    
    TermQuery makeTermQuery(int dfToSet) throws IOException {
    	
    	IndexReaderContext context = indexSearcher.getTopReaderContext();
    	
    	// This is copied/modified from org.apache.lucene.index.TermContext.build(IndexReaderContext, Term)
    	// Though TermQuery and TermState allow us to pass/set an arbitrary df value, 
    	// org.apache.lucene.search.TermStatistics.TermStatistics(BytesRef, long, long) later asserts df <= total term frequency, 
    	// which would cause an assertion error, if assertions are enabled. We create the TermContext ourselves as a workaround:
    	assert context != null && context.isTopLevel;
        final String field = term.field();
        final BytesRef bytes = term.bytes();
        final TermContext perReaderTermState = new TermContext(context);
        
        boolean updated = false;
        
        for (final AtomicReaderContext ctx : context.leaves()) {
          //if (DEBUG) System.out.println("  r=" + leaves[i].reader);
          final Fields fields = ctx.reader().fields();
          if (fields != null) {
            final Terms terms = fields.terms(field);
            if (terms != null) {
              final TermsEnum termsEnum = terms.iterator(null);
              if (termsEnum.seekExact(bytes)) { 
                final TermState termState = termsEnum.termState();
                // modified from TermContext.build():
                perReaderTermState.register(termState, ctx.ord, termsEnum.docFreq(), -1);
                updated = true;
              }
            }
          }
        }
        
        if (updated) {
        	perReaderTermState.setDocFreq(dfToSet);
        	return new TermQuery(term, perReaderTermState);
        } else {
        	// term does not exist
        	return new TermQuery(term);
        }
    	
    }

	@Override
	public void collectMaxDocFreqInSubtree(DocumentFrequencyCorrection dfc) {
		if (myDf == -1) {
			myDf = dfc.collect(term);
		}
	}

}
