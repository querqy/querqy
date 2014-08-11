/**
 * 
 */
package querqy.rewrite.lucene;

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

import querqy.model.Query;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;

/**
 * @author rene
 *
 */
public class LuceneSynonymsRewriterFactory implements RewriterFactory {

   final SynonymMap synonymMap;

   public LuceneSynonymsRewriterFactory(InputStream is, boolean expand, final boolean ignoreCase) throws IOException {

      Analyzer analyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer tokenizer = new KeywordTokenizer(reader);
            TokenStream stream = ignoreCase ? new LowerCaseFilter(Version.LUCENE_4_9, tokenizer) : tokenizer;
            return new TokenStreamComponents(tokenizer, stream);
         }
      };

      SolrSynonymParser parser = new SolrSynonymParser(true, expand, analyzer);
      try {
         parser.parse(new InputStreamReader(is));
      } catch (ParseException e) {
         throw new IOException(e);
      }
      synonymMap = parser.build();
   }

   @Override
   public QueryRewriter createRewriter(Query input, Map<String, ?> context) {
      return new LuceneSynonymsRewriter(synonymMap);
   }

}
