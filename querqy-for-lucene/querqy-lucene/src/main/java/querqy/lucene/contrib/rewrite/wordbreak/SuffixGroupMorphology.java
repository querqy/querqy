package querqy.lucene.contrib.rewrite.wordbreak;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


class Compound implements Comparable<Compound> {
    final CharSequence[] terms;
    final CharSequence compound;
    final float probability;

    public Compound(final CharSequence[] terms, final CharSequence compound, final float probability) {
        this.terms = terms;
        this.compound = compound;
        this.probability = probability;
    }


    @Override
    public int compareTo(final Compound other) {
        if (other == this) {
            return 0;
        }
        final int c = Float.compare(probability, other.probability); // greater is better
        if (c == 0) {
            return Integer.compare(compound.length(), other.compound.length()); // shorter is better
        }
        return c;
    }
}

class WordBreak {
    public final CharSequence originalLeft;
    public final CharSequence originalRight;
    public final List<Suggestion> suggestions;

    WordBreak(final CharSequence originalLeft, final CharSequence originalRight, final List<Suggestion> suggestions) {
        this.originalLeft = originalLeft;
        this.originalRight = originalRight;
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "WordBreak{" +
                "originalLeft=" + originalLeft +
                ", originalRight=" + originalRight +
                ", suggestions=" + suggestions +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WordBreak wordBreak = (WordBreak) o;
        return Objects.equals(originalLeft, wordBreak.originalLeft) && Objects.equals(originalRight, wordBreak.originalRight) && Objects.equals(suggestions, wordBreak.suggestions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalLeft, originalRight, suggestions);
    }
}

public class SuffixGroupMorphology implements Morphology {

    private final Function<Float, SuffixGroup> morphemeFactory;

    private final Function<Float, SuffixGroup> compoundingMorphemeFactory;


    SuffixGroupMorphology(final Function<Float, SuffixGroup> wordBreakMorphemeFactory,
                          final Function<Float, SuffixGroup> compoundingMorphemeFactory) {
        this.morphemeFactory = wordBreakMorphemeFactory;
        this.compoundingMorphemeFactory = compoundingMorphemeFactory;
    }

    SuffixGroupMorphology(final Function<Float, SuffixGroup> morphemeFactory) {
        this(morphemeFactory, morphemeFactory);
    }

    private SuffixGroup createMorphemes() {
        return morphemeFactory.apply(MorphologicalWordBreaker.DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }

    @Override
    public Compound[] suggestCompounds(final CharSequence left, final CharSequence right) {
        final SuffixGroup morphemes = compoundingMorphemeFactory.apply(MorphologicalWordBreaker.DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);

        return morphemes.generateCompoundSuggestions(left, right)
                .stream().distinct()
                .map(suggestion -> new Compound(new CharSequence[]{left, right},
                        suggestion.sequence[0],
                        suggestion.score)).toArray(Compound[]::new);
    }

    @Override
    public List<WordBreak> suggestWordBreaks(final CharSequence word, final int minBreakLength) {
        final SuffixGroup morphemes = createMorphemes();
        final int termLength = Character.codePointCount(word, 0, word.length());
        final List<WordBreak> wordBreaks = new ArrayList<>();
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {
            if (leftLength < minBreakLength || (termLength - leftLength) < minBreakLength) {
                //skip if right or left term is smaller than minBreakLength
                continue;
            }
            final int splitIndex = Character.offsetByCodePoints(word, 0, leftLength);
            final CharSequence right = word.subSequence(splitIndex, word.length());
            final CharSequence left = word.subSequence(0, splitIndex);
            final List<Suggestion> suggestions = morphemes.generateSuggestions(left).stream()
                    .filter(breakSuggestion -> breakSuggestion.sequence[0].length() >= minBreakLength)
                    .distinct()
                    .collect(Collectors.toList());
            wordBreaks.add(new WordBreak(left, right, suggestions));
        }

        return wordBreaks;
    }


}
