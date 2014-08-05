package querqy.lucene.parser;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;

import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.SubQuery.Occur;
import querqy.model.Term;
import querqy.parser.QuerqyParser;

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
      Preconditions.checkNotNull(synonymAnalyzer);

      this.queryAnalyzer = queryAnalyser;
      this.synonymAnalyzer = synonymAnalyzer;
   }

   @Override
   public Query parse(String input) {
      Preconditions.checkNotNull(input);

      try {
         // get dismax terms
         Collection<CharSequence> terms = analyze(CharSource.wrap(input), queryAnalyzer);

         // construct query while iterating terms
         Query query = new Query();

         for (CharSequence term : terms) {
            DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Occur.SHOULD, false);
            Term t = new Term(dmq, term);
            dmq.addClause(t);
            query.addClause(dmq);

            // evaluate synonyms
            Collection<CharSequence> synonyms = analyze(CharSource.wrap(term), synonymAnalyzer);
            if (!synonyms.isEmpty()) {
               for (CharSequence synonym : synonyms) {
                  dmq.addClause(new Term(dmq, synonym, true));
               }
            }
         }

         return query;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Analyzes the given string using the given analyzer.
    */
   protected Collection<CharSequence> analyze(CharSource input, Analyzer analyzer) throws IOException {
      Preconditions.checkNotNull(input);
      Preconditions.checkNotNull(analyzer);

      TokenStream tokenStream = analyzer.tokenStream("querqy", input.openStream());
      Collection<CharSequence> result = Lists.newArrayList();
      try {
         tokenStream.reset();
         CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
         while (tokenStream.incrementToken()) {
            result.add(charTermAttribute);
         }
      } finally {
         tokenStream.end();
         tokenStream.close();
      }

      return result;
   }
}
