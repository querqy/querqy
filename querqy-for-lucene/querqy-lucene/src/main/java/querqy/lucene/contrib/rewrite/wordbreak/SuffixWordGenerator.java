package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CompoundCharSequence;

import java.util.Optional;

public class SuffixWordGenerator implements WordGenerator {

    final CharSequence suffix;

    public SuffixWordGenerator(final CharSequence suffix) {
        if (suffix == null || suffix.length() == 0) {
            throw new IllegalArgumentException("suffix with length > 0 expected");
        }
        this.suffix = suffix;
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        return Optional.of(new CompoundCharSequence(null, reducedModifier, suffix));
    }

}
