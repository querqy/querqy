package querqy.rewrite.lookup.preprocessing;

public class LookupPreprocessorFactory {

    private static final LookupPreprocessor IDENTITY_PREPROCESSOR = charSequence -> charSequence;

    private static final LookupPreprocessor LOWERCASE_PREPROCESSOR = LowerCasePreprocessor.create();

    public static LookupPreprocessor identity() {
        return IDENTITY_PREPROCESSOR;
    }

    public static LookupPreprocessor lowercase() {
        return LOWERCASE_PREPROCESSOR;
    }

    public static LookupPreprocessor fromType(final LookupPreprocessorType type) {

        switch (type) {
            case NONE:
                return IDENTITY_PREPROCESSOR;

            case GERMAN:
                return GermanPreprocessorOnDemandClassHolder.GERMAN_PREPROCESSOR;

            case LOWERCASE:
                return LOWERCASE_PREPROCESSOR;

            default:
                throw new IllegalArgumentException("Preprocessor of type " + " is currently not supported");
        }

    }

    // This wrapper makes sure the maps in the GermanNounNormalizer are loaded lazily (and in a synchronized way)
    private static class GermanPreprocessorOnDemandClassHolder {
        private static final LookupPreprocessor GERMAN_PREPROCESSOR = PipelinePreprocessor.of(
                LowerCasePreprocessor.create(),
                GermanUmlautPreprocessor.create(),
                GermanNounNormalizer.create()
        );
    }
}
