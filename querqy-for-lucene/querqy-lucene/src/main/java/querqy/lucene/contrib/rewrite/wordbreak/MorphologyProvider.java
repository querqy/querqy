package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class MorphologyProvider {
    private static final Map<String, Optional<Morphology>> morphologies = new HashMap<>();
    private static final String DEFAULT_KEY = "default";
    public static final SuffixGroupMorphology DEFAULT = new SuffixGroupMorphology(weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, 1f))));

    static {
        morphologies.put(DEFAULT_KEY, Optional.of(DEFAULT));
        morphologies.put("german", Optional.of(new SuffixGroupMorphology(GermanDecompoundingMorphology::createDecompoundingMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes)));
    }

    public Optional<Morphology> get(final String name) {
        final String normName = name == null ? "" : name.toLowerCase();
        if (!exists(normName)) {
            throw new IllegalArgumentException(String.format("No such morphology %s", normName));
        }
        return morphologies.get(normName);
    }

    public boolean exists(final String name) {
        final String normName = name == null ? "" : name.toLowerCase();
        return morphologies.containsKey(normName);
    }
}
