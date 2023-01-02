package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;

import java.util.AbstractMap;
import java.util.Arrays;
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
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        decorate("deco1")
                )
        );

        ExpandedQuery query = makeQuery("a x");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        assertThat((Set<Object>)searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
              contains(
                      equalTo("deco1")
              ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDecorationForEmptyInput() {
        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("\"\""),
                        decorate("deco1")
                )
        );

        ExpandedQuery query = makeQuery("");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        assertThat((Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
              contains(
                      equalTo("deco1")
              ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatMultipleDecorationsAreEmitted() {

        final CommonRulesRewriter rewriter = rewriter(
                rule(
                        input("x"),
                        decorate("deco1"),
                        decorate("deco2")
                ),
                rule(
                        input("a"),
                        decorate("deco3")
                )
        );

        ExpandedQuery query = makeQuery("a x");
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        rewriter.rewrite(query, searchEngineRequestAdapter);

        assertThat(
                (Set<Object>) searchEngineRequestAdapter.getContext().get(DecorateInstruction.DECORATION_CONTEXT_KEY),
                containsInAnyOrder("deco1", "deco2", "deco3")
                              
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
