package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

public class DecorateInstructionTest extends AbstractCommonRulesTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testThatSingleDecorationIsEmitted() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DecorateInstruction deco = new DecorateInstruction("deco1");
        
        builder.addRule(new Input(Collections.singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", Collections.singletonList(deco)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a x");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        
        
        assertThat((Set<Object>)searchEngineRequestAdapter.getContext().get(DecorateInstruction.CONTEXT_KEY),
              contains( 
                      equalTo((Object) "deco1")
              ));
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDecorationForEmptyInput() throws Exception {
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DecorateInstruction deco = new DecorateInstruction("deco1");
        builder.addRule((Input) LineParser.parseInput(LineParser.BOUNDARY + "" + LineParser.BOUNDARY),
                    new Instructions(1, "1", Collections.singletonList( deco)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        
        
        assertThat((Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.CONTEXT_KEY),
              contains( 
                      equalTo((Object) "deco1")
              ));
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testThatMultipleDecorationsAreEmitted() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DecorateInstruction deco1 = new DecorateInstruction("deco1");
        DecorateInstruction deco2 = new DecorateInstruction("deco2");
        DecorateInstruction deco3 = new DecorateInstruction("deco3");
        
        builder.addRule(new Input(Collections.singletonList(mkTerm("x")), false, false, "x"),
                new Instructions(1, "1", Arrays.asList(deco1, deco2)));
        builder.addRule(new Input(Collections.singletonList(mkTerm("a")), false, false, "a"),
                new Instructions(2, "2", Collections.singletonList(deco3)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules, DEFAULT_SELECTION_STRATEGY);

        ExpandedQuery query = makeQuery("a x");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        
        
        assertThat(
                (Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.CONTEXT_KEY),
                containsInAnyOrder((Object) "deco1", (Object) "deco2", (Object) "deco3")
                              
              );

        
        
    }
}
