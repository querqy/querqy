package querqy.rewrite.lookup;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Term;
import querqy.rewrite.lookup.model.Sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class extracts sequences from a boolean query. Boolean clauses are taken as parts of a sequence while dmq
 * clauses are taken as alternatives. For the query bq(dmq(A), dmq(B, C)) the subsequences A, B, C, A B, and A C
 * are extracted.
 *
 * The class furthermore administers states per subsequence returned by a collector. If a collector returns a state
 * for a certain subsequence, the state is passed back to the collector for subsequent lookups. Given the collector
 * returns a state for subsequence A, this state will be passed for subsequent lookups for the sequences A B and A C.
 * @see StateExchangingCollector
 *
 */
public class StateExchangingSequenceExtractor<T> extends AbstractNodeVisitor<Void> {

    protected static final Term BOUNDARY_TERM = new Term(null, "\u0002");

    private final BooleanQuery booleanQuery;
    private final StateExchangingCollector<T, ?> stateExchangingCollector;
    private final LookupConfig lookupConfig;

    private List<Sequence<T>> previousSequences = List.of();
    private List<Sequence<T>> sequences = new ArrayList<>();

    private StateExchangingSequenceExtractor(
            final BooleanQuery booleanQuery,
            final StateExchangingCollector<T, ?> stateExchangingCollector,
            final LookupConfig lookupConfig
    ) {
        this.booleanQuery = booleanQuery;
        this.stateExchangingCollector = stateExchangingCollector;
        this.lookupConfig = lookupConfig;
    }

    public void extractSequences() {
        potentiallyEvaluateBoundaryTerm();
        visitBooleanQuery();
        potentiallyEvaluateBoundaryTerm();
    }

    private void potentiallyEvaluateBoundaryTerm() {
        if (lookupConfig.hasBoundaries()) {
            visit(BOUNDARY_TERM);
            refreshSequenceLists();
        }
    }

    private void visitBooleanQuery() {
        for (final BooleanClause clause : booleanQuery.getClauses()) {
            clause.accept(this);
            refreshSequenceLists();
        }
    }

    private void refreshSequenceLists() {
        if (!sequences.isEmpty()) {
            previousSequences = sequences;
            sequences = new ArrayList<>();

        } else if (!previousSequences.isEmpty()) {
            previousSequences = new ArrayList<>();
        }
    }

    @Override
    public Void visit(final BooleanQuery booleanQuery) {
        // TODO: return all full sequences from bq to enable cross-hierarchy lookups

        final StateExchangingSequenceExtractor<T> extractor = StateExchangingSequenceExtractor.<T>builder()
                .booleanQuery(booleanQuery)
                .stateExchangingCollector(stateExchangingCollector)
                .lookupConfig(lookupConfig)
                .build();

        extractor.extractSequences();

        return null;
    }

    @Override
    public Void visit(final Term term) {
        stateExchangingCollector.evaluateTerm(term).ifPresent(
                state -> sequences.add(Sequence.of(state, List.of(term)))
        );

        for (final Sequence<T> previousSequence : previousSequences) {
            stateExchangingCollector.evaluateNextTerm(previousSequence, term).ifPresent(
                    state -> sequences.add(
                            Sequence.of(state, concat(previousSequence.getTerms(), term))
                    )
            );
        }

        return null;
    }

    private List<Term> concat(final List<Term> terms, final Term term) {
        return Stream.concat(
                terms.stream(),
                Stream.of(term)
        ).collect(Collectors.toList());
    }

    public static <T> StateExchangingSequenceExtractorBuilder<T> builder() {
        return new StateExchangingSequenceExtractorBuilder<>();
    }

    public static class StateExchangingSequenceExtractorBuilder<T> {

        private BooleanQuery booleanQuery;
        private StateExchangingCollector<T, ?> stateExchangingCollector;
        private LookupConfig lookupConfig;

        public StateExchangingSequenceExtractorBuilder<T> booleanQuery(final BooleanQuery booleanQuery) {
            this.booleanQuery = booleanQuery;
            return this;
        }

        public StateExchangingSequenceExtractorBuilder<T> stateExchangingCollector(final StateExchangingCollector<T, ?> stateExchangingCollector) {
            this.stateExchangingCollector = stateExchangingCollector;
            return this;
        }

        public StateExchangingSequenceExtractorBuilder<T> lookupConfig(final LookupConfig lookupConfig) {
            this.lookupConfig = lookupConfig;
            return this;
        }

        public StateExchangingSequenceExtractor<T> build() {
            return new StateExchangingSequenceExtractor<>(booleanQuery, stateExchangingCollector, lookupConfig);
        }
    }
}
