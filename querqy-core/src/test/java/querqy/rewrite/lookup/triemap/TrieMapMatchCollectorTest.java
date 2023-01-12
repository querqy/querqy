package querqy.rewrite.lookup.triemap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.TermMatch;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.triemap.model.TrieMapEvaluation;
import querqy.trie.State;
import querqy.trie.States;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TrieMapMatchCollectorTest {

    TrieMapMatchCollector<String> trieMapMatchCollector;

    @Mock DisjunctionMaxQuery disjunctionMaxQuery;

    @Before
    public void prepare() {
        trieMapMatchCollector = new TrieMapMatchCollector<>();
    }

    @Test
    public void testThat_matchIsCollectedWithSingleTermMatch_forGivenCompleteMatchAndSingleTerm() {
        final TrieMapEvaluation<String> evaluation = evaluation(states("value"), "term");
        trieMapMatchCollector.collect(evaluation);

        assertThat(trieMapMatchCollector.getMatches()).isEqualTo(
                List.of(match("value", "term"))
        );
    }

    @Test
    public void testThat_matchIsCollectedWithoutTermMatch_forGivenCompleteMatchAndSingleTermWithoutParent() {
        final TrieMapEvaluation<String> evaluation = evaluation(states("value"), term(null, "term"));
        trieMapMatchCollector.collect(evaluation);

        assertThat(trieMapMatchCollector.getMatches()).isEqualTo(
                List.of(match("value", "term"))
        );
    }

    @Test
    public void testThat_matchIsCollectedWithMultipleTermMatches_forGivenCompleteMatchAndMultipleTerms() {
        final TrieMapEvaluation<String> evaluation = evaluation(states("value"), "term1", "term2");
        trieMapMatchCollector.collect(evaluation);

        assertThat(trieMapMatchCollector.getMatches()).isEqualTo(
                List.of(match("value", "term1", "term2"))
        );
    }

    @Test
    public void testThat_matchesAreCollectedWithPrefixTermMatch_forGivenPrefixMatchesAndSingleTerm() {
        final TrieMapEvaluation<String> evaluation = evaluation(
                states(state(null), prefixState("value", 2)),"term"
        );

        trieMapMatchCollector.collect(evaluation);

        assertThat(trieMapMatchCollector.getMatches()).isEqualTo(
                List.of(match("value", prefixTermMatch("term", 2)))
        );
    }

    public Match<String> match(final String value, final String... termMatches) {
        return Match.of(
                Arrays.stream(termMatches).map(this::termMatch).collect(Collectors.toCollection(TermMatches::new)),
                value
        );
    }

    public Match<String> match(final String value, final TermMatch... termMatches) {
        return Match.of(
                Arrays.stream(termMatches).collect(Collectors.toCollection(TermMatches::new)),
                value
        );
    }

    public TrieMapEvaluation<String> evaluation(final States<String> states, final String... terms) {
        return evaluation(
                states, Arrays.stream(terms).map(this::term).toArray(Term[]::new)
        );
    }

    public TrieMapEvaluation<String> evaluation(final States<String> states, final Term... terms) {
        return TrieMapEvaluation.of(
                Arrays.asList(terms).subList(0, terms.length - 1),
                Arrays.asList(terms).get(terms.length - 1),
                states
        );
    }

    private TermMatch prefixTermMatch(final String term, final int prefixIndex) {
        return new TermMatch(term(term), true, prefixIndex);
    }

    private TermMatch termMatch(final String term) {
        return new TermMatch(term(term));
    }

    private Term term(final String term) {
        return new Term(disjunctionMaxQuery, term);
    }

    private Term term(final DisjunctionMaxQuery parent, final String term) {
        return new Term(disjunctionMaxQuery, term);
    }


    @SafeVarargs
    private States<String> states(final State<String> complete, final State<String>... prefixes) {
        final States<String> states = new States<>(complete);
        Arrays.stream(prefixes).forEach(states::addPrefix);
        return states;
    }

    private States<String> states(final String value) {
        return new States<>(state(value));
    }

    private State<String> state(final String value) {
        return new State<>(true, value, null);
    }

    private State<String> prefixState(final String value, final int prefixIndex) {
        return new State<>(true, value, null, prefixIndex);
    }

}
