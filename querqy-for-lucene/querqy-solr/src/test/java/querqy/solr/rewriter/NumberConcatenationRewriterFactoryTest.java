package querqy.solr.rewriter;

import org.apache.solr.common.util.NamedList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CONFIG;
import static querqy.solr.rewriter.NumberConcatenationRewriterFactory.CONF_ACCEPT_GENERATED_TERMS;

public class NumberConcatenationRewriterFactoryTest {

    private final NumberConcatenationRewriterFactory factory = new NumberConcatenationRewriterFactory("test");

    @Test
    public void testThatDeprecatedConfigurationIsCorrectlyParsed() {

        final GZIPAwareResourceLoader resourceLoader = mock(GZIPAwareResourceLoader.class);

        NamedList<Object> configuration = new NamedList<>();
        configuration.add(CONF_CLASS, NumberConcatenationRewriterFactory.class.getName());
        configuration.add(CONF_ACCEPT_GENERATED_TERMS, true);

        Map<String, Object> parsed = factory.parseConfigurationToRequestHandlerBody(configuration, resourceLoader);
        // no exceptions!
        factory.configure((Map<String, Object>) parsed.get(CONF_CONFIG));

        RewriterFactory rewriterFactory = factory.getRewriterFactory();

        Assertions.assertThat(rewriterFactory).isInstanceOf(querqy.rewrite.contrib.NumberConcatenationRewriterFactory.class);
        Assertions.assertThat(factory.validateConfiguration((Map<String, Object>) parsed.get(CONF_CONFIG))).isNullOrEmpty();
    }
}