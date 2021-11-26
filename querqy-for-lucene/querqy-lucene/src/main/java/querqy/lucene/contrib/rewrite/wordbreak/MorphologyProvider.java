package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.HashMap;

import static java.util.Collections.singletonList;

public class MorphologyProvider {
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
