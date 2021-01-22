package querqy.solr.rewriter.commonrules;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.select.ExpressionCriteriaSelectionStrategyFactory;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.solr.FactoryAdapter;
import querqy.solr.SolrRewriterFactoryAdapter;
import querqy.solr.rewriter.ClassicConfigurationParser;
import querqy.solr.utils.ConfigUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;

public class CommonRulesRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_IGNORE_CASE = "ignoreCase";
    public static final String CONF_RHS_QUERY_PARSER = "querqyParser";
    public static final String CONF_RULES = "rules";
    public static final String CONF_RULE_SELECTION_STRATEGIES = "ruleSelectionStrategies";

    static final QuerqyParserFactory DEFAULT_RHS_QUERY_PARSER = new WhiteSpaceQuerqyParserFactory();
    static final SelectionStrategyFactory DEFAULT_SELECTION_STRATEGY_FACTORY =
            new ExpressionCriteriaSelectionStrategyFactory();

    private RewriterFactory delegate = null;

    public CommonRulesRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }


    @Override
    public void configure(final Map<String, Object> config) {

        final boolean ignoreCase = ConfigUtils.getArg(config, CONF_IGNORE_CASE, true);

        final QuerqyParserFactory querqyParser = ConfigUtils.getInstanceFromArg(config, CONF_RHS_QUERY_PARSER,
                DEFAULT_RHS_QUERY_PARSER);

        final String rules = ConfigUtils.getStringArg(config, CONF_RULES, "");

        final Map<String, SelectionStrategyFactory> selectionStrategyFactories = loadSelectionStrategyFactories(config);

        try {
            delegate = new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(rewriterId,
                    new StringReader(rules), querqyParser, ignoreCase, selectionStrategyFactories,
                    DEFAULT_SELECTION_STRATEGY_FACTORY);
        } catch (final IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                    "Could not create delegate factory ", e);
        }


    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {

        final String rules = ConfigUtils.getStringArg(config, CONF_RULES, null);
        if (rules == null) {
            return Collections.singletonList("Missing attribute '" + CONF_RULES + "'");
        }
        final QuerqyParserFactory querqyParser;
        try {
            querqyParser = ConfigUtils
                    .getInstanceFromArg(config, CONF_RHS_QUERY_PARSER, DEFAULT_RHS_QUERY_PARSER);
        } catch (final Exception e) {
            return Collections.singletonList("Invalid attribute '" + CONF_RHS_QUERY_PARSER + "': " + e.getMessage());
        }

        final Map<String, SelectionStrategyFactory> selectionStrategyFactories;
        try {
            selectionStrategyFactories = loadSelectionStrategyFactories(config);
        } catch (final Exception e) {
            return Collections.singletonList(e.getMessage());
        }


        final boolean ignoreCase = ConfigUtils.getArg(config, CONF_IGNORE_CASE, true);
        try {
            new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(rewriterId,
                    new StringReader(rules), querqyParser, ignoreCase, selectionStrategyFactories,
                    DEFAULT_SELECTION_STRATEGY_FACTORY);
        } catch (final IOException e) {
            return Collections.singletonList("Cannot create rewriter: " + e.getMessage());
        }

        return null;
    }

    protected Map<String, SelectionStrategyFactory> loadSelectionStrategyFactories(final Map<String, Object> config) {
        final Map<String, Map<String, Object>> ruleSelectionStrategiesConfig = ConfigUtils.getArg(config,
                CONF_RULE_SELECTION_STRATEGIES, Collections.emptyMap());

        final Map<String, SelectionStrategyFactory> selectionStrategyFactories = new HashMap<>(
                ruleSelectionStrategiesConfig.size());


        for (final Map.Entry<String, Map<String, Object>> entry : ruleSelectionStrategiesConfig.entrySet()) {

            final String strategyId = entry.getKey();
            try {
                final Map<String, Object> strategyConfig = entry.getValue();
                final FactoryAdapter<SelectionStrategyFactory> factory = ConfigUtils.newInstance(
                        strategyConfig.get("class").toString(), FactoryAdapter.class);
                if (selectionStrategyFactories.put(strategyId,
                        factory.createFactory(strategyId, strategyConfig)) != null) {
                    throw new RuntimeException("Could not create ruleSelectionStrategy " + strategyId);
                }
            } catch (final Exception e) {
                throw new RuntimeException("Could not create ruleSelectionStrategy " + strategyId, e);

            }
        }

        return selectionStrategyFactories;
    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return delegate;
    }

    @Override
    public Map<String, Object> parseConfigurationToRequestHandlerBody(NamedList<Object> configuration, GZIPAwareResourceLoader resourceLoader) throws RuntimeException {

        final Map<String, Object> result = new HashMap<>();

        ifNotNull((String) configuration.get(CONF_RULES), rulesFile -> {
            try {
                final String rules = IOUtils.toString(resourceLoader.openResource(rulesFile), UTF_8);
                final HashMap<Object, Object> ruleMap = new HashMap<>();
                ruleMap.put(CONF_RULES, rules);
                result.put(CONF_CONFIG, ruleMap);
            } catch (IOException e) {
                throw new RuntimeException("Could not load file: " + rulesFile + " because " + e.getMessage());
            }
        });

        ifNotNull(configuration.get(CONF_IGNORE_CASE), v -> result.put(CONF_IGNORE_CASE, v));
        ifNotNull(configuration.get(CONF_RHS_QUERY_PARSER), v -> result.put(CONF_RHS_QUERY_PARSER, v));
        ifNotNull(configuration.get(CONF_RULE_SELECTION_STRATEGIES), v -> result.put(CONF_RULE_SELECTION_STRATEGIES, v));
        ifNotNull(configuration.get(CONF_CLASS), v -> result.put(CONF_CLASS, v));

        return result;
    }

    private static <T> void ifNotNull(T value, Consumer<T> supplier) {
        if (value != null) {
            supplier.accept(value);
        }
    }
}
