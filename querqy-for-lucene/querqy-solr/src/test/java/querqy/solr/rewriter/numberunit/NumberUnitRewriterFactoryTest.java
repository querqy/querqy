package querqy.solr.rewriter.numberunit;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.util.NamedList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.rewrite.RewriterFactory;

import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;
import static querqy.solr.rewriter.numberunit.NumberUnitRewriterFactory.CONF_PROPERTY;

public class NumberUnitRewriterFactoryTest {

    private static final String FILE_NAME = "f1";
    private final NumberUnitRewriterFactory factory = new NumberUnitRewriterFactory("test");

    @Test
    public void testThatDeprecatedConfigurationIsCorrectlyParsed() throws IOException {

        final GZIPAwareResourceLoader resourceLoader = mock(GZIPAwareResourceLoader.class);

        when(resourceLoader.openResource(FILE_NAME)).thenReturn(IOUtils.toInputStream("{\n" +
                "   \"numberUnitDefinitions\": [\n" +
                "      {\n" +
                "         \"units\": [ { \"term\": \"inch\" } ],\n" +
                "         \"fields\": [ { \"fieldName\": \"screen_size\" } ]\n" +
                "      }\n" +
                "   ]\n" +
                "}", UTF_8));

        NamedList<Object> configuration = new NamedList<>();
        configuration.add(CONF_CLASS, NumberUnitRewriterFactory.class.getName());
        configuration.add(CONF_PROPERTY, "f1");

        Map<String, Object> parsed = factory.parseConfigurationToRequestHandlerBody(configuration, resourceLoader);
        // no exceptions!
        factory.configure((Map<String, Object>) parsed.get(CONF_PROPERTY));

        RewriterFactory rewriterFactory = factory.getRewriterFactory();

        Assertions.assertThat(rewriterFactory).isInstanceOf(querqy.rewrite.contrib.NumberUnitRewriterFactory.class);
        Assertions.assertThat(factory.validateConfiguration((Map<String, Object>) parsed.get(CONF_PROPERTY))).isNullOrEmpty();
    }

}