/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.Version;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class LuceneSynonymsRewriterFactory implements RewriterFactory {

   @Override
   public QueryRewriter createRewriter(ExpandedQuery input, Map<String, ?> context) {
      return new LuceneSynonymsRewriter(synonymMap);
   }

   SynonymMap synonymMap = null;
   final SolrSynonymParser parser;

   public LuceneSynonymsRewriterFactory(boolean expand, final boolean ignoreCase) throws IOException {

      Analyzer analyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer tokenizer = new KeywordTokenizer(reader);
            TokenStream stream = tokenizer;
            if (ignoreCase) {
               stream = new LowerCaseFilter(Version.LUCENE_4_9, stream);
            }
            return new TokenStreamComponents(tokenizer, stream);
         }
      };

      parser = new SolrSynonymParser(true, expand, analyzer);
   }

   public void addResource(InputStream is) throws IOException {
      try {
         parser.parse(new InputStreamReader(is));
      } catch (ParseException e) {
         throw new IOException(e);
      }
   }

   public void build() throws IOException {
      synonymMap = parser.build();
   }

}
