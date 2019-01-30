/**
 * 
 */
package querqy.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.model.Clause.Occur;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.Properties;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;

/**
 * @author René Kriegler, @renekrie
 *
 * 
 */
@Deprecated
public class SynonymFormatCommonRulesRewriterFactory implements
      RewriterFactoryAdapter {

   /*
    * (non-Javadoc)
    * 
    * @see
    * querqy.solr.RewriterFactoryAdapter#createRewriterFactory(org.apache.solr
    * .common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
    */
   @Override
   public RewriterFactory createRewriterFactory(NamedList<?> args,
         ResourceLoader resourceLoader) throws IOException {

      String boostUp = (String) args.get("boostUp");
      String boostDown = (String) args.get("boostDown");
      Boolean ignoreCase = args.getBooleanArg("ignoreCase");

      if ((boostUp == null) && (boostDown == null)) {
         // remove this check when we load other instruction types
         throw new IllegalArgumentException("At least on of boostUp or boostDown must be configured");
      }

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(ignoreCase != null && ignoreCase);

      if (boostUp != null) {
         addBoostInstructions(builder, BoostDirection.UP, 1f, resourceLoader, boostUp);
      }

      if (boostDown != null) {
         addBoostInstructions(builder, BoostDirection.DOWN, 1f, resourceLoader, boostDown);
      }

      return new RulesRewriterFactory(builder.build());
   }

   void addBoostInstructions(RulesCollectionBuilder builder, BoostDirection direction, float boost,
         ResourceLoader resourceLoader, String resourceName) throws IOException {

      try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceLoader.openResource(resourceName)))) {

         String line;

         while ((line = reader.readLine()) != null) {

            line = line.trim();
            if (line.length() > 0) {

               int pos = line.indexOf("#");

               if (pos > -1) {
                  if (line.length() == 1) {
                     continue;
                  }
                  line = line.substring(0, pos).trim();
               }

               pos = line.indexOf("=>");
               if (pos > 0) {
                  String inputsStr = line.substring(0, pos).trim();
                  if (pos < line.length() - 2) {

                     String instructionStr = line.substring(pos + 2).trim();
                     if (instructionStr.length() > 0) {

                        List<Input> inputs = makeInputs(inputsStr);
                        if (inputs.size() > 0) {

                           for (String t : instructionStr.split(",")) {
                              t = t.trim();
                              if (t.length() > 0) {
                                 Query query = termsToQuery(t);
                                 if (!query.getClauses().isEmpty()) {
                                    for (Input input : inputs) {
                                       BoostInstruction bi = new BoostInstruction(query, direction, boost);
                                       builder.addRule(input, buildProperty(new Instructions(Collections.singletonList((Instruction) bi))));
                                    }
                                 }
                              }
                           }

                        }

                     }
                  }
               }

            }

         }
      }
   }

   private static Properties buildProperty(Instructions instructions) {
      return new Properties(instructions);
   }

   List<Input> makeInputs(String inputsStr) {

      List<Input> result = new LinkedList<>();

      for (String inputStr : inputsStr.split(",")) {

         inputsStr = inputsStr.trim();

         if (inputStr.length() > 0) {

            List<querqy.rewrite.commonrules.model.Term> terms = new LinkedList<>();
            for (String termStr : inputStr.split("\\s+")) {
               if (termStr.length() > 0) {
                  terms.add(new querqy.rewrite.commonrules.model.Term(termStr.toCharArray(), 0, termStr.length(), null));
               }
            }

            if (!terms.isEmpty()) {
               result.add(new Input(terms));
            }
         }
      }

      return result;
   }

   Query termsToQuery(String termsQuery) {

      Query query = new Query();

      for (String t : termsQuery.split("\\s+")) {

         if (t.length() > 0) {

            DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Occur.SHOULD, false);
            query.addClause(dmq);

            Term term = new Term(dmq, t);
            dmq.addClause(term);

         }
      }

      return query;

   }

   public static class RulesRewriterFactory implements RewriterFactory {

      final RulesCollection rules;

      public RulesRewriterFactory(RulesCollection rules) {
         this.rules = rules;
      }

      @Override
      public QueryRewriter createRewriter(ExpandedQuery input,
            Map<String, ?> context) {
         return new CommonRulesRewriter(rules);
      }

    @Override
    public Set<Term> getGenerableTerms() {
        Set<Term> result = new HashSet<>();
        for (Instruction instruction: rules.getInstructions()) {
            result.addAll(instruction.getGenerableTerms());
        }
        return result;
    }

   }

}
