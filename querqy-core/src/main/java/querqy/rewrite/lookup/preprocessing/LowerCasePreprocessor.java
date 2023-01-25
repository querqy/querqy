package querqy.rewrite.lookup.preprocessing;

import querqy.LowerCaseCharSequence;

public class LowerCasePreprocessor implements LookupPreprocessor {

    private LowerCasePreprocessor() {}

    @Override
    public CharSequence process(final CharSequence charSequence) {
        return new LowerCaseCharSequence(charSequence);
    }

    public static LowerCasePreprocessor create() {
        return new LowerCasePreprocessor();
    }
}
