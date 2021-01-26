package querqy.solr.rewriter.replace;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.solr.rewriter.ClassicConfigurationParser;
import querqy.solr.utils.ConfigUtils;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.utils.ConfigUtils.ifNotNull;

public class ReplaceRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_RULES = "rules";
    public static final String CONF_RHS_QUERY_PARSER = "querqyParser";
    public static final String CONF_INPUT_DELIMITER = "inputDelimiter";
    public static final String CONF_IGNORE_CASE = "ignoreCase";


    private static final Boolean DEFAULT_IGNORE_CASE = true;
    private static final String DEFAULT_INPUT_DELIMITER = "\t";
    private static final QuerqyParserFactory DEFAULT_RHS_QUERY_PARSER = new WhiteSpaceQuerqyParserFactory();

    private querqy.rewrite.contrib.ReplaceRewriterFactory delegate = null;

    public ReplaceRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) {
        final String rules = (String) config.get(CONF_RULES);
        final InputStreamReader rulesReader = new InputStreamReader(new ByteArrayInputStream(rules.getBytes()));

        final boolean ignoreCase = ConfigUtils.getArg(config, CONF_IGNORE_CASE, DEFAULT_IGNORE_CASE);

        final String inputDelimiter = ConfigUtils.getArg(config, CONF_INPUT_DELIMITER, DEFAULT_INPUT_DELIMITER);

        final QuerqyParserFactory querqyParser = ConfigUtils.getInstanceFromArg(
                config, CONF_RHS_QUERY_PARSER, DEFAULT_RHS_QUERY_PARSER);

        try {
            delegate = new querqy.rewrite.contrib.ReplaceRewriterFactory(rewriterId, rulesReader, ignoreCase,
                    inputDelimiter, querqyParser.createParser());
        } catch (final IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Cannot parse previously validated " +
                    "configuration for rewriter " + rewriterId, e);
        }
    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        final String rules = (String) config.get(CONF_RULES);
        if (rules == null) {
            return Collections.singletonList("Property '" + CONF_RULES + "' not configured");
        }

        final InputStreamReader rulesReader = new InputStreamReader(new ByteArrayInputStream(rules.getBytes()));

        final boolean ignoreCase = ConfigUtils.getArg(config, CONF_IGNORE_CASE, DEFAULT_IGNORE_CASE);

        final String inputDelimiter = ConfigUtils.getArg(config, CONF_INPUT_DELIMITER, DEFAULT_INPUT_DELIMITER);


        final QuerqyParserFactory querqyParser;
        try {
            querqyParser = ConfigUtils.getInstanceFromArg(config, CONF_RHS_QUERY_PARSER, DEFAULT_RHS_QUERY_PARSER);
        } catch (final Exception e) {
            return Collections.singletonList("Invalid attribute '" + CONF_RHS_QUERY_PARSER + "': " + e.getMessage());
        }

        try {
            new querqy.rewrite.contrib.ReplaceRewriterFactory(rewriterId, rulesReader, ignoreCase,
                    inputDelimiter, querqyParser.createParser());
        } catch (final IOException e) {
            return Collections.singletonList("Cannot create rewriter: " + e.getMessage());
        }

        return null;
    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return delegate;
    }

    @Override
    public Map<String, Object> parseConfigurationToRequestHandlerBody(final NamedList<Object> configuration, final ResourceLoader resourceLoader) throws RuntimeException {

        final Map<String, Object> result = new HashMap<>();
        final Map<Object, Object> conf = new HashMap<>();
        result.put(CONF_CONFIG, conf);

        ifNotNull((String) configuration.get(CONF_RULES), rulesFile -> {
            try {
                final String rules = IOUtils.toString(resourceLoader.openResource(rulesFile), UTF_8);
                conf.put(CONF_RULES, rules);
            } catch (IOException e) {
                throw new RuntimeException("Could not load file: " + rulesFile + " because " + e.getMessage());
            }
        });

        ifNotNull(configuration.get(CONF_IGNORE_CASE), v -> conf.put(CONF_IGNORE_CASE, v));
        ifNotNull(configuration.get(CONF_RHS_QUERY_PARSER), v -> conf.put(CONF_RHS_QUERY_PARSER, v));
        ifNotNull(configuration.get(CONF_INPUT_DELIMITER), v -> conf.put(CONF_INPUT_DELIMITER, v));
        ifNotNull(configuration.get(CONF_CLASS), v -> result.put(CONF_CLASS, v));

        return result;
    }
}
