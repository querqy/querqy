package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import querqy.CharSequenceUtil;
import querqy.LowerCaseCharSequence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * S. Langer. 1998. Zur Morphologie und Semantik von Nominalkomposita. Tagungsband der 4. Konferenz zur Verarbeitung
 *   natürlicher Sprache (KONVENS).
 */

public class MorphologicalWordBreaker implements LuceneWordBreaker {

    public static final float DEFAULT_WEIGHT_MORPHEME_STRATEGY = 0.8f;



    private final int minBreakLength;
    private final SuffixGroup suffixGroup;
    private final boolean lowerCaseInput;
    private final String dictionaryField;
    private final int minSuggestionFrequency;
    final float weightDfObservation;

    public MorphologicalWordBreaker(final Language language, final String dictionaryField, final boolean lowerCaseInput,
                                    final int minSuggestionFrequency, final int minBreakLength) {
        this(language, dictionaryField, lowerCaseInput, minSuggestionFrequency, minBreakLength,
                DEFAULT_WEIGHT_MORPHEME_STRATEGY);
    }

    public MorphologicalWordBreaker(final Language language, final String dictionaryField, final boolean lowerCaseInput,
                                    final int minSuggestionFrequency, final int minBreakLength,
                                    final float weightMorphemeStrategy) {

        this.minBreakLength = minBreakLength;
        this.lowerCaseInput = lowerCaseInput;
        this.dictionaryField = dictionaryField;
        this.minSuggestionFrequency = minSuggestionFrequency;

        weightDfObservation = 1f - weightMorphemeStrategy;

        suffixGroup = language.createMorphemes(weightMorphemeStrategy);

    }

    @Override
    public List<CharSequence[]> breakWord(final CharSequence word,
                                          final IndexReader indexReader,
                                          final int maxDecompoundExpansions,
                                          final boolean verifyCollation) {

        if (maxDecompoundExpansions < 1) {
            return Collections.emptyList();
        }

        final Collector collector = new Collector(minSuggestionFrequency, maxDecompoundExpansions, verifyCollation,
                indexReader, dictionaryField, weightDfObservation);

        collectSuggestions(word, indexReader, collector);

        return collector.flushResults();

    }


    protected void collectSuggestions(final CharSequence word, final IndexReader indexReader,
                                      final Collector collector) throws UncheckedIOException {
        final int termLength = Character.codePointCount(word, 0, word.length());
        if (termLength < minBreakLength) {
            return;
        }

        final CharSequence input = lowerCaseInput && (!(word instanceof LowerCaseCharSequence))
                ? new LowerCaseCharSequence(word) : word;


        // the original left term can be longer than rightOfs because the compounding might have removed characters
        // TODO: find min left size (based on linking morphemes and minBreakLength)
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {

            int splitIndex = Character.offsetByCodePoints(input, 0, leftLength);

            final CharSequence right = input.subSequence(splitIndex, input.length());
            final Term rightTerm = new Term(dictionaryField, new BytesRef(right));

            final int rightDf;
            try {
                rightDf = indexReader.docFreq(rightTerm);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            if (rightDf < minSuggestionFrequency) {
                continue;
            }

            final CharSequence left = input.subSequence(0, splitIndex);
            suffixGroup.collect(left, 0, right, rightTerm, rightDf, minBreakLength, collector);


        }
    }





    public static class BreakSuggestion implements Comparable<BreakSuggestion> {

        final CharSequence[] sequence;
        final float score;

        BreakSuggestion(final CharSequence[] sequence, final float score) {
            this.sequence = sequence;
            this.score = score;
        }


        @Override
        public int compareTo(final BreakSuggestion other) {

            if (other == this) {
                return 0;
            }
            int c = Float.compare(score, other.score); // greater is better
            if (c == 0) {
                c = Integer.compare(sequence.length, other.sequence.length); // shorter is better
                if (c == 0) {
                    for (int i = 0; i < sequence.length && c == 0; i++) {
                        c = CharSequenceUtil.compare(sequence[i], other.sequence[i]);
                    }
                }
            }

            return c;
        }

    }

}
