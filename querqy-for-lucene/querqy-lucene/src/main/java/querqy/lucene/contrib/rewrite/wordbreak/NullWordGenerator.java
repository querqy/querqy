package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Optional;

public class NullWordGenerator implements WordGenerator {

    public static NullWordGenerator INSTANCE = new NullWordGenerator();

    private NullWordGenerator() {
        // Use the singleton instance!
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        return Optional.of(reducedModifier);
    }


}
