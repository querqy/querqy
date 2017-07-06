/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    
    protected TermStats doCalculateTermContexts(final IndexReaderContext indexReaderContext)
            throws IOException {

       final int[] dfs = new int[terms.size()];
       final TermContext[] contexts = new TermContext[dfs.length];
       
       for (int i = 0; i < dfs.length; i++) {
           
           final Term term = terms.get(i);
           
           contexts[i] = new TermContext(indexReaderContext);
           
           for (final LeafReaderContext ctx : indexReaderContext.leaves()) {
               final Fields fields = ctx.reader().fields();
               if (fields != null) {
                 final Terms terms = fields.terms(term.field());
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
                           contexts[pos].accumulateStatistics(delta, -1);
                       }
                   }
                   pos++;
               }
           }
       }

        return new TermStats(dfs, contexts, indexReaderContext);
       
   }


}
