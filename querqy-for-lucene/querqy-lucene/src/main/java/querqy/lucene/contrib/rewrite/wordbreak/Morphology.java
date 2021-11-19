package querqy.lucene.contrib.rewrite.wordbreak;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.replaceAll;
import static java.util.Collections.singletonList;


class Compound {
    CharSequence[] terms;
    CharSequence compound;
    float probability;
}

class WordBreak {
    public final CharSequence originalRight;
    public final CharSequence originalLeft;
    public final List<MorphologicalWordBreaker.BreakSuggestion> suggestions;
    CharSequence originalTerm;
    List<MorphologicalWordBreaker.BreakSuggestion> breakSuggestions = new ArrayList<>();

    WordBreak(final CharSequence originalLeft, final CharSequence originalRight, final List<MorphologicalWordBreaker.BreakSuggestion> suggestions) {
        this.originalRight = originalRight;
        this.originalLeft = originalLeft;
        this.suggestions = suggestions;
    }
}

interface IMorphology {
    Compound[] suggestCompounds(CharSequence left, CharSequence right);

    List<WordBreak> suggestWordBreaks(CharSequence word, int minBreakLength);
}

public class Morphology implements IMorphology {
    public static final float DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN = 0.8f;

//    DEFAULT(weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NullWordGenerator.INSTANCE, 1f)))),
//    GERMAN(GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes);

    public static Morphology DEFAULT = new Morphology("DEFAULT", weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NullWordGenerator.INSTANCE, 1f))));
    public static Morphology GERMAN = new Morphology("GERMAN", GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes);

    private final Function<Float, SuffixGroup> morphemeFactory;
    private final Function<Float, SuffixGroup> compoundingMorphemeFactory;

    private final String name;


    Morphology(final String name, final Function<Float, SuffixGroup> morphemeFactory, final Function<Float, SuffixGroup> compoundingMorphemeFactory) {
        this.morphemeFactory = morphemeFactory;
        this.compoundingMorphemeFactory = compoundingMorphemeFactory;
        this.name = name;
    }

    Morphology(final String name, final Function<Float, SuffixGroup> morphemeFactory) {
        this(name, morphemeFactory, morphemeFactory);
    }

    public SuffixGroup createMorphemes(final float weightMorphologicalPattern) {
        return morphemeFactory.apply(weightMorphologicalPattern);
    }

    public SuffixGroup createCompoundingMorphemes(final float weightMorphologicalPattern) {
        return compoundingMorphemeFactory.apply(weightMorphologicalPattern);
    }

    @Override
    public Compound[] suggestCompounds(final CharSequence left, final CharSequence right) {
        return new Compound[0];
    }

    @Override
    public List<WordBreak> suggestWordBreaks(final CharSequence word, final int minBreakLength) {
        final SuffixGroup morphemes = createMorphemes(DEFAULT_WEIGHT_MORPHOLOGICAL_PATTERN);
        final int termLength = Character.codePointCount(word, 0, word.length());
        final List<WordBreak> wordBreaks = new ArrayList<>();
        for (int leftLength = termLength - minBreakLength; leftLength > 0; leftLength--) {
            final int splitIndex = Character.offsetByCodePoints(word, 0, leftLength);
            final CharSequence right = word.subSequence(splitIndex, word.length());
            final CharSequence left = word.subSequence(0, splitIndex);
            final List<MorphologicalWordBreaker.BreakSuggestion> breakSuggestions = morphemes.collect2(left, 0);
            wordBreaks.add(new WordBreak(left, right, breakSuggestions));
        }

        return wordBreaks;
    }

    public String name() {
        return name;
    }

    public static class MorphologyProvider {
        private static HashMap<String, Morphology> morphologies = new HashMap<>();
        private static String DEFAULT_KEY = "DEFAULT";
        private static Morphology DEFAULT = new Morphology(DEFAULT_KEY, weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NullWordGenerator.INSTANCE, 1f))));

        static {
            morphologies.put(DEFAULT_KEY, DEFAULT);
            morphologies.put("GERMAN", new Morphology("GERMAN", GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes));
        }

        public Morphology get(final String name) {
            return morphologies.getOrDefault(name, DEFAULT);
        }

        public boolean exists(final String name) {
            return morphologies.containsKey(name);
        }
    }

}
