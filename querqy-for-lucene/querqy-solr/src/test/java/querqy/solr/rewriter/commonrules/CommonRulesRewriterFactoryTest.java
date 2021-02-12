package querqy.solr.rewriter.commonrules;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory.*;

@RunWith(MockitoJUnitRunner.class)
public class CommonRulesRewriterFactoryTest {

    private static final String FILE_NAME = "f1";
    private final CommonRulesRewriterFactory factory = new CommonRulesRewriterFactory("test");

    @Mock
    private GZIPAwareResourceLoader resourceLoader;

    @Test
    public void testParseDeprecatedConfigurationWorksFine() throws IOException {

        when(resourceLoader.openResource(FILE_NAME)).thenReturn(IOUtils.toInputStream("1 tb =>\n" +
                "\tSYNONYM: 1tb", UTF_8));

        NamedList<Object> configuration = new NamedList<>();
        configuration.add(CONF_CLASS, CommonRulesRewriterFactory.class.getName());
        configuration.add(CONF_IGNORE_CASE, true);
        configuration.add(CONF_ALLOW_BOOLEAN_INPUT, false);
        configuration.add(CONF_RHS_QUERY_PARSER, WhiteSpaceQuerqyParserFactory.class.getName());
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("class", ExpressionSelectionStrategyFactory.class.getName());
        Map<String, Map<String, Object>> selectionStrategies = new HashMap<>();
        selectionStrategies.put("strategy1", strategy);
        configuration.add(CONF_RULE_SELECTION_STRATEGIES, selectionStrategies);
        configuration.add(CONF_RULES, "f1");

        Map<String, Object> parsed = factory.parseConfigurationToRequestHandlerBody(configuration, resourceLoader);
        // no exceptions!
        Map<String, Object> config = (Map<String, Object>) parsed.get(CONF_CONFIG);
        factory.configure(config);

        RewriterFactory rewriterFactory = factory.getRewriterFactory();

        assertThat(rewriterFactory).isInstanceOf(querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory.class);
        assertThat(factory.validateConfiguration(config)).isNull();

        assertThat(config.get(CONF_ALLOW_BOOLEAN_INPUT)).isEqualTo(false);
    }
}