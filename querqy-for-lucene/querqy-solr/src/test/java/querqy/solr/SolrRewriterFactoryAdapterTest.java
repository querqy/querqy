package querqy.solr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.RewriterFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static querqy.solr.SolrRewriterFactoryAdapter.loadInstance;

@RunWith(MockitoJUnitRunner.class)
public class SolrRewriterFactoryAdapterTest {

    @Test
    public void testThat_defaultSinkIsAdded_forNoLoggingConfigured() {
        final SolrRewriterFactoryAdapter adapter = loadInstance(
                "id", Map.of(
                        "class", TestRewriterFactory.class.getName(),
                        "config", Collections.emptyMap()
                )
        );

        assertThat(adapter.getSinks()).isNotEmpty();
    }

    @Test
    public void testThat_exceptionIsThrown_forNoSinkConfiguredInLogging() {
        assertThatThrownBy(
                () -> loadInstance(
                        "id", Map.of(
                        "class", TestRewriterFactory.class.getName(),
                        "config", Collections.emptyMap(),
                        "logging", Collections.emptyMap()
                        )
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testThat_allSinksAreCreated_forGivenClassPaths() {
        final SolrRewriterFactoryAdapter adapter = loadInstance(
                "id", Map.of(
                        "class", TestRewriterFactory.class.getName(),
                        "config", Collections.emptyMap(),
                        "logging", Map.of(
                                "sinks", List.of(
                                        "response",
                                        "response"
                                )
                        )
                )
        );

        assertThat(adapter.getSinks()).hasSize(2);
    }



    public static class TestRewriterFactory extends SolrRewriterFactoryAdapter {

        public TestRewriterFactory(String rewriterId) {
            super(rewriterId);
        }

        @Override
        public void configure(Map<String, Object> config) {}

        @Override
        public List<String> validateConfiguration(Map<String, Object> config) {
            return Collections.emptyList();
        }

        @Override
        public RewriterFactory getRewriterFactory() {
            return mock(RewriterFactory.class);
        }
    }

}
