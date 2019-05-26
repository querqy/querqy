package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static querqy.QuerqyMatchers.*;
import static querqy.rewrite.commonrules.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Collections;

import org.junit.Test;

import querqy.model.*;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

public class DeleteInstructionTest extends AbstractCommonRulesTest {

    @Test
    public void testThatNothingIsDeletedIfWeWouldEndUpWithAnEmptyQuery() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DeleteInstruction delete = new DeleteInstruction(Collections.singletonList(mkTerm("a")));
        builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
                new Instructions(1, "1", Collections.singletonList(delete)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a");
        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
            bq(
                    dmq(
                            term("a")
                       )
            ));

    }

    @Test
    public void testThatTermIsRemovedIfThereIsAnotherTermInTheSameDMQ() {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DeleteInstruction delete = new DeleteInstruction(Collections.singletonList(mkTerm("a")));
        builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
                new Instructions(1, "1", Collections.singletonList(delete)));
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


        ExpandedQuery expandedQuery = makeQuery("a");
        Query query = (Query) expandedQuery.getUserQuery();

        DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);
        querqy.model.Term termB = new querqy.model.Term(dmq, null, "b");
        dmq.addClause(termB);

        Query rewritten = (Query) rewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter()).getUserQuery();

        assertThat(rewritten,
            bq(
                    dmq(
                            term("b")
                       )
            ));
   }

   @Test
   public void testThatTermIsRemovedOnceIfItExistsTwiceInSameDMQAndNoOtherTermExistsInQuery() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Collections.singletonList(mkTerm("a")));
      builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
              new Instructions(1, "1", Collections.singletonList(delete)));
      RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


       ExpandedQuery expandedQuery = makeQuery("a");
      Query query = (Query) expandedQuery.getUserQuery();

      DisjunctionMaxQuery dmq = query.getClauses(DisjunctionMaxQuery.class).get(0);

      querqy.model.Term termB = new querqy.model.Term(dmq, null, "a");
      dmq.addClause(termB);

      Query rewritten = (Query) rewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter()).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

   @Test
   public void testThatTermIsRemovedIfThereASecondDMQWithoutTheTerm() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Collections.singletonList(mkTerm("a")));
      builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
              new Instructions(1, "1", Collections.singletonList(delete)));
      RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


       Query rewritten = (Query) rewriter.rewrite(makeQuery("a b"), new EmptySearchEngineRequestAdapter()).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("b")
            )
            ));
   }

   @Test
   public void testThatTermIsNotRemovedOnceIfThereASecondDMQWithTheSameTermAndNoOtherTermExists() {
      RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
      DeleteInstruction delete = new DeleteInstruction(Collections.singletonList(mkTerm("a")));
      builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
              new Instructions(1, "1", Collections.singletonList(delete)));
      RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);


       Query rewritten = (Query) rewriter.rewrite(makeQuery("a a"), new EmptySearchEngineRequestAdapter()).getUserQuery();

      assertThat(rewritten,
            bq(
            dmq(
            term("a")
            )
            ));
   }

   @Test
   public void testThatDeleteIsAppliedToWildcardInput() {

       RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);


       Input input = (Input) LineParser.parseInput("k*");

       DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

       builder.addRule(input, new Instructions(1, "1", Collections.singletonList(deleteInstruction)));

       RulesCollection rules = builder.build();
       CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);



       ExpandedQuery query = makeQuery("x klm");


       Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();


       assertThat(rewritten,
               bq(
                       dmq(
                               term("x")
                       )
               ));


   }

    @Test
    public void testThatDeleteIsAppliedToMultiTermWildcardInput() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);


        Input input = (Input) LineParser.parseInput("ab k*");

        DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

        builder.addRule(input, new Instructions(1, "1", Collections.singletonList(deleteInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("x ab klm");


        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();


        assertThat(rewritten,
                bq(
                        dmq(
                                term("x")
                        )
                ));


    }

    @Test
    public void testThatWilcardTermIsNotDeletedIfItIsTheOnlyQueryTerm() {

        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);

        Input input = (Input) LineParser.parseInput("k*");

        DeleteInstruction deleteInstruction = new DeleteInstruction(input.getInputTerms());

        builder.addRule(input, new Instructions(1, "1", Collections.singletonList(deleteInstruction)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);



        ExpandedQuery query = makeQuery("klm");


        Query rewritten = (Query) rewriter.rewrite(query, new EmptySearchEngineRequestAdapter()).getUserQuery();


        assertThat(rewritten,
                bq(
                        dmq(
                                term("klm")
                        )
                ));


    }
}
