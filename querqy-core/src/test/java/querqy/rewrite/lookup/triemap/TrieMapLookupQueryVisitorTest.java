package querqy.rewrite.lookup.triemap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import querqy.model.BooleanQuery;
import querqy.model.Term;
import querqy.model.convert.builder.TermBuilder;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.triemap.model.TrieMapEvaluation;
import querqy.rewrite.lookup.triemap.model.TrieMapSequence;
import querqy.trie.State;
import querqy.trie.States;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static querqy.model.convert.builder.BooleanQueryBuilder.bq;
import static querqy.model.convert.builder.DisjunctionMaxQueryBuilder.dmq;
import static querqy.rewrite.lookup.triemap.TrieMapLookupQueryVisitor.BOUNDARY_TERM;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TrieMapLookupQueryVisitorTest {

    @Mock
    TrieMapSequenceLookup<String> trieMapSequenceLookup;
    @Mock
    TrieMapMatchCollector<String> trieMapMatchCollector;

    @Captor ArgumentCaptor<Term> termCaptor;
    @Captor ArgumentCaptor<Term> nextTermCaptor;
    @Captor ArgumentCaptor<TrieMapSequence<String>> previousSequenceCaptor;
    @Captor ArgumentCaptor<TrieMapEvaluation<String>> trieMapEvaluationCaptor;

    @Test
    public void testThat_sequenceLookupIsCalledOnce_forSingleTermQuery() {
        final BooleanQuery bq = bq("a").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(false, null));

        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup).evaluateTerm(termCaptor.capture());
        verify(trieMapSequenceLookup, never()).evaluateNextTerm(any(), any());
        assertThat(termCaptor.getValue()).isEqualTo(
                term("a")
        );
    }

    @Test
    public void testThat_valueIsCollected_forSingleTermQueryAndCompleteMatch() {
        final BooleanQuery bq = bq("a").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(true, "val"));

        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapMatchCollector).collect(trieMapEvaluationCaptor.capture());
        assertThat(trieMapEvaluationCaptor.getAllValues()).hasSize(1);
    }

    @Test
    public void testThat_prefixValuesAreCollected_forSingleTermQueryAndPrefixMatches() {
        final BooleanQuery bq = bq("a").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(
                states(state(false, null), prefixState("val", 1))
        );

        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapMatchCollector).collect(trieMapEvaluationCaptor.capture());
        assertThat(trieMapEvaluationCaptor.getAllValues()).hasSize(1);
    }

    @Test
    public void testThat_sequenceLookupIsCalledThreeTimes_forSingleTermQueryAndBoundaries() {
        final BooleanQuery bq = bq("a").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(false, null));
        createVisitor(bq, true).lookupAndCollect();

        verify(trieMapSequenceLookup, times(3)).evaluateTerm(termCaptor.capture());
        assertThat(termCaptor.getAllValues()).isEqualTo(
                List.of(
                        BOUNDARY_TERM,
                        term("a"),
                        BOUNDARY_TERM
                )
        );
    }

    @Test
    public void testThat_sequenceLookupIsCalledMultipleTimes_forEachTermQuery() {
        final BooleanQuery bq = bq("a", "b", "c").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(false, null));
        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup, times(3)).evaluateTerm(termCaptor.capture());
        assertThat(termCaptor.getAllValues()).isEqualTo(
                List.of(
                    term("a"),
                    term("b"),
                    term("c")
                )
        );
    }

    @Test
    public void testThat_sequenceLookupIsCalledWithSequence_forStateReturned() {
        final BooleanQuery bq = bq("a", "b").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(true, null));
        when(trieMapSequenceLookup.evaluateNextTerm(any(), any())).thenReturn(states(true, null));

        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup).evaluateNextTerm(previousSequenceCaptor.capture(), termCaptor.capture());

        assertThat(previousSequenceCaptor.getValue().getTerms()).isEqualTo(List.of(term("a")));
        assertThat(termCaptor.getValue()).isEqualTo(term("b"));
    }

    @Test
    public void testThat_sequenceLookupIsNotCalledWithSequence_forNoStateReturned() {
        final BooleanQuery bq = bq("a", "b").build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(false, null));
        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup, never()).evaluateNextTerm(any(), any());
    }

    @Test
    public void testThat_sequenceLookupIsCalledWithAllVariations_forTermsInDmq() {
        final BooleanQuery bq = bq(dmq("a", "b"), dmq("c", "d")).build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(true, null));
        when(trieMapSequenceLookup.evaluateNextTerm(any(), any())).thenReturn(states(true, null));
        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup, times(4)).evaluateTerm(termCaptor.capture());
        verify(trieMapSequenceLookup, times(4)).evaluateNextTerm(previousSequenceCaptor.capture(), nextTermCaptor.capture());

        assertThat(termCaptor.getAllValues()).containsExactly(
                term("a"),
                term("b"),
                term("c"),
                term("d")
        );

        final List<List<String>> seqs = zip(previousSequenceCaptor.getAllValues(), nextTermCaptor.getAllValues());

        assertThat(seqs).containsExactlyInAnyOrder(
                List.of("a", "c"),
                List.of("a", "d"),
                List.of("b", "c"),
                List.of("b", "d")
        );
    }


    @Test
    public void testThat_sequenceLookupIsCalledSeparately_forNestedBq() {
        final BooleanQuery bq = bq(dmq("a"), dmq(TermBuilder.term("b"), bq("c", "d"))).build();

        when(trieMapSequenceLookup.evaluateTerm(any())).thenReturn(states(true, null));
        when(trieMapSequenceLookup.evaluateNextTerm(any(), any())).thenReturn(states(true, null));
        createVisitor(bq, false).lookupAndCollect();

        verify(trieMapSequenceLookup, times(4)).evaluateTerm(termCaptor.capture());
        verify(trieMapSequenceLookup, times(2)).evaluateNextTerm(previousSequenceCaptor.capture(), nextTermCaptor.capture());

        assertThat(termCaptor.getAllValues()).containsExactly(
                term("a"),
                term("b"),
                term("c"),
                term("d")
        );

        final List<List<String>> seqs = zip(previousSequenceCaptor.getAllValues(), nextTermCaptor.getAllValues());

        assertThat(seqs).containsExactlyInAnyOrder(
                List.of("a", "b"),
                List.of("c", "d")
        );
    }

    private List<List<String>> zip(final List<TrieMapSequence<String>> seqs, final List<Term> terms) {
        final List<List<Term>> termSeqs = seqs.stream().map(TrieMapSequence::getTerms).collect(Collectors.toList());

        final List<List<String>> zippedTerms = new ArrayList<>();

        for (int i = 0; i < termSeqs.size(); i++) {
            zippedTerms.add(
                    Stream.concat(termSeqs.get(i).stream(), Stream.of(terms.get(i)))
                            .map(term -> term.getValue().toString())
                            .collect(Collectors.toList()));
        }

        return zippedTerms;
    }


    private TrieMapLookupQueryVisitor<String> createVisitor(
            final BooleanQuery booleanQuery,
            final boolean hasBoundaries
    ) {
        return new TrieMapLookupQueryVisitor<>(
                booleanQuery,
                LookupConfig.builder().hasBoundaries(hasBoundaries).build(),
                trieMapSequenceLookup,
                trieMapMatchCollector
        );
    }

    private State<String> prefixState(final String value, final int prefixIndex) {
        return new State<>(true, value, null, prefixIndex);
    }

    private static Term term(final String term) {
        return new Term(null, term);
    }

    @SafeVarargs
    private States<String> states(final State<String> complete, final State<String>... prefixes) {
        final States<String> states = new States<>(complete);
        Arrays.stream(prefixes).forEach(states::addPrefix);
        return states;
    }

    private States<String> states(final boolean isKnown, final String value) {
        return new States<>(state(isKnown, value));
    }

    private State<String> state(final boolean isKnown, final String value) {
        return new State<>(isKnown, value, null);
    }

}
