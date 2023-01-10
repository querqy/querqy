package querqy.rewrite.lookup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.model.Sequence;
import querqy.rewrite.lookup.preprocessing.Preprocessor;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TrieMapAutomatonWrapperTest {

    @Mock Preprocessor preprocessor;

    TrieMap<String> trieMap;
    TrieMapAutomatonWrapper<String> collector;

    @Before
    public void prepare() {
        trieMap = new TrieMap<>();
        collector = TrieMapAutomatonWrapper.<String>builder()
                .trieMap(trieMap)
                .lookupConfig(
                        LookupConfig.builder()
                                .ignoreCase(true)
                                .build()
                )
                .build();
    }

    @Test
    public void testThat_termIsCollected_forMatchingTrieMapEntry() {
        put("a", "val a");
        collector.evaluateTerm(expandableTerm("a"));

        assertThat(collector.getMatches()).isEqualTo(
                List.of(
                        match("a", "val a")
                )
        );
    }

    @Test
    public void testThat_termIsNotCollected_forNotMatchingTrieMapEntry() {
        put("a", "val a");
        collector.evaluateTerm(expandableTerm("b"));

        assertThat(collector.getMatches()).isEqualTo(List.of());
    }

    @Test
    public void testThat_termsAreCollected_forMatchingMultipleTerms() {
        put("a b", "val a b");

        Optional<State<String>> state = collector.evaluateTerm(expandableTerm("a"));
        assertThat(state).isPresent();

        Sequence<State<String>> sequence = Sequence.of(state.get(), List.of(expandableTerm("a")));
        collector.evaluateNextTerm(sequence, expandableTerm("b"));

        assertThat(collector.getMatches()).isEqualTo(
                List.of(
                        match("a b", "val a b")
                )
        );
    }

    @Test
    public void testThat_termsAreNotCollected_forMatchingOnlyPartialSequence() {
        put("a c", "val a not b");

        Optional<State<String>> state = collector.evaluateTerm(expandableTerm("a"));
        assertThat(state).isPresent();

        Sequence<State<String>> sequence = Sequence.of(state.get(), List.of(expandableTerm("a")));
        collector.evaluateNextTerm(sequence, expandableTerm("b"));

        assertThat(collector.getMatches()).isEqualTo(List.of());
    }

    @Test
    public void testThat_termIsNotButValueIsCollected_forMatchingTrieMapEntryButNotBeingExpandable() {
        put("a", "val a");
        collector.evaluateTerm(nonExpandableTerm("a"));

        assertThat(collector.getMatches()).isEqualTo(
                List.of(
                        match("val a")
                )
        );
    }

    @Test
    public void testThat_termIsPreprocessed_BeforePassingToTrieMap() {
        when(preprocessor.process(any())).thenReturn("b");

        put("b", "val b");

        collector = TrieMapAutomatonWrapper.<String>builder()
                .trieMap(trieMap)
                .lookupConfig(
                        LookupConfig.builder()
                                .ignoreCase(true)
                                .preprocessor(preprocessor)
                                .build()
                )
                .build();

        collector.evaluateTerm(expandableTerm("a"));
        assertThat(collector.getMatches()).isNotEmpty();
    }

    @Test
    public void testThat_onlyExpandableTermIsCollected_forMatchingExpandableAndNonExpandableTerm() {
        put("a b", "val a b");

        final Term nonExpandableTerm = nonExpandableTerm("a");

        Optional<State<String>> state = collector.evaluateTerm(nonExpandableTerm);
        assertThat(state).isPresent();

        Sequence<State<String>> sequence = Sequence.of(state.get(), List.of(nonExpandableTerm));
        collector.evaluateNextTerm(sequence, expandableTerm("b"));

        assertThat(collector.getMatches()).isEqualTo(
                List.of(
                        match("b", "val a b")
                )
        );
    }

    private static Term expandableTerm(final String term) {
        return new Term(new DisjunctionMaxQuery(null, null, false), term);
    }

    private static Term nonExpandableTerm(final String term) {
        return new Term(null, term);
    }

    private void put(final String... entries) {
        assert entries.length % 2 == 0;
        for (int i = 0; i < entries.length; i = i + 2) {
            trieMap.put(entries[i], entries[i + 1]);
        }
    }

    private Match<String> match(final String value) {
        return match(null, value);
    }

    private Match<String> match(final String termMatches, final String value) {
        return Match.of(
                termMatches == null ? new TermMatches() :
                        Arrays.stream(termMatches.split(" "))
                                .map(term -> new TermMatch(new Term(null, term)))
                                .collect(Collectors.toCollection(TermMatches::new)),
                value
        );
    }

}
