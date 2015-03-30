package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import querqy.model.ExpandedQuery;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

public class DecorateInstructionTest extends AbstractCommonRulesTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testThatSingleDecorationIsEmitted() {
        
        RulesCollectionBuilder builder = new TrieMapRulesCollectionBuilder(false);
        DecorateInstruction deco = new DecorateInstruction("deco1");
        
        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) deco)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a x");
        Map<String, Object> context = new HashMap<>();
        rewriter.rewrite(query, context);

        
        
        assertThat((Set<Object>)context.get(DecorateInstruction.CONTEXT_KEY),
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
                    new Instructions(Arrays.asList((Instruction) deco)));
        
        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("");
        Map<String, Object> context = new HashMap<>();
        rewriter.rewrite(query, context);

        
        
        assertThat((Set<Object>) context.get(DecorateInstruction.CONTEXT_KEY),
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
        
        builder.addRule(new Input(Arrays.asList(mkTerm("x")), false, false), new Instructions(Arrays.asList((Instruction) deco1, (Instruction) deco2)));
        builder.addRule(new Input(Arrays.asList(mkTerm("a")), false, false), new Instructions(Arrays.asList((Instruction) deco3)));

        RulesCollection rules = builder.build();
        CommonRulesRewriter rewriter = new CommonRulesRewriter(rules);

        ExpandedQuery query = makeQuery("a x");
        Map<String, Object> context = new HashMap<>();
        rewriter.rewrite(query, context);

        
        
        assertThat(
                (Set<Object>) context.get(DecorateInstruction.CONTEXT_KEY),
                containsInAnyOrder((Object) "deco1", (Object) "deco2", (Object) "deco3")
                              
              );

        
        
    }
}
