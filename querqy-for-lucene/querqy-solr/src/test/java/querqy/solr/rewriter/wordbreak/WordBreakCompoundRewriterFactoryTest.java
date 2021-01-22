package querqy.solr.rewriter.wordbreak;

import com.google.common.collect.Lists;
import org.apache.solr.common.util.NamedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.*;

@RunWith(MockitoJUnitRunner.class)
public class WordBreakCompoundRewriterFactoryTest {

    private final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("test");

    @Mock
    private GZIPAwareResourceLoader resourceLoader;

    @Test
    public void testParseDeprecatedConfigurationWorksFine() {
        NamedList<Object> configuration = new NamedList<>();
        configuration.add(CONF_CLASS, WordBreakCompoundRewriterFactory.class.getName());
        configuration.add(CONF_DICTIONARY_FIELD, "f1");
        configuration.add(CONF_LOWER_CASE_INPUT, true);
        configuration.add(CONF_DECOMPOUND + "." + CONF_DECOMPOUND_MAX_EXPANSIONS, 5);
        configuration.add(CONF_DECOMPOUND + "." + CONF_DECOMPOUND_VERIFY_COLLATION, true);
        configuration.add(CONF_MORPHOLOGY, "GERMAN");
        configuration.add(CONF_MORPHOLOGY, Lists.newArrayList("for"));

        Map<String, Object> parsed = factory.parseConfigurationToRequestHandlerBody(configuration, resourceLoader);
        factory.configure((Map<String, Object>) parsed.get(CONF_CONFIG));

        RewriterFactory rewriterFactory = factory.getRewriterFactory();

        assertThat(rewriterFactory).isInstanceOf(querqy.lucene.contrib.rewrite.wordbreak.WordBreakCompoundRewriterFactory.class);
        assertThat(factory.validateConfiguration((Map<String, Object>) parsed.get(CONF_CONFIG))).isNull();
    }
}