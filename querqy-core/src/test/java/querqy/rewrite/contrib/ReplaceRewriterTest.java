package querqy.rewrite.contrib;

import org.junit.Test;
import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static querqy.QuerqyMatchers.bq;
import static querqy.QuerqyMatchers.dmq;
import static querqy.QuerqyMatchers.term;

public class ReplaceRewriterTest {

    @Test
    public void testReplacementCaseSensitive() {
        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));
        trieMap.put("b c", getCharSeqs(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, false);

        ExpandedQuery expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        ExpandedQuery newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("A")),
                        dmq(term("b")),
                        dmq(term("C"))
                )
        );

        replaceRewriter = new ReplaceRewriter(trieMap, true);

        expandedQuery = getQuery(Arrays.asList("A", "b", "C"));
        newExpandedQuery = replaceRewriter.rewrite(expandedQuery);

        assertThat((Query) newExpandedQuery.getUserQuery(),
                bq(
                        dmq(term("d")),
                        dmq(term("e")),
                        dmq(term("C"))
                )
        );
    }

    @Test
    public void testReplacementOverlappingKeysIntersect() {

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));
        trieMap.put("b c", getCharSeqs(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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
    public void testReplacementOverlappingKeysSubset() {

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));
        trieMap.put("a b c", getCharSeqs(Collections.singletonList("f")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();
        trieMap.put("a b", getCharSeqs(Arrays.asList("d", "e")));

        ReplaceRewriter replaceRewriter = new ReplaceRewriter(trieMap, true);

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

    private List<ComparableCharSequence> getCharSeqs(List<String> strings) {
        return strings.stream().map(ComparableCharSequenceWrapper::new).collect(Collectors.toList());
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
