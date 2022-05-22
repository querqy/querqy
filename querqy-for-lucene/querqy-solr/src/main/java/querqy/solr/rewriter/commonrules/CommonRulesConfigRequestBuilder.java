package querqy.solr.rewriter.commonrules;

import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.solr.FactoryAdapter;
import querqy.solr.RewriterConfigRequestBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CommonRulesConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private Boolean ignoreCase = null;
    private Boolean allowBooleanInput = null;
    private BoostInstruction.BoostMethod boostMethod = null;
    private Class<? extends QuerqyParserFactory> rhsParser = null;
    private String rules = null;
    private final Map<String, Map<String, Object>> ruleSelectionStrategies = new HashMap<>();

    public CommonRulesConfigRequestBuilder() {
        super(CommonRulesRewriterFactory.class);
    }

    public CommonRulesConfigRequestBuilder ignoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }

    public CommonRulesConfigRequestBuilder allowBooleanInput(final boolean allowBooleanInput) {
        this.allowBooleanInput = allowBooleanInput;
        return this;
    }

    public CommonRulesConfigRequestBuilder boostMethod(final BoostInstruction.BoostMethod boostMethod) {
        this.boostMethod = boostMethod;
        return this;
    }

    public CommonRulesConfigRequestBuilder rhsParser(final Class<? extends QuerqyParserFactory> rhsParser) {
        this.rhsParser = rhsParser;
        return this;
    }

    public CommonRulesConfigRequestBuilder rules(final String rules) {
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        this.rules = rules;
        return this;
    }

    public CommonRulesConfigRequestBuilder rules(final InputStream inputStream) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            rules = reader.lines().collect(Collectors.joining("\n"));
        }
        return this;
    }

    public CommonRulesConfigRequestBuilder ruleSelectionStrategy(final String id,
                                                                 final Class<? extends FactoryAdapter<SelectionStrategyFactory>> strategy) {
        final Map<String, Object> config = new HashMap<>(1);
        config.put("class", strategy.getName());
        if (ruleSelectionStrategies.put(id, config) != null) {
            throw new IllegalStateException("Duplicate RuleSelectionStrategy: " + id);
        }
        return this;
    }

    @Override
    public Map<String, Object> buildConfig() {

        final Map<String, Object> config = new HashMap<>();

        if (ignoreCase != null) {
            config.put(CommonRulesRewriterFactory.CONF_IGNORE_CASE, ignoreCase);
        }
        if (rhsParser != null) {
            config.put(CommonRulesRewriterFactory.CONF_RHS_QUERY_PARSER, rhsParser.getName());
        }

        if (allowBooleanInput != null) {
            config.put(CommonRulesRewriterFactory.CONF_ALLOW_BOOLEAN_INPUT, allowBooleanInput);
        }

        if (boostMethod != null) {
            config.put(CommonRulesRewriterFactory.CONF_BOOST_METHOD, boostMethod.name());
        }

        if (rules == null) {
            throw new RuntimeException(CommonRulesRewriterFactory.CONF_RULES + " must not be null");
        }

        config.put(CommonRulesRewriterFactory.CONF_RULES, rules);

        if (!ruleSelectionStrategies.isEmpty()) {
            config.put(CommonRulesRewriterFactory.CONF_RULE_SELECTION_STRATEGIES, ruleSelectionStrategies);
        }

        return config;

    }


}
