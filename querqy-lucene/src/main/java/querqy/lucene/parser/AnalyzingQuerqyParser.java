package querqy.lucene.parser;

import java.io.IOException;
import java.util.Collection;

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
import com.google.common.collect.Lists;

/**
 * A {@linkplain QuerqyParser} that works solely on Lucene {@linkplain Analyzer}
 * s. The query is run through a query analyzer. The resulting tokens are used
 * to lookup synonyms with the synonym analyzer. The tokens remaining in that
 * analyzer are treated as synonyms.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class AnalyzingQuerqyParser implements QuerqyParser {

   private final Analyzer queryAnalyzer;
   private final Analyzer synonymAnalyzer;

   public AnalyzingQuerqyParser(Analyzer queryAnalyser, Analyzer synonymAnalyzer) {
      Preconditions.checkNotNull(queryAnalyser);

      this.queryAnalyzer = queryAnalyser;
      this.synonymAnalyzer = synonymAnalyzer;
   }

   @Override
   public Query parse(String input) {
      Preconditions.checkNotNull(input);

      // get dismax terms
      Collection<String> terms = analyze(input, queryAnalyzer);

      // construct query while iterating terms
      Query query = new Query();

      for (String term : terms) {
         DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Occur.SHOULD, false);
         Term t = new Term(dmq, term);
         dmq.addClause(t);
         query.addClause(dmq);

         if (synonymAnalyzer != null) {
            // evaluate synonyms
            Collection<String> synonyms = analyze(term, synonymAnalyzer);
            if (!synonyms.isEmpty()) {
               for (CharSequence synonym : synonyms) {
                  dmq.addClause(new Term(dmq, synonym, true));
               }
            }
         }
      }

      return query;
   }

   /**
    * Analyzes the given string using the given {@link Analyzer} (-chain).
    */
   protected Collection<String> analyze(String input, Analyzer analyzer) {
      Preconditions.checkNotNull(input);
      Preconditions.checkNotNull(analyzer);

      Collection<String> result = Lists.newArrayList();
      try (TokenStream tokenStream = analyzer.tokenStream("querqy", new CharSequenceReader(input))) {
         tokenStream.reset();
         CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
         while (tokenStream.incrementToken()) {
            // needs to converted to string, because on tokenStream.end() the
            // charTermAttribute will be flushed.
            result.add(charTermAttribute.toString());
         }
         tokenStream.end();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return result;
   }
}
