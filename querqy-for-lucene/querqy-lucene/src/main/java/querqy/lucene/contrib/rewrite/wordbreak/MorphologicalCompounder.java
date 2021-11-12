package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import querqy.model.Term;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static querqy.lucene.LuceneQueryUtil.toLuceneTerm;

public class MorphologicalCompounder implements LuceneCompounder {

    public static final float DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN = 0.8f;
    private final SuffixGroup suffixGroup;
    private final String dictionaryField;
    private final int minSuggestionFrequency;

    public MorphologicalCompounder(final Morphology morphology, final String dictionaryField, final boolean lowercaseInput, final int minSuggestionFrequency) {
        this(morphology::createCompoundingMorphemes, dictionaryField, lowercaseInput, minSuggestionFrequency, DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }


    // Visible for testing
    public MorphologicalCompounder(final Function<Float, SuffixGroup> morphology, final String dictionaryField, final boolean lowercaseInput, final int minSuggestionFrequency, float weightMorphologicalPattern) {
        this.dictionaryField = dictionaryField;
        this.minSuggestionFrequency = minSuggestionFrequency;
        suffixGroup = morphology.apply(weightMorphologicalPattern);
    }

    @Override
    public List<CompoundTerm> combine(final Term[] terms, final IndexReader indexReader, final boolean reverse) {
        if (terms.length < 2) {
            return Collections.emptyList();
        }
        final Term left = terms[0];
        final Term right = terms[1];
        final int maxCompoundExpansions = 10; // move to constructor
        final int queueInitialCapacity = Math.min(maxCompoundExpansions, 10);
        final Queue<MorphologicalWordBreaker.BreakSuggestion> collector = new PriorityQueue<>(queueInitialCapacity);
        // do we care about the collector in this overload or change signature to turn list?
        suffixGroup.collect(left, right, collector);

        return collector.stream().map(suggestion -> {
            final CharSequence modifier = suggestion.sequence[0];
            final CharSequence base = suggestion.sequence[1];
            final CharSequence modifierBaseCompound = new StringBuilder().append(modifier).append(base);
            return new CompoundTerm(modifierBaseCompound, new Term[]{left, right});
        }).filter(compound -> {
            // Should this check be configurable?
            final org.apache.lucene.index.Term compoundTerm = toLuceneTerm(dictionaryField, compound.value, false);
            final int compoundDf;
            try {
                compoundDf = indexReader.docFreq(compoundTerm);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return (compoundDf >= minSuggestionFrequency);
        }).collect(Collectors.toList());
    }
}
