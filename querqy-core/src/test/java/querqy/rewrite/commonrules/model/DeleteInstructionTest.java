package querqy.rewrite.commonrules.model;

import org.junit.Test;
import querqy.Constants;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.*;

public class DeleteInstructionTest extends AbstractCommonRulesTest {
    
   @Test
   public void testThatNothingIsDeletedIfWeWouldEndUpWithAnEmptyQuery() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Properties(new Instructions(Arrays.asList((Instruction) delete))));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

      ExpandedQuery query = makeQuery("a");
      Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
                    dmq(
                            term("a")
                       )
            ));

   }

   @Test
   public void testThatTermIsRemovedIfThereIsAnotherTermInTheSameDMQ() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Properties(new Instructions(Arrays.asList((Instruction) delete))));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = (Query) expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);
      querqy.model.Term termB = new querqy.model.Term(dmq, null, "b");
      dmq.addClause(termB);

      Query rewritten = (Query) rewriter.rewrite(expandedQuery, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
                    dmq(
                            term("b")
                       )
            ));
   }

   @Test
   public void testThatTermIsRemovedOnceIfItExistsTwiceInSameDMQAndNoOtherTermExistsInQuery() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Properties(new Instructions(Arrays.asList((Instruction) delete))));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = (Query) expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);

      querqy.model.Term termB = new querqy.model.Term(dmq, null, "a");
      dmq.addClause(termB);

      Query rewritten = (Query) rewriter.rewrite(expandedQuery, EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

   @Test
   public void testThatTermIsRemovedIfThereASecondDMQWithoutTheTerm() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Properties(new Instructions(Arrays.asList((Instruction) delete))));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

      Query rewritten = (Query) rewriter.rewrite(makeQuery("a b"), EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("b")
            )
            ));
   }

   @Test
   public void testThatTermIsNotRemovedOnceIfThereASecondDMQWithTheSameTermAndNoOtherTermExists() throws Exception {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Properties(new Instructions(Arrays.asList((Instruction) delete))));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);

      Query rewritten = (Query) rewriter.rewrite(makeQuery("a a"), EMPTY_CONTEXT).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

   @Test
   public void testThatDeleteIsAppliedToWildcardInput() throws Exception {

       RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);


       Input input = (Input) LineParser.parseInput("k*");

       DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

       builder.addRule(input, new Properties(new Instructions(Collections.singletonList((Instruction) deleteInstruction))));


       RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);


       ExpandedQuery query = makeQuery("x klm");


       Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();


       assertThat(rewritten,
               bq(
                       dmq(
                               term("x")
                       )
               ));


   }

    @Test
    public void testThatDeleteIsAppliedToMultiTermWildcardInput() throws Exception {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);


        Input input = (Input) LineParser.parseInput("ab k*");

        DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

        builder.addRule(input, new Properties(new Instructions(Collections.singletonList((Instruction) deleteInstruction))));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);


        ExpandedQuery query = makeQuery("x ab klm");


        Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();


        assertThat(rewritten,
                bq(
                        dmq(
                                term("x")
                        )
                ));


    }

    @Test
    public void testThatWilcardTermIsNotDeletedIfItIsTheOnlyQueryTerm() throws Exception {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        Input input = (Input) LineParser.parseInput("k*");

        DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

        builder.addRule(input, new Properties(new Instructions(Collections.singletonList((Instruction) deleteInstruction))));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, Constants.DEFAULT_SELECTION_STRATEDGY);


        ExpandedQuery query = makeQuery("klm");


        Query rewritten = (Query) rewriter.rewrite(query, EMPTY_CONTEXT).getUserQuery();


        assertThat(rewritten,
                bq(
                        dmq(
                                term("klm")
                        )
                ));


    }
}
