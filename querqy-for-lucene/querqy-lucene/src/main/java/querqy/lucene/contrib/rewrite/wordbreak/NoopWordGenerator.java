package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Optional;

public class NoopWordGenerator implements WordGenerator {

    public static NoopWordGenerator INSTANCE = new NoopWordGenerator();

    private NoopWordGenerator() {
        // Use the singleton instance!
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        return Optional.of(reducedModifier);
    }


}
