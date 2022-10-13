package querqy.model.logging;

import java.util.Collections;
import java.util.Set;

public class RewriteLoggingConfig {

    private final boolean isActive;
    private final boolean hasDetails;
    private final Set<String> includedRewriters;

    public RewriteLoggingConfig(final boolean isActive, final boolean hasDetails, final Set<String> includedRewriters) {
        this.isActive = isActive;
        this.hasDetails = hasDetails;
        this.includedRewriters = includedRewriters;
    }

    public RewriteLoggingConfig(final boolean isActive, final boolean hasDetails) {
        this(isActive, hasDetails, Collections.emptySet());
    }

    public RewriteLoggingConfig(final boolean isActive) {
        this(isActive, false, Collections.emptySet());
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

    public static final RewriteLoggingConfig INACTIVE_REWRITE_LOGGING = new RewriteLoggingConfig(false);

    public static RewriteLoggingConfig inactiveRewriteLogging() {
        return INACTIVE_REWRITE_LOGGING;
    }
}
