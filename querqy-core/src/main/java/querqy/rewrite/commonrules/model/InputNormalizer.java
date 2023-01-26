package querqy.rewrite.commonrules.model;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.SimpleComparableCharSequence;
import querqy.model.Input;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static querqy.rewrite.commonrules.model.TrieMapRulesCollection.*;

public class InputNormalizer {

    private static final List<CharSequence> SPACE = Collections.singletonList(
            new CompoundCharSequence(" ", BOUNDARY_WORD, BOUNDARY_WORD));

    private static final LookupPreprocessor LOWER_CASE_PREPROCESSOR = LookupPreprocessorFactory.lowercase();

    private final LookupPreprocessor lookupPreprocessor;
    private final boolean isIdentityPreprocessor;

    public InputNormalizer(final LookupPreprocessor lookupPreprocessor) {
        this.lookupPreprocessor = lookupPreprocessor;
        isIdentityPreprocessor = lookupPreprocessor == LookupPreprocessorFactory.identity();
    }

    public List<CharSequence> getNormalizedInputSequences(final Input.SimpleInput input) {
        final List<Term> inputTerms = input.getInputTerms();
        if (inputTerms.isEmpty()) {
            return spaceInput(input.isRequiresLeftBoundary(), input.isRequiresRightBoundary());
        }

        final List<CharSequence> seqs = inputTerms.size() == 1
                ? getTermCharSequences(inputTerms.get(0))
                : getTermCharSequences(inputTerms);

        return seqs.stream()
                .map(seq -> applyBoundaries(seq, input.isRequiresLeftBoundary(), input.isRequiresRightBoundary()))
                .collect(Collectors.toList());

    }

    protected CharSequence applyBoundaries(final CharSequence seq, final boolean requiresLeftBoundary,
                                           final boolean requiresRightBoundary) {
        if (requiresLeftBoundary == requiresRightBoundary) {
            if (requiresLeftBoundary) {
                return new CompoundCharSequence(" ", BOUNDARY_WORD, seq, BOUNDARY_WORD);
            } else {
                return seq;
            }
        } else if (requiresLeftBoundary) {
            return new CompoundCharSequence(" ", BOUNDARY_WORD, seq);
        } else {
            return new CompoundCharSequence(" ", seq, BOUNDARY_WORD);
        }
    }

    protected List<CharSequence> spaceInput(final boolean isLeftBoundaryRequired,
                                                      final boolean isRightBoundaryRequired) {
        if (!(isLeftBoundaryRequired && isRightBoundaryRequired)) {
            throw new IllegalArgumentException("Empty input!");
        }

        return SPACE;
    }

    protected List<CharSequence> getTermCharSequences(final Term term) {

        final CharSequence value = term instanceof PrefixTerm ? getPrefixCharSequence((PrefixTerm) term)
                : lookupPreprocessor.process(term);

        if (!term.hasFieldNames()) {
            return Collections.singletonList(value);
        }

        return term.getFieldNames().stream().map(name -> new CompoundCharSequence(Term.FIELD_CHAR, name, value))
                .collect(Collectors.toList());

    }

    protected List<CharSequence> getTermCharSequences(final List<Term> terms) {

        LinkedList<List<CharSequence>> slots = new LinkedList<>();

        for (final Term inputTerm : terms) {
            slots.add(getTermCharSequences(inputTerm));
        }

        final List<CharSequence> seqs = new LinkedList<>();
        collectTails(new LinkedList<>(), slots, seqs);

        return seqs;

    }

    void collectTails(final List<CharSequence> prefix, List<List<CharSequence>> tailSlots,
                      final List<CharSequence> result) {
        if (tailSlots.size() == 1) {
            for (final CharSequence sequence : tailSlots.get(0)) {
                final List<CharSequence> combined = new LinkedList<>(prefix);
                combined.add(sequence);
                result.add(new CompoundCharSequence(" ", combined));
            }
        } else {

            final List<List<CharSequence>> newTail = tailSlots.subList(1, tailSlots.size());
            for (final CharSequence sequence : tailSlots.get(0)) {
                final List<CharSequence> newPrefix = new LinkedList<>(prefix);
                newPrefix.add(sequence);
                collectTails(newPrefix, newTail, result);
            }
        }
    }


    /**
     * Get the char sequence for a {@link PrefixTerm}. Returns the sequence verbatim if we are using an identity
     * LookupPreprocessor and the lower-cased sequence otherwise. This means that stemming etc. will not be applied to
     * a prefix term.
     *
     * @param term The prefix term to process.
     * @return The char sequence for the prefix term
     */

    protected CharSequence getPrefixCharSequence(final PrefixTerm term) {
        return isIdentityPreprocessor ? term : LOWER_CASE_PREPROCESSOR.process(term);
    }


}
