package querqy.rewrite;

import java.util.Collections;
import java.util.Set;

public class RewriteLoggingConfig {

    private final boolean isActive;
    private final boolean hasDetails;
    private final Set<String> includedRewriters;

    private RewriteLoggingConfig(final boolean isActive, final boolean hasDetails, final Set<String> includedRewriters) {
        this.isActive = isActive;
        this.hasDetails = hasDetails;
        this.includedRewriters = includedRewriters;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean hasDetails() {
        return hasDetails;
    }

    public Set<String> getIncludedRewriters() {
        return includedRewriters;
    }

    private static final RewriteLoggingConfig INACTIVE = RewriteLoggingConfig.builder()
            .isActive(false)
            .build();

    private static final RewriteLoggingConfig FULL = RewriteLoggingConfig.builder()
            .isActive(true)
            .hasDetails(true)
            .build();

    private static final RewriteLoggingConfig IDS_ONLY = RewriteLoggingConfig.builder()
            .isActive(true)
            .hasDetails(false)
            .build();

    public static RewriteLoggingConfig off() {
        return INACTIVE;
    }

    public static RewriteLoggingConfig idsOnly() {
        return IDS_ONLY;
    }

    public static RewriteLoggingConfig details() {
        return FULL;
    }

    public static RewriteLoggingConfigBuilder builder() {
        return new RewriteLoggingConfigBuilder();
    }

    public static class RewriteLoggingConfigBuilder {

        private boolean isActive;
        private boolean hasDetails;
        private Set<String> includedRewriters;

        public RewriteLoggingConfigBuilder isActive(final boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public RewriteLoggingConfigBuilder hasDetails(final boolean hasDetails) {
            this.hasDetails = hasDetails;
            return this;
        }

        public RewriteLoggingConfigBuilder includedRewriters(final Set<String> includedRewriters) {
            this.includedRewriters = includedRewriters;
            return this;
        }

        public RewriteLoggingConfig build() {
            if (includedRewriters == null) {
                includedRewriters = Collections.emptySet();
            }

            return new RewriteLoggingConfig(isActive, hasDetails, includedRewriters);
        }
    }

}
