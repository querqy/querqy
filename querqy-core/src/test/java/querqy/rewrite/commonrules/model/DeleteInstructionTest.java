package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static querqy.QuerqyMatchers.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;

public class DeleteInstructionTest extends AbstractCommonRulesTest {
    
   @Test
   public void testThatNothingIsDeletedIfWeWouldEndUpWithAnEmptyQuery() {

      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Arrays.asList(mkTerm("a")));
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery query = makeQuery("a");
      Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchRequestAdapter()).getUserQuery();

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
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = (Query) expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);
      querqy.model.Term termB = new querqy.model.Term(dmq, null, "b");
      dmq.addClause(termB);

      Query rewritten = (Query) rewriter.rewrite(expandedQuery, new EmptySearchRequestAdapter()).getUserQuery();

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
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      ExpandedQuery expandedQuery = makeQuery("a");
      Query query = (Query) expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);

      querqy.model.Term termB = new querqy.model.Term(dmq, null, "a");
      dmq.addClause(termB);

      Query rewritten = (Query) rewriter.rewrite(expandedQuery, new EmptySearchRequestAdapter()).getUserQuery();

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
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      Query rewritten = (Query) rewriter.rewrite(makeQuery("a b"), new EmptySearchRequestAdapter()).getUserQuery();

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
      builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) delete)));
      RulesCollection rules = builder.build();
      CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

      Query rewritten = (Query) rewriter.rewrite(makeQuery("a a"), new EmptySearchRequestAdapter()).getUserQuery();

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

       builder.addRule(input, new Instructions(Collections.singletonList((Instruction) deleteInstruction)));


       RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


       ExpandedQuery query = makeQuery("x klm");


       Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchRequestAdapter()).getUserQuery();


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

        builder.addRule(input, new Instructions(Collections.singletonList((Instruction) deleteInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("x ab klm");


        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchRequestAdapter()).getUserQuery();


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

        builder.addRule(input, new Instructions(Collections.singletonList((Instruction) deleteInstruction)));


        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);


        ExpandedQuery query = makeQuery("klm");


        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchRequestAdapter()).getUserQuery();


        assertThat(rewritten,
                bq(
                        dmq(
                                term("klm")
                        )
                ));


    }
}
