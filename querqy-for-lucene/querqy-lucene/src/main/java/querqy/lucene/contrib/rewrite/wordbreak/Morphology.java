package querqy.lucene.contrib.rewrite.wordbreak;


import java.util.HashMap;
import java.util.function.Function;

import static java.util.Collections.singletonList;


class Compound {
    CharSequence[] terms;
    CharSequence compound;
    float probability;
}

class WordBreak {
    CharSequence[] terms;
    CharSequence original;
    float probability;
}

interface IMorphology {
    Compound[] suggestCompounds(CharSequence left, CharSequence right);
    WordBreak[] suggestWordBreaks(CharSequence term);
}

public class Morphology implements IMorphology {

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
    public Compound[] suggestCompounds(CharSequence left, CharSequence right) {
        return new Compound[0];
    }

    @Override
    public WordBreak[] suggestWordBreaks(CharSequence term) {
        return new WordBreak[0];
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
