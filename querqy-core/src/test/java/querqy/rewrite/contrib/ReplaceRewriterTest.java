package querqy.rewrite.contrib;

import org.junit.Test;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.EmptySearchEngineRequestAdapter;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.replace.ReplaceInstruction;
import querqy.rewrite.contrib.replace.TermsReplaceInstruction;
import querqy.rewrite.contrib.replace.WildcardReplaceInstruction;
import querqy.trie.SequenceLookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class ReplaceRewriterTest {

    private List<CharSequence> list(String... seqs) {
        return new ArrayList<>(Arrays.asList(seqs));
    }


    @Test
    public void testEmptyQueryAfterSuffixAndPrefixRule() {
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putSuffix("suffix1", new WildcardReplaceInstruction(Collections.emptyList()));
        sequenceLookup.putPrefix("prefix1", new WildcardReplaceInstruction(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("suffix1", "prefix1"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testEmptyQueryAfterExactMatchRule() {
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c d"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("d e"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("g h"), getTermsReplaceInstruction(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);
        ExpandedQuery expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h")), new EmptySearchEngineRequestAdapter());
        assertThat((Query) expandedQuery.getUserQuery(),
                bq()
        );
    }

    @Test
    public void testRemoveTerms() {

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("h"), getTermsReplaceInstruction(Collections.singletonList("g")));
        sequenceLookup.put(tokenListFromString("i j"), getTermsReplaceInstruction(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery;
        expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")), new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b c"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("c d e"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("e f g"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("f g h"), getTermsReplaceInstruction(Collections.emptyList()));
        sequenceLookup.put(tokenListFromString("i"), getTermsReplaceInstruction(Collections.emptyList()));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery;
        expandedQuery = replaceRewriter.rewrite(getQuery(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")), new EmptySearchEngineRequestAdapter());

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
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(Collections.singletonList("abcde"), getTermsReplaceInstruction(Collections.singletonList("fghi")));
        sequenceLookup.putSuffix("hi", new WildcardReplaceInstruction(list("$1de")));
        sequenceLookup.putPrefix("fg", new WildcardReplaceInstruction(list("ab$1")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Collections.singletonList("abcde"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("abde"))
                )
        );
    }

    @Test
    public void testPrefixSuffixCaseSensitive() {
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>(false);
        sequenceLookup.putPrefix("abc", new WildcardReplaceInstruction(list("a$1")));
        sequenceLookup.putPrefix("DEF", new WildcardReplaceInstruction(list("d$1")));

        sequenceLookup.putSuffix("abc", new WildcardReplaceInstruction(list("$1a")));
        sequenceLookup.putSuffix("DEF", new WildcardReplaceInstruction(list("$1d")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList(
                "abcd", "ABCD", "defg", "DEFG",
                "dabc", "DABC", "gdef", "GDEF"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putSuffix("bc", new WildcardReplaceInstruction(list("$1ab")));
        sequenceLookup.putSuffix("bcd", new WildcardReplaceInstruction(list("$1abcd")));
        sequenceLookup.putSuffix("fg", new WildcardReplaceInstruction(list("$1")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("abc", "bcd", "abcdefg", "cde"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.putPrefix("ab", new WildcardReplaceInstruction(list("bc$1")));
        sequenceLookup.putPrefix("abc", new WildcardReplaceInstruction(list("bcde$1")));
        sequenceLookup.putPrefix("fg", new WildcardReplaceInstruction(list("$1")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("abc", "abd", "abcdef", "cde", "fghi"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a"), getTermsReplaceInstruction(Collections.singletonList("b")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Collections.singletonList("a"));

        expandedQuery.addBoostDownQuery(new BoostQuery(null, 1.0f));
        expandedQuery.addBoostUpQuery(new BoostQuery(null, 1.0f));
        expandedQuery.addMultiplicativeBoostQuery(new BoostQuery(null, 1.0f));
        expandedQuery.addFilterQuery(null);

        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

        assertEquals(1, newExpandedQuery.getBoostDownQueries().size());
        assertEquals(1, newExpandedQuery.getBoostUpQueries().size());
        assertEquals(1, newExpandedQuery.getMultiplicativeBoostQueries().size());
        assertEquals(1, newExpandedQuery.getFilterQueries().size());
    }

    @Test
    public void testReplacementCaseSensitive() {
        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>(false);
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "E")));
        sequenceLookup.put(tokenListFromString("b c"), getTermsReplaceInstruction(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("A")),
                        dmq(term("b")),
                        dmq(term("C"))
                )
        );

        sequenceLookup = new SequenceLookup<>(true);
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "E")));
        sequenceLookup.put(tokenListFromString("b c"), getTermsReplaceInstruction(Collections.singletonList("f")));
        replaceRewriter = new ReplaceRewriter(sequenceLookup);

        expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));
        sequenceLookup.put(tokenListFromString("b c"), getTermsReplaceInstruction(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b c d"), getTermsReplaceInstruction(Collections.singletonList("f")));
        sequenceLookup.put(tokenListFromString("b c e"), getTermsReplaceInstruction(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "c", "e", "f"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));
        sequenceLookup.put(tokenListFromString("a b c"), getTermsReplaceInstruction(Collections.singletonList("f")));
        sequenceLookup.put(tokenListFromString("b c"), getTermsReplaceInstruction(Collections.singletonList("g")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "a", "b", "e"));
        SearchEngineRequestAdapter searchEngineRequestAdapter = new EmptySearchEngineRequestAdapter();
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

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
        newExpandedQuery = replaceRewriter.rewrite(expandedQuery, searchEngineRequestAdapter);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("a")),
                        dmq(term("f"))
                )
        );
    }

    @Test
    public void testReplacementInTheMiddle() {

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "b", "b", "c"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("a", "a", "a", "b"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        Query query = new Query();
        addTerm(query, "a", false);
        addTerm(query, "a", true);
        addTerm(query, "b", false);
        addTerm(query, "c", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

        SequenceLookup<ReplaceInstruction> sequenceLookup = new SequenceLookup<>();
        sequenceLookup.put(tokenListFromString("a b"), getTermsReplaceInstruction(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(sequenceLookup);

        Query query = new Query();
        addTerm(query, "a", false);
        addTerm(query, "a", true);
        addTerm(query, "a", false);
        addTerm(query, "c", false);

        ExpandedQuery expandedQuery = new ExpandedQuery(query);
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery, new EmptySearchEngineRequestAdapter());

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

    private ReplaceInstruction getTermsReplaceInstruction(List<String> strings) {
        return new TermsReplaceInstruction(strings);
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
