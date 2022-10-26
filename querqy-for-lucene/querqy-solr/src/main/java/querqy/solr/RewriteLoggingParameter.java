package querqy.solr;

import java.util.Locale;

public enum RewriteLoggingParameter {

    OFF("off"), DETAILS("details"), REWRITER_ID("rewriter_id");

    public static final String REWRITE_LOGGING_PARAM_KEY = "querqy.rewriteLogging";

    private final String value;

    RewriteLoggingParameter(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RewriteLoggingParameter of(final String value) {
        return RewriteLoggingParameter.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
