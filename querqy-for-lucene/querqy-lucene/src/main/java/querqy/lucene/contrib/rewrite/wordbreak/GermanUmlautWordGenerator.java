package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CompoundCharSequence;

import java.util.Optional;

public class GermanUmlautWordGenerator implements WordGenerator {

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {

        String replacement = null;
        int position = reducedModifier.length() - 1;
        while ((position > -1) && (replacement == null)) {
            switch (reducedModifier.charAt(position)) {
                case 'a':
                case 'o':
                    replacement = null;
                    position = -1;
                    break;
                case 'u':
                    if (position > 0 && reducedModifier.charAt(position - 1) == 'ä') {
                        position -= 1;
                        replacement = "a";
                    } else {
                        replacement = null;
                        position = -1;
                    }
                    break;
                case 'ä':
                    replacement = "a";
                    break;
                case 'ö':
                    replacement = "o";
                    break;
                case 'ü':
                    replacement = "u";
                    break;
                default:
                    replacement = null;
                    position--;
            }
        }

        if (replacement == null) {
            return Optional.empty();
        }

        return Optional.of(
                position == 0
                        ? new CompoundCharSequence(null, replacement,
                            reducedModifier.subSequence(1, reducedModifier.length()))
                        : new CompoundCharSequence(null, reducedModifier.subSequence(0, position), replacement,
                            reducedModifier.subSequence(position + 1, reducedModifier.length())));


    }
}
