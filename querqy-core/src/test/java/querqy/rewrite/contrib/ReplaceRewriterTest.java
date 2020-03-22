package querqy.rewrite.contrib;

import org.junit.Test;
import querqy.ComparableCharSequenceWrapper;
import querqy.CompoundCharSequence;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.trie.RuleExtractor;
import querqy.trie.State;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class ReplaceRewriterTest {

    // test suffix
    // test prefix
    // test combinations
    // empty input

    @Test
    public void testPrefixOnly() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
    }

    @Test
    public void testReplacementKeepBoostAndFilterQueries() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a"), getTermQueue(Collections.singletonList("b")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Collections.singletonList("a"));

        expandedQuery.addBoostDownQuery(new BoostQuery(null, 1.0f));
        expandedQuery.addBoostUpQuery(new BoostQuery(null, 1.0f));
        expandedQuery.addFilterQuery(null);

        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertEquals(1, newExpandedQuery.getBoostDownQueries().size());
        assertEquals(1, newExpandedQuery.getBoostUpQueries().size());
        assertEquals(1, newExpandedQuery.getFilterQueries().size());
    }

    @Test
    public void testReplacementCaseSensitive() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>(false);
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "E")));
        ruleExtractor.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("A")),
                        dmq(term("b")),
                        dmq(term("C"))
                )
        );

        ruleExtractor = new RuleExtractor<>(true);
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "E")));
        ruleExtractor.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));
        replaceRewriter = new ReplaceRewriter(ruleExtractor);

        expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("E")),
                        dmq(term("C"))
                )
        );
    }

    @Test
    public void testReplacementOverlappingKeysIntersect() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));
        ruleExtractor.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("c"))
                )
        );
    }

    @Test
    public void testReplacementOverlappingKeysTermsIntersect() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b c d"), getTermQueue(Collections.singletonList("f")));
        ruleExtractor.put(tokenListFromString("b c e"), getTermQueue(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "c", "e", "f"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("g")),
                        dmq(term("f"))
                )
        );
    }

    @Test
    public void testReplacementOverlappingKeysSubset() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));
        ruleExtractor.put(tokenListFromString("a b c"), getTermQueue(Collections.singletonList("f")));
        ruleExtractor.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "a", "b", "e"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("a")),
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("e"))
                )
        );

        expandedQuery = getQuery(Arrays.asList("a", "a", "b", "c"));
        newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("f"))
                )
        );
    }

    @Test
    public void testReplacementInTheMiddle() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("c"))
                )
        );
    }

    @Test
    public void testReplacementAtBeginning() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("b")),
                        dmq(term("c"))
                )
        );
    }

    @Test
    public void testReplacementAtEnd() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "a", "b"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("a")),
                        dmq(term("d")),
                        dmq(term("e"))
                )
        );
    }

    @Test
    public void testReplacementRemoveGeneratedIfMatch() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        Query query = new Query();
        addTerm(query, "a", false);
        addTerm(query, "a", true);
        addTerm(query, "b", false);
        addTerm(query, "c", false);


        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("c"))
                )
        );
    }

    @Test
    public void testReplacementKeepGeneratedIfUnmatched() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        Query query = new Query();
        addTerm(query, "a", false);
        addTerm(query, "a", true);
        addTerm(query, "a", false);
        addTerm(query, "c", false);


        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("a")),
                        dmq(term("a")),
                        dmq(term("c"))
                )
        );
    }

    private ExpandedQuery getQuery(List<String> tokens) {
        Query query = new Query();
        tokens.forEach(token -> addTerm(query, token));
        return new ExpandedQuery(query);
    }

    private List<CharSequence> tokenListFromString(String str) {
        return Arrays.stream(str.split(" ")).collect(Collectors.toList());
    }

    private Queue<CharSequence> getTermQueue(List<String> strings) {
        return strings.stream().map(ComparableCharSequenceWrapper::new).collect(Collectors.toCollection(LinkedList::new));
    }

    private void addTerm(Query query, String value) {
        addTerm(query, null, value);
    }

    private void addTerm(Query query, String field, String value) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value);
        dmq.addClause(term);
    }

    private void addTerm(Query query, String value, boolean isGenerated) {
        addTerm(query, null, value, isGenerated);
    }

    private void addTerm(Query query, String field, String value, boolean isGenerated) {
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(query, Clause.Occur.SHOULD, true);
        query.addClause(dmq);
        Term term = new Term(dmq, field, value, isGenerated);
        dmq.addClause(term);
    }

}
