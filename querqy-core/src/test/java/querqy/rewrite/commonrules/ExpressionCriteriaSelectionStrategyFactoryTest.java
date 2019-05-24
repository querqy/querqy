package querqy.rewrite.commonrules;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static querqy.rewrite.commonrules.SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.CriteriaSelectionStrategy;
import querqy.rewrite.commonrules.model.ExpressionFilterCriterion;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.SelectionStrategy;
import querqy.rewrite.commonrules.model.Sorting;
import querqy.rewrite.commonrules.model.TopRewritingActionCollector;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class ExpressionCriteriaSelectionStrategyFactoryTest {

    @Mock
    SearchEngineRequestAdapter requestAdapter;

    ExpressionCriteriaSelectionStrategyFactory factory;

    @Before
    public void setUp() {
        factory = new ExpressionCriteriaSelectionStrategyFactory();
    }

    @Test
    public void testThatDefaultSelectionStrategyIsUsedIfNoParamsAreSet() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(any())).thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertEquals(DEFAULT_SELECTION_STRATEGY, strategy);

    }

    @Test
    public void testThatFiltersAreTurnedIntoExpressions() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getIntegerRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter")))
                .thenReturn(new String[] {"expr1", "expr2"});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);
        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        assertThat((List<ExpressionFilterCriterion>) collector.getFilters(),
                containsInAnyOrder(filter("expr1"), filter("expr2")));


    }

    @Test
    public void testThatSortingIsTakenFromRequestParams() {

        when(requestAdapter.getRequestParam(eq("querqy.rules1.criteria.sort"))).thenReturn(Optional.of("x desc"));
        when(requestAdapter.getIntegerRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter")))
                .thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        final Comparator<Instructions> comparator = collector.getComparator();
        assertEquals(new Sorting("x", Sorting.SortOrder.DESC), comparator);

    }

    @Test
    public void testThatLimitIsTakenFromRequestParams() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getIntegerRequestParam(eq("querqy.rules1.criteria.limit"))).thenReturn(Optional.of(12));
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter")))
                .thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        assertEquals(collector.getLimit(), 12);

    }

    static FilterCriterionMatcher filter(final String filterExpression) {
        return new FilterCriterionMatcher(filterExpression);
    }

    static class FilterCriterionMatcher extends TypeSafeMatcher<ExpressionFilterCriterion> {

        final String expectedExpression;

        FilterCriterionMatcher(final String expectedExpression) {
            this.expectedExpression = expectedExpression;
        }

        @Override
        protected boolean matchesSafely(ExpressionFilterCriterion filterCriterion) {
            return Objects.equals(expectedExpression, filterCriterion.getExpression());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("expression=").appendValue(expectedExpression);
        }
    }
}