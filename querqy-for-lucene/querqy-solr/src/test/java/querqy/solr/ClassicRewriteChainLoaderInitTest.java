package querqy.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClassicRewriteChainLoaderInitTest {
    
    @Mock
    private SolrCore core;

    private ClassicRewriteChainLoader testee;

    @Before
    public void setup() {
        testee = new ClassicRewriteChainLoader(core);
    }

    @Test
    public void shouldConfigureRewriterRequestHandler() {
        NamedList<String> args = new NamedList<>();
        args.add("rewriterRequestHandler", "/my/crazy/endpoint");
        testee.init(args);

        MatcherAssert.assertThat(testee.getRewriterRequestHandler(), Matchers.is("/my/crazy/endpoint"));
    }
    
    @Test
    public void shouldNotConfigureEmptyRewriterRequestHandler() {
        NamedList<String> args = new NamedList<>();
        args.add("rewriterRequestHandler", "");
        testee.init(args);

        MatcherAssert.assertThat(testee.getRewriterRequestHandler(), Matchers.is(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME));
    }

}
