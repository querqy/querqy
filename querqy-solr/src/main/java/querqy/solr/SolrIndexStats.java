package querqy.solr;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.solr.search.SolrIndexSearcher;

import querqy.lucene.rewrite.IndexStats;

public class SolrIndexStats implements IndexStats {
   final SolrIndexSearcher searcher;

   public SolrIndexStats(SolrIndexSearcher searcher) {
      this.searcher = searcher;
   }

   @Override
   public int df(Term term) {
      try {
         return searcher.docFreq(term);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

}