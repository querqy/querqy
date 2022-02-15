package querqy.lucene.contrib.rewrite.wordbreak;

import querqy.CompoundCharSequence;

import java.util.Optional;

public class GermanUmlautCompoundingWordGenerator implements WordGenerator {

    private final CharSequence suffix;

    public GermanUmlautCompoundingWordGenerator(final CharSequence suffix) {
        this.suffix = suffix;
    }
    public GermanUmlautCompoundingWordGenerator() {
        this.suffix = "";
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {

        String replacement = null;
        int position = reducedModifier.length() - 1;
        while ((position > -1) && (replacement == null)) {
            switch (reducedModifier.charAt(position)) {
                case 'a':
                    replacement = "ä";
                    break;
                case 'o':
                    replacement = "ö";
                    break;
                case 'u':
                    replacement = "ü";
                    break;
                default:
                    position--;
            }
        }

        if (replacement == null) {
            return Optional.empty();
        }
        if (position == 0) {
            return Optional.of(new CompoundCharSequence(null, replacement,
                    reducedModifier.subSequence(1, reducedModifier.length())));
        }


        return Optional.of(
                new CompoundCharSequence(null, reducedModifier.subSequence(0, position), replacement,
                        reducedModifier.subSequence(position + 1, reducedModifier.length()), suffix));


    }
}
