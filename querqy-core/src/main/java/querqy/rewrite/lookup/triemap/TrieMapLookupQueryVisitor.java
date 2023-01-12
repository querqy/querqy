package querqy.rewrite.lookup.triemap;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Term;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.model.Match;
import querqy.rewrite.lookup.triemap.model.TrieMapEvaluation;
import querqy.rewrite.lookup.triemap.model.TrieMapSequence;
import querqy.trie.States;

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
 *
 */
public class TrieMapLookupQueryVisitor<T> extends AbstractNodeVisitor<Void> {

    protected static final Term BOUNDARY_TERM = new Term(null, "\u0002");

    private final BooleanQuery booleanQuery;
    private final LookupConfig lookupConfig;

    private final TrieMapSequenceLookup<T> trieMapSequenceLookup;
    private final TrieMapMatchCollector<T> matchCollector;

    private List<TrieMapSequence<T>> previousSequences = List.of();
    private List<TrieMapSequence<T>> sequences = new ArrayList<>();

    TrieMapLookupQueryVisitor(
            final BooleanQuery booleanQuery,
            final LookupConfig lookupConfig,
            final TrieMapSequenceLookup<T> trieMapSequenceLookup,
            final TrieMapMatchCollector<T> matchCollector
    ) {
        this.booleanQuery = booleanQuery;
        this.lookupConfig = lookupConfig;
        this.trieMapSequenceLookup = trieMapSequenceLookup;
        this.matchCollector = matchCollector;
    }

    public List<Match<T>> lookupAndCollect() {
        lookup();
        return matchCollector.getMatches();
    }

    private void lookup() {
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

        final TrieMapLookupQueryVisitor<T> nestedTrieMapLookupQueryVisitor = new TrieMapLookupQueryVisitor<>(
                booleanQuery, lookupConfig, trieMapSequenceLookup, matchCollector
        );

        nestedTrieMapLookupQueryVisitor.lookup();

        return null;
    }

    @Override
    public Void visit(final Term term) {
        visitSingleTerm(term);

        if (!previousSequences.isEmpty()) {
            visitTermWithPreviousSequences(term);
        }
        return null;
    }

    private void visitSingleTerm(final Term term) {
        final States<T> states = trieMapSequenceLookup.evaluateTerm(term);

        if (isPartialMatch(states)) {
            sequences.add(TrieMapSequence.of(states, List.of(term)));
        }

        if (hasMatch(states)) {
            matchCollector.collect(TrieMapEvaluation.of(List.of(), term, states));
        }
    }

    private void visitTermWithPreviousSequences(final Term term) {
        for (final TrieMapSequence<T> previousSequence : previousSequences) {
            final States<T> states = trieMapSequenceLookup.evaluateNextTerm(previousSequence, term);

            if (isPartialMatch(states)) {
                sequences.add(TrieMapSequence.of(states, concat(previousSequence.getTerms(), term)));
            }

            if (hasMatch(states)) {
                matchCollector.collect(TrieMapEvaluation.of(previousSequence.getTerms(), term, states));
            }
        }
    }

    private boolean isPartialMatch(final States<T> states) {
        return states.getStateForCompleteSequence().isKnown();
    }

    private boolean hasMatch(final States<T> states) {
        return states.getStateForCompleteSequence().isFinal() || states.getPrefixes() != null;
    }

    private List<Term> concat(final List<Term> terms, final Term term) {
        return Stream.concat(
                terms.stream(),
                Stream.of(term)
        ).collect(Collectors.toList());
    }
}
