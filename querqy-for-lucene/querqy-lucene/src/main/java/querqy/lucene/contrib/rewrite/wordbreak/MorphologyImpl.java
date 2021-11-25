package querqy.lucene.contrib.rewrite.wordbreak;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;


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
    public final CharSequence originalRight;
    public final CharSequence originalLeft;
    public final List<BreakSuggestion> suggestions;

    WordBreak(final CharSequence originalLeft, final CharSequence originalRight, final List<BreakSuggestion> suggestions) {
        this.originalRight = originalRight;
        this.originalLeft = originalLeft;
        this.suggestions = suggestions;
    }
}

public class MorphologyImpl implements Morphology {

    public static MorphologyImpl DEFAULT = new MorphologyImpl("DEFAULT", weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, 1f))));
    public static MorphologyImpl GERMAN = new MorphologyImpl("GERMAN", GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes);

    private final Function<Float, SuffixGroup> morphemeFactory;

    private final String name;


    MorphologyImpl(final String name, final Function<Float, SuffixGroup> morphemeFactory, final Function<Float, SuffixGroup> compoundingMorphemeFactory) {
        this.morphemeFactory = morphemeFactory;
        this.name = name;
    }

    MorphologyImpl(final String name, final Function<Float, SuffixGroup> morphemeFactory) {
        this(name, morphemeFactory, morphemeFactory);
    }

    private SuffixGroup createMorphemes() {
        return morphemeFactory.apply(MorphologicalWordBreaker.DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
    }

    @Override
    public Compound[] suggestCompounds(final CharSequence left, final CharSequence right) {
        final Compound compound = new Compound(new CharSequence[]{left, right}, String.valueOf(left) + right, 0f);
        return new Compound[]{compound};
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
            final List<BreakSuggestion> breakSuggestions = morphemes.generateBreakSuggestions(left).stream()
                    .filter(breakSuggestion -> breakSuggestion.sequence[0].length() >= minBreakLength)
                    .collect(Collectors.toList());
            wordBreaks.add(new WordBreak(left, right, breakSuggestions));
        }

        return wordBreaks;
    }

    public String name() {
        return name;
    }

    public static class MorphologyProvider {
        private static final HashMap<String, MorphologyImpl> morphologies = new HashMap<>();
        private static final String DEFAULT_KEY = "DEFAULT";
        private static final MorphologyImpl DEFAULT = new MorphologyImpl(DEFAULT_KEY, weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, 1f))));

        static {
            morphologies.put(DEFAULT_KEY, DEFAULT);
            morphologies.put("GERMAN", new MorphologyImpl("GERMAN", GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes));
        }

        public MorphologyImpl get(final String name) {
            return morphologies.getOrDefault(name, DEFAULT);
        }

        public boolean exists(final String name) {
            return morphologies.containsKey(name);
        }
    }

}
