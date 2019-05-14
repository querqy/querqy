/**
 * 
 */
package querqy.lucene.contrib.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

/**
 * @author rene
 *
 */
public class LuceneSynonymsRewriterFactory extends RewriterFactory {

    SynonymMap synonymMap = null;
    final SolrSynonymParser parser;

    public LuceneSynonymsRewriterFactory(final String rewriterId, final boolean expand, final boolean ignoreCase) {
        super(rewriterId);

        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer tokenizer = new KeywordTokenizer();
                TokenStream stream = tokenizer;
                if (ignoreCase) {
                   stream = new LowerCaseFilter(stream);
                }
                return new TokenStreamComponents(tokenizer, stream);
            }
        };

        parser = new SolrSynonymParser(true, expand, analyzer);
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new LuceneSynonymsRewriter(synonymMap);
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

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

}
