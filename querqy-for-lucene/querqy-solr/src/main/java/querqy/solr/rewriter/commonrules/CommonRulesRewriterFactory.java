package querqy.solr.rewriter.commonrules;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewrite.commonrules.select.ExpressionCriteriaSelectionStrategyFactory;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;
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
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.utils.ConfigUtils.ifNotNull;

public class CommonRulesRewriterFactory extends SolrRewriterFactoryAdapter implements ClassicConfigurationParser {

    public static final String CONF_IGNORE_CASE = "ignoreCase";
    public static final String CONF_ALLOW_BOOLEAN_INPUT = "allowBooleanInput";
    public static final String CONF_BOOST_METHOD = "boostMethod";
    public static final String CONF_RHS_QUERY_PARSER = "querqyParser";
    public static final String CONF_RULES = "rules";
    public static final String CONF_RULE_SELECTION_STRATEGIES = "ruleSelectionStrategies";
    public static final String CONF_LOOKUP_PREPROCESSOR = "lookupPreprocessor";

    static final QuerqyParserFactory DEFAULT_RHS_QUERY_PARSER = new WhiteSpaceQuerqyParserFactory();
    static final SelectionStrategyFactory DEFAULT_SELECTION_STRATEGY_FACTORY =
            new ExpressionCriteriaSelectionStrategyFactory();
    public static final String CONF_BUILD_TERM_CACHE = "buildTermCache";

    private RewriterFactory delegate = null;

    public CommonRulesRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }


    @Override
    public void configure(final Map<String, Object> config) {

        final boolean ignoreCase = ConfigUtils.getArg(config, CONF_IGNORE_CASE, true);
        final boolean allowBooleanInput = ConfigUtils.getArg(config, CONF_ALLOW_BOOLEAN_INPUT, false);
        final BoostMethod boostMethod = readBoostMethod(config);

        final QuerqyParserFactory querqyParser = ConfigUtils.getInstanceFromArg(config, CONF_RHS_QUERY_PARSER,
                DEFAULT_RHS_QUERY_PARSER);

        final String rules = ConfigUtils.getStringArg(config, CONF_RULES, "");

        final Boolean buildTermCache = ConfigUtils.getArg(config, CONF_BUILD_TERM_CACHE, true);

        final Map<String, SelectionStrategyFactory> selectionStrategyFactories = loadSelectionStrategyFactories(config);

        final Optional<String> lookupPreprocessorTypeName = ConfigUtils.getStringArg(config, CONF_LOOKUP_PREPROCESSOR);
        final LookupPreprocessorType lookupPreprocessorType = lookupPreprocessorTypeName
                .map(LookupPreprocessorType::fromString)
                .orElse(LookupPreprocessorType.NONE);

        try {
            delegate = new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(
                    rewriterId,
                    new StringReader(rules),
                    allowBooleanInput,
                    boostMethod,
                    querqyParser,
                    ignoreCase,
                    selectionStrategyFactories,
                    DEFAULT_SELECTION_STRATEGY_FACTORY,
                    buildTermCache,
                    lookupPreprocessorType);
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
        final boolean allowBooleanInput = ConfigUtils.getArg(config, CONF_ALLOW_BOOLEAN_INPUT, false);
        final BoostMethod boostMethod = readBoostMethod(config);

        final Boolean buildTermCache = ConfigUtils.getArg(config, CONF_BUILD_TERM_CACHE, true);

        final Optional<String> lookupPreprocessorTypeName = ConfigUtils.getStringArg(config, CONF_LOOKUP_PREPROCESSOR);
        final LookupPreprocessorType lookupPreprocessorType = lookupPreprocessorTypeName
                .map(LookupPreprocessorType::fromString)
                .orElse(LookupPreprocessorType.NONE);

        try {
            new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(
                    rewriterId,
                    new StringReader(rules),
                    allowBooleanInput,
                    boostMethod,
                    querqyParser,
                    ignoreCase,
                    selectionStrategyFactories,
                    DEFAULT_SELECTION_STRATEGY_FACTORY,
                    buildTermCache,
                    lookupPreprocessorType
            );
        } catch (final IOException e) {
            return Collections.singletonList("Cannot create rewriter: " + e.getMessage());
        }

        return null;
    }

    protected BoostMethod readBoostMethod(final Map<String, Object> config) {
        final String boostMethodConfig = ConfigUtils.getArg(config, CONF_BOOST_METHOD, BoostMethod.ADDITIVE.name());
        return BoostMethod.valueOf(boostMethodConfig.toUpperCase());
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
    public Map<String, Object> parseConfigurationToRequestHandlerBody(final NamedList<Object> configuration,
                                                                      final ResourceLoader resourceLoader) throws RuntimeException {

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
        ifNotNull(configuration.get(CONF_RULE_SELECTION_STRATEGIES), v -> conf.put(CONF_RULE_SELECTION_STRATEGIES, v));
        ifNotNull(configuration.get(CONF_ALLOW_BOOLEAN_INPUT), v -> conf.put(CONF_ALLOW_BOOLEAN_INPUT, v));
        ifNotNull(configuration.get(CONF_CLASS), v -> result.put(CONF_CLASS, v));
        return result;
    }
}
