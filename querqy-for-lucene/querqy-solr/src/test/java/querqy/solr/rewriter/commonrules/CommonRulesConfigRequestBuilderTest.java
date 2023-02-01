package querqy.solr.rewriter.commonrules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorType;

import java.util.List;
import java.util.Map;

public class CommonRulesConfigRequestBuilderTest {

    @Test
    public void testThatRulesMustBeSet() {
        try {
            new CommonRulesConfigRequestBuilder().buildConfig();
            fail("rules==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CommonRulesRewriterFactory.CONF_RULES));
        }

        try {
            new CommonRulesConfigRequestBuilder().rules((String) null);
            fail("rules==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CommonRulesRewriterFactory.CONF_RULES));
        }
    }

    @Test
    public void testThatRulesCanBeEmpty() {
        final Map<String, Object> config = new CommonRulesConfigRequestBuilder().rules("").buildConfig();
        assertThat(config, hasEntry(CommonRulesRewriterFactory.CONF_RULES, ""));

        final List<String> errors = new CommonRulesRewriterFactory("id").validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());
    }

    @Test
    public void testSetAllProperties() {
        final Map<String, Object> config = new CommonRulesConfigRequestBuilder()
                .rules("trainers =>\n" +
                        "SYNONYM: sneakers")
                .lookupPreprocessorType(LookupPreprocessorType.GERMAN)
                .rhsParser(WhiteSpaceQuerqyParserFactory.class)
                .ruleSelectionStrategy("strategy1", ExpressionSelectionStrategyFactory.class)
                .buildConfig();
        assertThat(config, hasEntry(CommonRulesRewriterFactory.CONF_RULES, "trainers =>\n" +
                "SYNONYM: sneakers"));
        assertThat(config, hasEntry(CommonRulesRewriterFactory.CONF_LOOKUP_PREPROCESSOR, "german"));

        final Map<String,Map<String,Object>> strategyConfig = (Map<String,Map<String,Object>>) config
                .get(CommonRulesRewriterFactory.CONF_RULE_SELECTION_STRATEGIES);
        assertNotNull(strategyConfig);
        assertThat(strategyConfig.get("strategy1"), hasEntry(equalTo("class"),
                equalTo(ExpressionSelectionStrategyFactory.class.getName())));

        assertThat(config, hasEntry(CommonRulesRewriterFactory.CONF_RHS_QUERY_PARSER,
                WhiteSpaceQuerqyParserFactory.class.getName()));
    }

}