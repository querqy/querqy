package querqy.solr;

import java.util.Locale;

public enum RewriteLoggingParameters {

    OFF("off"), DETAILS("details"), REWRITER_ID("rewriter_id");

    public static final String REWRITE_LOGGING_PARAM_KEY = "querqy.rewriteLogging";
    public static final String PARAM_REWRITE_LOGGING_REWRITERS = "querqy.rewriteLogging.rewriters";

    private final String value;

    RewriteLoggingParameters(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RewriteLoggingParameters of(final String value) {
        return RewriteLoggingParameters.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
