package querqy.lucene.parser;

import java.io.IOException;

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.parser.QuerqyParser;

import com.google.common.base.Preconditions;

/**
 * A {@linkplain QuerqyParser} that works solely on Lucene {@linkplain Analyzer}s. 
 * The query is run through a query analyzer. The resulting tokens are used
 * to lookup synonyms with the synonym analyzer. The tokens remaining in that
 * analyzer are treated as synonyms.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class AnalyzingQuerqyParser implements QuerqyParser {
   /**
    * {@link Analyzer} for the query.
    */
   private final Analyzer queryAnalyzer;

   /**
    * {@link Analyzer} for the synonyms.
    */
   private final Analyzer optSynonymAnalyzer;

   /**
    * Constructor.
    * 
    * @param queryAnalyser
    *           {@link Analyzer} for the query.
    * @param optSynonymAnalyzer
    *           {@link Analyzer} for the synonyms.
    */
   public AnalyzingQuerqyParser(Analyzer queryAnalyser, Analyzer optSynonymAnalyzer) {
      Preconditions.checkNotNull(queryAnalyser);

      this.queryAnalyzer = queryAnalyser;
      this.optSynonymAnalyzer = optSynonymAnalyzer;
   }

   /**
    * Generate query for the input.
    * 
    * @param input
    *           Search term.
    */
   @Override
   public Query parse(String input) {
      Preconditions.checkNotNull(input);

      try (TokenStream queryTokens = queryAnalyzer.tokenStream("querqy", new CharSequenceReader(input))) {
         Query query = new Query();

         queryTokens.reset();
         CharTermAttribute original = queryTokens.addAttribute(CharTermAttribute.class);
         while (queryTokens.incrementToken()) {
            DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Occur.SHOULD, false);
            // We need to copy "original" per toString() here, because "original" is transient.
            dmq.addClause(new Term(dmq, original.toString()));
            query.addClause(dmq);
            
            if (optSynonymAnalyzer != null) {
               addSynonyms(dmq, original);
            }            
         }
         queryTokens.end();

         return query;

      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Add terms to the query for the synonyms.
    * 
    * @param dmq
    *           {@link DisjunctionMaxQuery}
    * @param original
    *           Original term to determine synonyms for.
    */
   private void addSynonyms(DisjunctionMaxQuery dmq, CharSequence original) throws IOException {
      try (TokenStream synonymTokens = optSynonymAnalyzer.tokenStream("querqy", new CharSequenceReader(original))) {
         synonymTokens.reset();
         CharTermAttribute generated = synonymTokens.addAttribute(CharTermAttribute.class);
         while (synonymTokens.incrementToken()) {
            // We need to copy "generated" per toString() here, because "generated" is transient.
            dmq.addClause(new Term(dmq, generated.toString(), true));
         }
         synonymTokens.end();
      }
   }
}
