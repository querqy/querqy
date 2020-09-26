package querqy.solr.rewriter.replace;

import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.solr.RewriterConfigRequestBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ReplaceConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private Boolean ignoreCase;
    private Class<? extends QuerqyParserFactory> rhsParser;
    private String rules;
    private String inputDelimiter;

    public ReplaceConfigRequestBuilder() {
        super(ReplaceRewriterFactory.class);
    }

    @Override
    public Map<String, Object> buildConfig() {
        final Map<String, Object> config = new HashMap<>();

        if (ignoreCase != null) {
            config.put(ReplaceRewriterFactory.KEY_CONFIG_IGNORE_CASE, ignoreCase);
        }
        if (rhsParser != null) {
            config.put(ReplaceRewriterFactory.KEY_CONFIG_RHS_QUERY_PARSER, rhsParser.getName());
        }

        if (inputDelimiter != null) {
            if (inputDelimiter.isEmpty()) {
                throw new RuntimeException(ReplaceRewriterFactory.KEY_CONFIG_INPUT_DELIMITER + " must not be empty");
            }
            config.put(ReplaceRewriterFactory.KEY_CONFIG_INPUT_DELIMITER, inputDelimiter);
        }

        if (rules == null) {
            throw new RuntimeException(ReplaceRewriterFactory.KEY_CONFIG_RULES + " must not be null");
        }
        config.put(ReplaceRewriterFactory.KEY_CONFIG_RULES, rules);

        return config;
    }

    public ReplaceConfigRequestBuilder rules(final String rules) {
        this.rules = rules;
        return this;
    }

    public ReplaceConfigRequestBuilder rules(final InputStream inputStream) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            rules = reader.lines().collect(Collectors.joining("\n"));
        }
        return this;
    }

    public ReplaceConfigRequestBuilder ignoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }

    public ReplaceConfigRequestBuilder inputDelimiter(final String inputDelimiter) {
        this.inputDelimiter = inputDelimiter;
        return this;
    }

    public ReplaceConfigRequestBuilder rhsParser(final Class<? extends QuerqyParserFactory> rhsParser) {
        this.rhsParser = rhsParser;
        return this;
    }
}
