/**
 * 
 */
package querqy.parser;

import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Query;
import querqy.model.Term;

/**
 * A simple QuerqyParser that breaks a query string into clauses at whitespace.
 * Clauses are either terms or terms with a boolean operator prefix
 * (&quot;+&quot; &quot;-&quot;).
 * 
 * @author René Kriegler, @renekrie
 *
 */
public class WhiteSpaceQuerqyParser implements QuerqyParser {

   private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

   /*
    * (non-Javadoc)
    * 
    * @see querqy.parser.QuerqyParser#parse(java.lang.String)
    */
   @Override
   public Query parse(String input) {
      Query query = new Query();
      for (String token : Splitter.on(WHITESPACE_PATTERN).split(input)) {

         Occur occur = Occur.SHOULD;

         if (token.length() > 1) {
            if (token.startsWith("+")) {
               occur = Occur.MUST;
               token = token.substring(1);
            } else if (token.startsWith("-")) {
               occur = Occur.MUST_NOT;
               token = token.substring(1);
            }
         }
         DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, occur, false);
         query.addClause(dmq);
         dmq.addClause(new Term(dmq, token));
      }
      return query;
   }

}
