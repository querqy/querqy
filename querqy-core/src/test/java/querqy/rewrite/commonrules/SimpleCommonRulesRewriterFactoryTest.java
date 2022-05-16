package querqy.rewrite.commonrules;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class SimpleCommonRulesRewriterFactoryTest {


    @Mock
    QuerqyParserFactory querqyParserFactory;

    @Mock
    SelectionStrategyFactory defaultSelectionStrategyFactory;

    @Mock
    SelectionStrategyFactory namedSelectionStrategyFactory;


    @Mock
    SelectionStrategy defaultSelectionStrategy;

    @Mock
    SelectionStrategy namedSelectionStrategy;

    @Mock
    ExpandedQuery query;

    @Mock
    SearchEngineRequestAdapter requestAdapter;

    static final String STRATEGY_NAME1 = "strategy1";

    Map<String, SelectionStrategyFactory> namedStrategyFactories;


    @Before
    public void setUp() {
        namedStrategyFactories = new HashMap<>();
        namedStrategyFactories.put(STRATEGY_NAME1, namedSelectionStrategyFactory);
    }




    @Test(expected = NullPointerException.class)
    public void testThatDefaultSelectionStrategyFactoryMustBeSet() {
        try {
            new SimpleCommonRulesRewriterFactory("someId", new StringReader(""), true, false, querqyParserFactory, true,
                    namedStrategyFactories, null, true);
        } catch (final IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test(expected = IOException.class)
    public void testThatInvalidRulesTriggerException() throws IOException {
            new SimpleCommonRulesRewriterFactory("someId", new StringReader("This is not a parsable rule"),
                    true, false, querqyParserFactory, true, namedStrategyFactories, defaultSelectionStrategyFactory, true);
    }

    @Test
    public void testThatRulesAreParsed() throws IOException {
        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input =>\n DECORATE: deco1"), true, false, querqyParserFactory, true, namedStrategyFactories,
                defaultSelectionStrategyFactory, true);
        final RulesCollection rules = factory.getRules();
        assertEquals(1, rules.getInstructions().size());
    }

    @Test
    public void testCreateRewriterUsesDefaultSelectionStrategyFactory() throws IOException {
        when(defaultSelectionStrategyFactory.createSelectionStrategy(any(), any()))
                .thenReturn(defaultSelectionStrategy);
        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());

        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input =>\n DECORATE: deco1"), true, false, querqyParserFactory, true, namedStrategyFactories,
                defaultSelectionStrategyFactory, true);

        final QueryRewriter rewriter = factory.createRewriter(query, requestAdapter);
        assertTrue(rewriter instanceof CommonRulesRewriter);

        final CommonRulesRewriter commonRulesRewriter = (CommonRulesRewriter) rewriter;
        assertSame(defaultSelectionStrategy, commonRulesRewriter.selectionStrategy);
        assertSame(factory.getRules(), commonRulesRewriter.rules);

    }

    @Test
    public void testCreateRewriterSelectsSelectionStrategyFactoryByName() throws IOException {
        when(namedSelectionStrategyFactory.createSelectionStrategy(any(), any())).thenReturn(namedSelectionStrategy);
        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.of(STRATEGY_NAME1));

        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input =>\n DECORATE: deco1"), true, false, querqyParserFactory, true, namedStrategyFactories,
                defaultSelectionStrategyFactory, true);
        final QueryRewriter rewriter = factory.createRewriter(query, requestAdapter);
        assertTrue(rewriter instanceof CommonRulesRewriter);

        final CommonRulesRewriter commonRulesRewriter = (CommonRulesRewriter) rewriter;
        assertSame(namedSelectionStrategy, commonRulesRewriter.selectionStrategy);
        assertSame(factory.getRules(), commonRulesRewriter.rules);
        verify(defaultSelectionStrategyFactory, never()).createSelectionStrategy(any(), any());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRewriterDoesNotAcceptUnknownStrategyName() throws IOException {
        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.of(STRATEGY_NAME1 + "void"));

        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input =>\n DECORATE: deco1"), true, false, querqyParserFactory, true, namedStrategyFactories,
                defaultSelectionStrategyFactory, true);
        factory.createRewriter(query, requestAdapter);

    }

    @Test
    public void testThatGenerableTerms() throws IOException {

        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input1 =>\n SYNONYM: t1 t2\ninput2 =>\n SYNONYM: t3 t4"), true,
                false, querqyParserFactory, true, namedStrategyFactories, defaultSelectionStrategyFactory, true);

        final Set<Term> terms = factory.getCacheableGenerableTerms();

        assertThat(terms, Matchers.containsInAnyOrder(
                new Term(null, "t1"),
                new Term(null, "t2"),
                new Term(null, "t3"),
                new Term(null, "t4")));
    }
    
    @Test
    public void testThatTermCacheIsNotPopulated() throws IOException {

        final SimpleCommonRulesRewriterFactory factory = new SimpleCommonRulesRewriterFactory("someId",
                new StringReader("input1 =>\n SYNONYM: t1 t2\ninput2 =>\n SYNONYM: t3 t4"), true,
                false, querqyParserFactory, true, namedStrategyFactories, defaultSelectionStrategyFactory, false);

        final Set<Term> terms = factory.getCacheableGenerableTerms();

        assertThat(terms, Matchers.empty());
    }

}