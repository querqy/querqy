package querqy.rewrite.lookup;

public class LookupConfig {

    private static final LookupConfig DEFAULT_CONFIG = LookupConfig.builder()
            .ignoreCase(true)
            .hasBoundaries(true)
            .build();

    private final boolean ignoreCase;
    private final boolean hasBoundaries;

    private LookupConfig(final boolean ignoreCase, final boolean hasBoundaries) {
        this.ignoreCase = ignoreCase;
        this.hasBoundaries = hasBoundaries;
    }

    public boolean ignoreCase() {
        return ignoreCase;
    }

    public boolean hasBoundaries() {
        return hasBoundaries;
    }

    public static LookupConfigBuilder builder() {
        return new LookupConfigBuilder();
    }

    public static LookupConfig defaultConfig() {
        return DEFAULT_CONFIG;
    }

    public static class LookupConfigBuilder {

        private boolean ignoreCase;
        private boolean hasBoundaries;

        public LookupConfigBuilder ignoreCase(final boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }

        public LookupConfigBuilder hasBoundaries(final boolean hasBoundaries) {
            this.hasBoundaries = hasBoundaries;
            return this;
        }

        public LookupConfig build() {
            return new LookupConfig(ignoreCase, hasBoundaries);
        }
    }

}
