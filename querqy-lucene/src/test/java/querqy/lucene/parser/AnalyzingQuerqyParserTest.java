package querqy.lucene.parser;

import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Before;
import org.junit.Test;

import querqy.model.Query;
import querqy.parser.QuerqyParser;

/**
 * Test for {@link AnalyzingQuerqyParser}.
 */
public class AnalyzingQuerqyParserTest extends LuceneTestCase {
   /**
    * Query analyzer. Just lower cases the input.
    */
   private Analyzer queryAnalyzer;

   /**
    * Synonym analyzer. Just adds the synonyms "synonym1" and "synonym2" for
    * "test".
    */
   private Analyzer synonymAnalyzer;

   @Before
   public void createAnalyzers() throws Exception {
      queryAnalyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName) {
            // White space tokenizer, to lower case tokenizer.
            return new TokenStreamComponents(new MockTokenizer());
         }
      };

      SynonymMap.Builder builder = new SynonymMap.Builder(true);
      builder.add(new CharsRef("test"), new CharsRef("synonym1"), false);
      builder.add(new CharsRef("test"), new CharsRef("synonym2"), false);
      final SynonymMap synonyms = builder.build();

      synonymAnalyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName) {
            // White space tokenizer, to lower case tokenizer.
            MockTokenizer tokenizer = new MockTokenizer();
            // Filter for adding synonyms
            TokenStream result = new SynonymFilter(tokenizer, synonyms, true);
            // Filter all non-synonyms, because the synonym filter outputs the
            // original token too.
            result = new TypeTokenFilter(result, Collections.singleton(SynonymFilter.TYPE_SYNONYM),
                  true);
            return new TokenStreamComponents(tokenizer, result);
         }
      };
   }

   /**
    * Test for {@link AnalyzingQuerqyParser#parse(String)} without synonyms.
    */
   @Test
   public void parse_withoutSynonyms() {
      QuerqyParser parser = new AnalyzingQuerqyParser(queryAnalyzer, null);
      Query query = parser.parse("test dummy");

      assertThat(query, bq(dmq(term("test")), dmq(term("dummy"))));
   }

   /**
    * Test for {@link AnalyzingQuerqyParser#parse(String)} with synonyms.
    */
   @Test
   public void parse_withSynonyms() {
      QuerqyParser parser = new AnalyzingQuerqyParser(queryAnalyzer, synonymAnalyzer);
      Query query = parser.parse("test dummy");

      // "test" with its synonyms, "dummy" without synonyms, because none are
      // defined for it.
      assertThat(query, bq(dmq(term("test"), term("synonym1"), term("synonym2")), dmq(term("dummy"))));
   }
}
