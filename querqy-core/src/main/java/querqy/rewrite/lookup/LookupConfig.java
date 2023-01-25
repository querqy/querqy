package querqy.rewrite.lookup;

import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;

import java.util.Objects;

public class LookupConfig {

    private static final LookupPreprocessor IDENTITY_PREPROCESSOR = charSequence -> charSequence;

    private static final LookupConfig DEFAULT_CONFIG = LookupConfig.builder()
            .ignoreCase(true)
            .hasBoundaries(true)
            .build();


    private final boolean ignoreCase;
    private final boolean hasBoundaries;

    private final LookupPreprocessor preprocessor;

    private LookupConfig(final boolean ignoreCase, final boolean hasBoundaries, final LookupPreprocessor preprocessor) {
        this.ignoreCase = ignoreCase;
        this.hasBoundaries = hasBoundaries;
        this.preprocessor = Objects.requireNonNullElse(preprocessor, IDENTITY_PREPROCESSOR);
    }

    public boolean ignoreCase() {
        return ignoreCase;
    }

    public boolean hasBoundaries() {
        return hasBoundaries;
    }

    public LookupPreprocessor getPreprocessor() {
        return preprocessor;
    }

    public static LookupConfig defaultConfig() {
        return DEFAULT_CONFIG;
    }

    public static LookupConfigBuilder builder() {
        return new LookupConfigBuilder();
    }

    public static class LookupConfigBuilder {

        private boolean ignoreCase;
        private boolean hasBoundaries;
        private LookupPreprocessor preprocessor;

        public LookupConfigBuilder ignoreCase(final boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }

        public LookupConfigBuilder hasBoundaries(final boolean hasBoundaries) {
            this.hasBoundaries = hasBoundaries;
            return this;
        }

        public LookupConfigBuilder preprocessor(final LookupPreprocessor preprocessor) {
            this.preprocessor = preprocessor;
            return this;
        }

        public LookupConfig build() {
            return new LookupConfig(ignoreCase, hasBoundaries, preprocessor);
        }
    }

}
