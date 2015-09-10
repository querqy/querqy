/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

/**
 * @author René Kriegler, @renekrie
 *
 */
public class DocumentFrequencyCorrection implements DocumentFrequencyAndTermContextProvider {
    
    final List<DependentTermQuery> termQueries = new ArrayList<>(16);
    final List<Integer> clauseOffsets = new ArrayList<>();
    int endUserQuery = -1;
    TermStats termStats = null;

    enum Status {
        USER_QUERY, OTHER_QUERY
    }

    Status status;

    int maxInClause = -1;
    private int maxInUserQuery = -1;

    public DocumentFrequencyCorrection() {
        status = Status.USER_QUERY;
    }
   
   /* (non-Javadoc)
    * @see querqy.lucene.rewrite.DocumentFrequencyAndTermContextProvider#registerTermQuery(querqy.lucene.rewrite.DependentTermQuery)
    */
   @Override
   public int registerTermQuery(DependentTermQuery tq) {
       int idx = termQueries.size();
       termQueries.add(tq);
       return idx;
   }
   
   /* (non-Javadoc)
    * @see querqy.lucene.rewrite.DocumentFrequencyAndTermContextProvider#getDocumentFrequencyAndTermContext(int, org.apache.lucene.search.IndexSearcher)
    */
   @Override
   public DocumentFrequencyAndTermContext getDocumentFrequencyAndTermContext(int tqIndex, IndexSearcher searcher) throws IOException {

       TermStats ts = termStats;
       if (ts == null || ts.topReaderContext != searcher.getTopReaderContext()) {
           ts = calculateTermContexts(searcher);
       }
               
       return new DocumentFrequencyAndTermContext(ts.documentFrequencies[tqIndex], ts.termContexts[tqIndex]);
   }
   
   private TermStats calculateTermContexts(IndexSearcher searcher) throws IOException {
       IndexReaderContext topReaderContext = searcher.getTopReaderContext();
       
       int[] dfs = new int[termQueries.size()];
       TermContext[] contexts = new TermContext[dfs.length];
       
       for (int i = 0; i < dfs.length; i++) {
           
           Term term = termQueries.get(i).getTerm();
           
           contexts[i] = new TermContext(topReaderContext);
           
           for (final AtomicReaderContext ctx : topReaderContext.leaves()) {
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
       
       return setDfAndContexts(dfs, contexts, topReaderContext);
       
   }
   
   private synchronized TermStats setDfAndContexts(int[] dfs, TermContext[] contexts, IndexReaderContext topReaderContext) {
       TermStats ts = new TermStats(dfs, contexts, topReaderContext);
       this.termStats = ts;
       return ts;
   }

   void newClause() {
      if (status == Status.USER_QUERY) {
         maxInUserQuery = Math.max(maxInClause, maxInUserQuery);
      }
      maxInClause = -1;
      clauseOffsets.add(termQueries.size());
   }

   public void finishedUserQuery() {
      status = Status.OTHER_QUERY;
      maxInUserQuery = Math.max(maxInClause, maxInUserQuery);
      
      endUserQuery = termQueries.size();
   }

   int getDocumentFrequencyToSet() {
      return (status == Status.USER_QUERY || maxInUserQuery < 1) ? maxInClause : maxInClause + maxInUserQuery - 1;
   }
   
   public static class TermStats {
       final int[] documentFrequencies;
       final TermContext[] termContexts;
       final IndexReaderContext topReaderContext;
       
       public TermStats(int[] documentFrequencies, TermContext[] termContexts, IndexReaderContext topReaderContext) {
           this.documentFrequencies = documentFrequencies;
           this.termContexts = termContexts;
           this.topReaderContext = topReaderContext;
       }
   }
   
   public static class DocumentFrequencyAndTermContext {
       
       public final int df;
       public final TermContext termContext;
       
       public DocumentFrequencyAndTermContext(int df, TermContext termContext) {
           this.df = df;
           this.termContext = termContext;
       }
       
       
   }

   @Override
   public int hashCode() {
       final int prime = 31;
       int result = 1;
       result = prime * result
               + ((clauseOffsets == null) ? 0 : clauseOffsets.hashCode());
       
       for (TermQuery termQuery: termQueries) {
           result = prime * result + termQuery.getTerm().hashCode();
       }

       return result;
   }

   @Override
   public boolean equals(Object obj) {
       if (this == obj)
           return true;
       if (obj == null)
           return false;
       if (getClass() != obj.getClass())
           return false;
       DocumentFrequencyCorrection other = (DocumentFrequencyCorrection) obj;
       if (clauseOffsets == null) {
           if (other.clauseOffsets != null)
               return false;
       } else if (!clauseOffsets.equals(other.clauseOffsets))
           return false;
       if (termQueries == null) {
           if (other.termQueries != null)
               return false;
       } else if (termQueries.size() != other.termQueries.size())
           return false;
       for (int i = 0, len = termQueries.size(); i < len; i++) {
           if (!termQueries.get(i).getTerm().equals(other.termQueries.get(i).getTerm())) {
               return false;
           }
       }
       return true;
   }

}
