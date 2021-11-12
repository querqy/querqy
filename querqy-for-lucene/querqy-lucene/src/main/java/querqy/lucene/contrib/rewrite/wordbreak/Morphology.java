package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.function.Function;

import static java.util.Collections.singletonList;

public enum Morphology {

    DEFAULT(weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NullWordGenerator.INSTANCE, 1f)))),
    GERMAN(GermanDecompoundingMorphology::createMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes);

    private final Function<Float, SuffixGroup> morphemeFactory;
    private final Function<Float, SuffixGroup> compoundingMorphemeFactory;

    Morphology(final Function<Float, SuffixGroup> morphemeFactory, final Function<Float, SuffixGroup> compoundingMorphemeFactory) {
        this.morphemeFactory = morphemeFactory;
        this.compoundingMorphemeFactory = compoundingMorphemeFactory;
    }

    Morphology(final Function<Float, SuffixGroup> morphemeFactory) {
        this(morphemeFactory, morphemeFactory);
    }

    public SuffixGroup createMorphemes(final float weightMorphologicalPattern) {
        return morphemeFactory.apply(weightMorphologicalPattern);
    }

    public SuffixGroup createCompoundingMorphemes(final float weightMorphologicalPattern) {
        return compoundingMorphemeFactory.apply(weightMorphologicalPattern);
    }
}
