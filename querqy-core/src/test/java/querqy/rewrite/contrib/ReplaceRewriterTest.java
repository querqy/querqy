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


    // what to do if all terms are removed?
    // finish
    @Test
    public void testEmptyQueryAfterSuffixAndPrefixRule() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("suffix1", "");
        ruleExtractor.putPrefix("prefix1", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("suffix1", "prefix1"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testEmptyQueryAfterExactMatchRule() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("c d"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("d e"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("e f"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("g h"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);
        ExpandedQuery expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h")));
        assertThat((Query) expandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testRemoveTerms() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("c"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("e f"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("h"), getTermQueue(Collections.singletonList("g")));
        ruleExtractor.put(tokenListFromString("i j"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery;
        expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("g")),
                        dmq(term("g"))
                )
        );
    }

    @Test
    public void testRemoveOverlappingTerms() {

        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.put(tokenListFromString("a b c"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("c d e"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("e f g"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("f g h"), getTermQueue(Collections.emptyList()));
        ruleExtractor.put(tokenListFromString("i"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery;
        expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));

        assertThat((Query) expandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("h")),
                        dmq(term("j"))
                )
        );
    }

    @Test
    public void testRuleCombinations() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("cde", "dde");
        ruleExtractor.putPrefix("abd", "ab");
        ruleExtractor.put(Collections.singletonList("abde"), getTermQueue(Collections.singletonList("fghi")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Collections.singletonList("abcde"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("fghi"))
                )
        );
    }

    @Test
    public void testPrefixSuffixCaseSensitive() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>(false);
        ruleExtractor.putPrefix("abc", "a");
        ruleExtractor.putPrefix("DEF", "d");

        ruleExtractor.putSuffix("abc", "a");
        ruleExtractor.putSuffix("DEF", "d");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList(
                "abcd", "ABCD", "defg", "DEFG",
                "dabc", "DABC", "gdef", "GDEF"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("ad")),
                        dmq(term("ABCD")),
                        dmq(term("defg")),
                        dmq(term("dG")),
                        dmq(term("da")),
                        dmq(term("DABC")),
                        dmq(term("gdef")),
                        dmq(term("Gd"))
                )
        );
    }

    @Test
    public void testSuffix() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putSuffix("bc", "ab");
        ruleExtractor.putSuffix("bcd", "abcd");
        ruleExtractor.putSuffix("fg", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("abc", "bcd", "abcdefg", "cde"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("aab")),
                        dmq(term("abcd")),
                        dmq(term("abcde")),
                        dmq(term("cde"))
                )
        );
    }

    @Test
    public void testPrefix() {
        RuleExtractor<CharSequence, Queue<CharSequence>> ruleExtractor = new RuleExtractor<>();
        ruleExtractor.putPrefix("ab", "bc");
        ruleExtractor.putPrefix("abc", "bcde");
        ruleExtractor.putPrefix("fg", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(ruleExtractor);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("abc", "abd", "abcdef", "cde", "fghi"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("bcde")),
                        dmq(term("bcd")),
                        dmq(term("bcdedef")),
                        dmq(term("cde")),
                        dmq(term("hi"))
                )
        );
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
