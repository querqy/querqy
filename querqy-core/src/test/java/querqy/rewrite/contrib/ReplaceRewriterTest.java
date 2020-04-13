package querqy.rewrite.contrib;

import org.junit.Test;
import querqy.ComparableCharSequenceWrapper;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.trie.SequenceLookup;

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

    @Test
    public void testEmptyQueryAfterSuffixAndPrefixRule() {
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putSuffix("suffix1", "");
        sequenceLookup.putPrefix("prefix1", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("suffix1", "prefix1"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testEmptyQueryAfterExactMatchRule() {
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c d"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("d e"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("g h"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);
        ExpandedQuery expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h")));
        assertThat((Query) expandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testRemoveTerms() {

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("h"), getTermQueue(Collections.singletonList("g")));
        sequenceLookup.put(tokenListFromString("i j"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b c"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c d e"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f g"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("f g h"), getTermQueue(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("i"), getTermQueue(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putSuffix("cde", "dde");
        sequenceLookup.putPrefix("abd", "ab");
        sequenceLookup.put(Collections.singletonList("abde"), getTermQueue(Collections.singletonList("fghi")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>(false);
        sequenceLookup.putPrefix("abc", "a");
        sequenceLookup.putPrefix("DEF", "d");

        sequenceLookup.putSuffix("abc", "a");
        sequenceLookup.putSuffix("DEF", "d");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putSuffix("bc", "ab");
        sequenceLookup.putSuffix("bcd", "abcd");
        sequenceLookup.putSuffix("fg", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putPrefix("ab", "bc");
        sequenceLookup.putPrefix("abc", "bcde");
        sequenceLookup.putPrefix("fg", "");

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a"), getTermQueue(Collections.singletonList("b")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>(false);
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "E")));
        sequenceLookup.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("A")),
                        dmq(term("b")),
                        dmq(term("C"))
                )
        );

        sequenceLookup = new SequenceLookup<>(true);
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "E")));
        sequenceLookup.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));
        replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));
        sequenceLookup.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b c d"), getTermQueue(Collections.singletonList("f")));
        sequenceLookup.put(tokenListFromString("b c e"), getTermQueue(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));
        sequenceLookup.put(tokenListFromString("a b c"), getTermQueue(Collections.singletonList("f")));
        sequenceLookup.put(tokenListFromString("b c"), getTermQueue(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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

        SequenceLookup<CharSequence, Queue<CharSequence>> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermQueue(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

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
