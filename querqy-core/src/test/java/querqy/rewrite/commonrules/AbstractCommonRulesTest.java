package querqy.rewrite.commonrules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import querqy.model.ExpandedQuery;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.commonrules.model.Term;

public abstract class AbstractCommonRulesTest {
    
   public final static Map<String, Object> EMPTY_CONTEXT = new HashMap<>();

   protected ExpandedQuery makeQuery(String input) {
      return new ExpandedQuery(new WhiteSpaceQuerqyParser().parse(input));
   }

   protected Term mkTerm(String s) {
      return new Term(s.toCharArray(), 0, s.length(), null);
   }

   protected Term mkTerm(String s, String... fieldName) {
      return new Term(s.toCharArray(), 0, s.length(), Arrays.asList(fieldName));
   }

}
