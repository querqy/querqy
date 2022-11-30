package querqy.solr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.infologging.InfoLogging;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;

/**
 * Created by rene on 04/05/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class QuerqyQParserPluginTest {

    QuerqyQParserPlugin plugin = new QuerqyQParserPlugin() {

        @Override
        public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req,
                                    RewriteChain rewriteChain, InfoLogging infoLogging, TermQueryCache termQueryCache) {
            return null;
        }
    };


    @Mock
    NamedList<String> parserConfig;


    @Test
    public void testThatQuerqyParserFactoryIsLoadedAndInitializedIfFactoryIsConfigured() throws Exception {

        NamedList<NamedList<String>> args = mock(NamedList.class);
        when(args.get("parser")).thenReturn(parserConfig);

        when(parserConfig.get("factory")).thenReturn("querqy.solr.SimpleQuerqyQParserFactory");
        when(parserConfig.get("class")).thenReturn("querqy.parser.WhiteSpaceQuerqyParser");

        ResourceLoader resourceLoader = new ClasspathResourceLoader(getClass().getClassLoader());

        final SolrQuerqyParserFactory factory = plugin.loadSolrQuerqyParserFactory(resourceLoader, args);

        assertNotNull(factory);
        assertTrue(factory instanceof SimpleQuerqyQParserFactory);
        SimpleQuerqyQParserFactory qParserFactory = (SimpleQuerqyQParserFactory) factory;
        assertEquals(WhiteSpaceQuerqyParser.class, qParserFactory.querqyParserClass);

    }

    @Test
    public void testThatASimpleQuerqyQParserFactoryIsCreatedIfOnlyTheParserClassIsConfigured() throws Exception {

        NamedList<NamedList<String>> args = mock(NamedList.class);
        when(args.get("parser")).thenReturn(parserConfig);

        when(parserConfig.get("factory")).thenReturn(null);
        when(parserConfig.get("class")).thenReturn("querqy.parser.WhiteSpaceQuerqyParser");
        ResourceLoader resourceLoader = new ClasspathResourceLoader(getClass().getClassLoader());

        final SolrQuerqyParserFactory factory = plugin.loadSolrQuerqyParserFactory(resourceLoader, args);

        assertNotNull(factory);
        assertTrue(factory instanceof SimpleQuerqyQParserFactory);
        SimpleQuerqyQParserFactory qParserFactory = (SimpleQuerqyQParserFactory) factory;
        assertEquals(WhiteSpaceQuerqyParser.class, qParserFactory.querqyParserClass);

    }


    @Test
    public void testThatASimpleQuerqyQParserFactoryIsCreatedIfTheParserClassIsConfiguredAsAString() throws Exception {

        NamedList<String> args = mock(NamedList.class);
        when(args.get("parser")).thenReturn(DummyQuerqyParser.class.getName());
        ResourceLoader resourceLoader = new ClasspathResourceLoader(getClass().getClassLoader());

        final SolrQuerqyParserFactory factory = plugin.loadSolrQuerqyParserFactory(resourceLoader, args);

        assertNotNull(factory);
        assertTrue(factory instanceof SimpleQuerqyQParserFactory);
        SimpleQuerqyQParserFactory qParserFactory = (SimpleQuerqyQParserFactory) factory;
        assertEquals(DummyQuerqyParser.class, qParserFactory.querqyParserClass);

    }

    @Test
    public void testThatASimpleQuerqyQParserFactoryForAWhiteSpaceQuerqyParserIsCreatedIfThereIsNoParserConfig()
            throws Exception {

        NamedList<?> args = mock(NamedList.class);
        when(args.get("parser")).thenReturn(null);

        ResourceLoader resourceLoader = new ClasspathResourceLoader(getClass().getClassLoader());

        final SolrQuerqyParserFactory factory = plugin.loadSolrQuerqyParserFactory(resourceLoader, args);

        assertNotNull(factory);
        assertTrue(factory instanceof SimpleQuerqyQParserFactory);
        SimpleQuerqyQParserFactory qParserFactory = (SimpleQuerqyQParserFactory) factory;
        assertEquals(WhiteSpaceQuerqyParser.class, qParserFactory.querqyParserClass);



    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatInitShouldFailIfThereIsDeprecatedRewritersDefined() {
        NamedList<String> args = new NamedList<>();
        args.add("rewriters", "foo");
        plugin.init(args);
    }
}
