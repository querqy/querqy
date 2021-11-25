package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import querqy.LowerCaseCharSequence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

public class MorphologicalWordBreaker implements LuceneWordBreaker {

    public static final float DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN = 0.8f;
    private final int minBreakLength;
    private final int maxEvaluations;
    private final boolean lowerCaseInput;
    private final String dictionaryField;
    private final int minSuggestionFrequency;
    final float weightDfObservation;
    private final Morphology morphology;

    public MorphologicalWordBreaker(final Morphology morphology, final String dictionaryField,
                                    final boolean lowerCaseInput, final int minSuggestionFrequency,
                                    final int minBreakLength, final int maxEvaluations) {
        this(morphology, dictionaryField, lowerCaseInput, minSuggestionFrequency, minBreakLength, maxEvaluations,
                DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }

    public MorphologicalWordBreaker(final Morphology morphology, final String dictionaryField,
                                    final boolean lowerCaseInput, final int minSuggestionFrequency,
                                    final int minBreakLength, final int maxEvaluations,
                                    final float weightMorphologicalPattern) {

        this.minBreakLength = minBreakLength;
        this.maxEvaluations = maxEvaluations;
        this.lowerCaseInput = lowerCaseInput;
        this.dictionaryField = dictionaryField;
        this.minSuggestionFrequency = minSuggestionFrequency;

        weightDfObservation = 1f - weightMorphologicalPattern;

        this.morphology = morphology;

    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word,
                                          final IndexReader indexReader,
                                          final int maxDecompoundExpansions,
                                          final boolean verifyCollation) {

        if (maxDecompoundExpansions < 1) {
            return Collections.emptyList();
        }

        final Collector collector = new Collector(minSuggestionFrequency, maxDecompoundExpansions, maxEvaluations,
                verifyCollation, indexReader, dictionaryField, weightDfObservation);

        collectSuggestions(word, indexReader, collector);

        return collector.flushResults();

    }


    private void collectSuggestions(final CharSequence word, final IndexReader indexReader,
                                    final Collector collector) throws UncheckedIOException {
        final int termLength = Character.codePointCount(word, 0, word.length());
        if (termLength < minBreakLength) {
            return;
        }

        final CharSequence input = lowerCaseInput && (!(word instanceof LowerCaseCharSequence))
                ? new LowerCaseCharSequence(word) : word;


        // the original left term can be longer than rightOfs because the compounding might have removed characters
        // TODO: find min left size (based on linking morphemes and minBreakLength)
        // Generation of suggestions happens here -
        final List<WordBreak> suggestedWordBreaks = morphology.suggestWordBreaks(input, minBreakLength);

        for (final WordBreak suggestedWordBreak : suggestedWordBreaks) {
            //iterate through break suggest
            final Term rightTerm = new Term(dictionaryField, new BytesRef(suggestedWordBreak.originalRight));
            final int rightDf;
            try {
                rightDf = indexReader.docFreq(rightTerm);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            if (rightDf < minSuggestionFrequency) {
                continue;
            }

            for (final BreakSuggestion suggestion : suggestedWordBreak.suggestions) {
                final Collector.CollectionState collectionState = collector.collect(
                        suggestion.sequence[0],
                        suggestedWordBreak.originalRight,
                        rightTerm,
                        rightDf,
                        suggestion.score);
                if (collectionState.isMaxEvaluationsReached()) {
                    break;
                }
            }
        }
    }


}
