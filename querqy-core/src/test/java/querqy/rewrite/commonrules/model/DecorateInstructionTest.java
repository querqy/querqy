package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static querqy.model.builder.impl.BooleanQueryBuilder.bq;
import static querqy.rewrite.commonrules.select.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.AbstractCommonRulesTest;
import querqy.rewrite.commonrules.CommonRulesRewriter;
import querqy.rewrite.commonrules.LineParser;

public class DecorateInstructionTest extends AbstractCommonRulesTest {

    @Test
    public void testThatDecorationsWithSameKeyAreAllAddedToMap() {
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewrite(
                bq("a", "b", "c"),
                rewriter(
                        rule(input("a"), decorate("key1", "value1")),
                        rule(input("b"), decorate("key1", "value2")),
                        rule(input("c"), decorate("key1", "value2"))
                ),
                searchEngineRequestAdapter);

        Assertions.assertThat(getDecorationMap(searchEngineRequestAdapter))
                .containsOnly(entry("key1", "value1", "value2", "value2"));

    }

    @Test
    public void testThatTwoDecorationsWithDifferentKeysAreAddedToMap() {
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewrite(
                bq("a", "b"),
                rewriter(
                        rule(input("a"), decorate("key1", "value1")),
                        rule(input("b"), decorate("key2", "value2"))
                ),
                searchEngineRequestAdapter);

        Assertions.assertThat(getDecorationMap(searchEngineRequestAdapter))
                .containsOnly(
                        entry("key1", "value1"),
                        entry("key2", "value2")
                );

    }

    @Test
    public void testThatDecorationWithKeyIsAddedToMap() {
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewrite(
                bq("a"),
                rewriter(rule(input("a"), decorate("key1", "value1"))),
                searchEngineRequestAdapter);

        Assertions.assertThat(getDecorationMap(searchEngineRequestAdapter))
                .containsOnly(entry("key1", "value1"));

    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDecorationMap(SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return (Map<String, Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_MAP_KEY);
    }

    private AbstractMap.SimpleEntry<String, List<String>> entry(String key, String... value) {
        return new AbstractMap.SimpleEntry<>(key, Arrays.asList(value));
    }

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



        assertThat((Set<Object>)searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
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



        assertThat((Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
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
                (Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
                containsInAnyOrder((Object) "deco1", (Object) "deco2", (Object) "deco3")
                              
              );

    }

    @Test
    public void testHashCode() {

        DecorateInstruction deco1 = new DecorateInstruction("decoA");
        DecorateInstruction deco2 = new DecorateInstruction("decoA");
        DecorateInstruction deco3 = new DecorateInstruction("deco3");

        Assert.assertEquals(deco1.hashCode(), deco2.hashCode());
        Assert.assertNotEquals(deco1.hashCode(), deco3.hashCode());
        Assert.assertNotEquals(deco2.hashCode(), deco3.hashCode());

    }

    @Test
    public void testEquals() {

        DecorateInstruction deco1 = new DecorateInstruction("decoA");
        DecorateInstruction deco2 = new DecorateInstruction("decoA");
        DecorateInstruction deco3 = new DecorateInstruction("deco3");

        Assert.assertEquals(deco1, deco2);
        Assert.assertEquals(deco1.hashCode(), deco2.hashCode());
        Assert.assertEquals(deco1, deco1);
        Assert.assertNotEquals(deco1, deco3);
        Assert.assertNotEquals(deco2, deco3);
        Assert.assertNotEquals(deco2, null);
        Assert.assertNotEquals(deco2, new Object());

    }
}
