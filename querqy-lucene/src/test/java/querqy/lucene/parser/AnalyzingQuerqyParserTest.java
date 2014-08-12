package querqy.lucene.parser;

import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
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
    * Query analyzer.
    * Just lower cases the input.
    */
   private Analyzer queryAnalyzer;

   /**
    * Synonym analyzer.
    * Single synonym "synonym" for "test". 
    */
   private Analyzer optSynonymAnalyzer;

   @Before
   public void createAnalyzers() throws Exception {
      queryAnalyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            return new TokenStreamComponents(new MockTokenizer(reader));
         }
      };

      SynonymMap.Builder builder = new SynonymMap.Builder(true);
      builder.add(new CharsRef("test"), new CharsRef("synonym"), false);
      final SynonymMap synonyms = builder.build();

      optSynonymAnalyzer = new Analyzer() {
         @Override
         protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            MockTokenizer tokenizer = new MockTokenizer(reader);
            return new TokenStreamComponents(tokenizer, new SynonymFilter(tokenizer, synonyms, true));
         }
      };
   }

   /**
    * Test for {@link AnalyzingQuerqyParser#parse(String)}.
    */
   @Test
   public void parse_withoutSynonyms() {
      QuerqyParser parser = new AnalyzingQuerqyParser(queryAnalyzer, null);
      Query query = parser.parse("test");
      
      assertThat(query, bq(dmq(term("test"))));
   }

   /**
    * Test for {@link AnalyzingQuerqyParser#parse(String)}.
    */
   @Test
   public void parse_withSynonyms() {
      QuerqyParser parser = new AnalyzingQuerqyParser(queryAnalyzer, optSynonymAnalyzer);
      Query query = parser.parse("test");

      assertThat(query, bq(dmq(term("test"), term("synonym"))));
   }
}
