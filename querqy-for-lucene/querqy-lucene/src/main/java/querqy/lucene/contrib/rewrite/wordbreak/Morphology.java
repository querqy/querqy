package querqy.lucene.contrib.rewrite.wordbreak;

import static java.util.Collections.singletonList;

import java.util.function.Function;

public enum Morphology {

    DEFAULT(weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NullWordGenerator.INSTANCE, 1f)))),
    GERMAN(GermanDecompoundingMorphology::createMorphemes);

    private final Function<Float, SuffixGroup> morphemeFactory;

    Morphology(final Function<Float, SuffixGroup> morphemeFactory) {
        this.morphemeFactory = morphemeFactory;
    }

    public SuffixGroup createMorphemes(final float weightMorphologicalPattern) {
        return morphemeFactory.apply(weightMorphologicalPattern);
    }

}
