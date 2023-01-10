package querqy.rewrite.lookup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import querqy.model.BooleanQuery;
import querqy.model.Term;
import querqy.model.convert.builder.TermBuilder;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.model.Sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import static querqy.rewrite.lookup.AutomatonSequenceExtractor.BOUNDARY_TERM;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AutomatonSequenceExtractorTest {

    @Mock
    AutomatonWrapper<String, String> collector;
    @Captor ArgumentCaptor<Term> termCaptor;
    @Captor ArgumentCaptor<Sequence<String>> sequenceCaptor;

    @Test
    public void testThat_collectorIsCalledOnce_forSingleTermQuery() {
        final BooleanQuery bq = bq("a").build();

        createExtractor(bq, collector).extractSequences();

        verify(collector).evaluateTerm(termCaptor.capture());
        verify(collector, never()).evaluateNextTerm(any(), any());
        assertThat(termCaptor.getValue()).isEqualTo(
                term("a")
        );
    }

    @Test
    public void testThat_collectorIsCalledThreeTimes_forSingleTermQueryAndBoundaries() {
        final BooleanQuery bq = bq("a").build();

        createExtractor(bq, collector, true).extractSequences();

        verify(collector, times(3)).evaluateTerm(termCaptor.capture());
        assertThat(termCaptor.getAllValues()).isEqualTo(
                List.of(
                        BOUNDARY_TERM,
                        term("a"),
                        BOUNDARY_TERM
                )
        );
    }

    @Test
    public void testThat_collectorIsCalledMultipleTimes_forEachTermQuery() {
        final BooleanQuery bq = bq("a", "b", "c").build();

        createExtractor(bq, collector).extractSequences();

        verify(collector, times(3)).evaluateTerm(termCaptor.capture());
        assertThat(termCaptor.getAllValues()).isEqualTo(
                List.of(
                    term("a"),
                    term("b"),
                    term("c")
                )
        );
    }

    @Test
    public void testThat_collectorIsCalledWithSequence_forStateReturned() {
        final BooleanQuery bq = bq("a", "b").build();

        when(collector.evaluateTerm(term("a"))).thenReturn(Optional.of("state"));
        createExtractor(bq, collector).extractSequences();

        verify(collector).evaluateNextTerm(sequenceCaptor.capture(), termCaptor.capture());

        assertThat(sequenceCaptor.getValue()).isEqualTo(Sequence.of("state", List.of(term("a"))));
        assertThat(termCaptor.getValue()).isEqualTo(term("b"));
    }

    @Test
    public void testThat_collectorIsNotCalledWithSequence_forNoStateReturned() {
        final BooleanQuery bq = bq("a", "b").build();

        when(collector.evaluateTerm(term("a"))).thenReturn(Optional.empty());
        createExtractor(bq, collector).extractSequences();

        verify(collector, never()).evaluateNextTerm(any(), any());
    }

    @Test
    public void testThat_collectorIsCalledWithAllVariations_forTermsInDmq() {
        final BooleanQuery bq = bq(dmq("a", "b"), dmq("c", "d")).build();

        final TestAutomatonWrapper collector = testCollector();
        createExtractor(bq, collector).extractSequences();

        assertThat(collector.getCalls()).containsExactlyInAnyOrderElementsOf(
                List.of(
                        "a", "b", "c", "d",
                        "a c", "a d", "b c", "b d"
                )
        );
    }

    @Test
    public void testThat_collectorIsCalledSeparately_forNestedBq() {
        final BooleanQuery bq = bq(dmq("a"), dmq(TermBuilder.term("b"), bq("c", "d"))).build();

        final TestAutomatonWrapper collector = testCollector();
        createExtractor(bq, collector).extractSequences();

        assertThat(collector.getCalls()).containsExactlyInAnyOrderElementsOf(
                List.of(
                        "a", "b", "a b",
                        "c", "d", "c d"
                )
        );
    }

    private AutomatonSequenceExtractor<String> createExtractor(
            final BooleanQuery booleanQuery,
            final AutomatonWrapper<String, String> collector
    ) {
        return createExtractor(booleanQuery, collector, false);
    }

    private AutomatonSequenceExtractor<String> createExtractor(
            final BooleanQuery booleanQuery,
            final AutomatonWrapper<String, String> collector,
            final boolean hasBoundaries
    ) {
        return AutomatonSequenceExtractor.<String>builder()
                .booleanQuery(booleanQuery)
                .stateExchangingCollector(collector)
                .lookupConfig(LookupConfig.builder().hasBoundaries(hasBoundaries).build())
                .build();
    }

    private static TestAutomatonWrapper testCollector() {
        return new TestAutomatonWrapper();
    }

    private static class TestAutomatonWrapper implements AutomatonWrapper<String, String> {
        private final List<String> calls = new ArrayList<>();

        @Override
        public Optional<String> evaluateTerm(final Term term) {
            calls.add(term.getValue().toString());
            return Optional.of("");
        }

        @Override
        public Optional<String> evaluateNextTerm(final Sequence<String> sequence, final Term term) {
            calls.add(
                    Stream.concat(sequence.getTerms().stream(), Stream.of(term))
                            .map(elem -> elem.getValue().toString()).collect(Collectors.joining(" "))
            );
            return Optional.of("");
        }

        @Override
        public List<Match<String>> getMatches() {
            return null;
        }

        public List<String> getCalls() {
            return calls;
        }
    }

    private static Term term(final String term) {
        return new Term(null, term);
    }
}
