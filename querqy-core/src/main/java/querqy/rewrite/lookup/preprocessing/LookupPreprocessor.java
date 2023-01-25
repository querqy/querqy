package querqy.rewrite.lookup.preprocessing;

@FunctionalInterface
public interface LookupPreprocessor {

    CharSequence process(final CharSequence charSequence);
}
