package querqy.rewrite.lookup.preprocessing;

@FunctionalInterface
public interface Preprocessor {

    CharSequence process(final CharSequence charSequence);
}
