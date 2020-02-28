package querqy.rewrite.commonrules.select;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Limit;

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
    public void testThatDefaultSortOrderIsUsedIfNoParamsAreSet() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(any())).thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final CriteriaSelectionStrategy criteriaSelectionStrategy = (CriteriaSelectionStrategy) strategy;

        assertEquals(Sorting.DEFAULT_SORTING, criteriaSelectionStrategy.getSorting());

    }

    @Test
    public void testThatDefaultLimitIsUsedIfNoParamsAreSet() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(any())).thenReturn(new String[] {});


        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final CriteriaSelectionStrategy criteriaSelectionStrategy = (CriteriaSelectionStrategy) strategy;

        assertEquals(new Limit(-1, false), criteriaSelectionStrategy.getLimit());

    }

    @Test
    public void testThatEmptyFiltersAreUsedIfNoParamsAreSet() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(any())).thenReturn(new String[] {});


        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final CriteriaSelectionStrategy criteriaSelectionStrategy = (CriteriaSelectionStrategy) strategy;
        final List<FilterCriterion> filters = criteriaSelectionStrategy.getFilters();
        assertNotNull(filters);
        assertTrue(filters.isEmpty());

    }


    @Test
    public void testThatSortingIsTakenFromRequestParams() {

        when(requestAdapter.getRequestParam(eq("querqy.rules1.criteria.sort"))).thenReturn(Optional.of("x desc"));
        when(requestAdapter.getIntegerRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter"))).thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final CriteriaSelectionStrategy criteriaSelectionStrategy = (CriteriaSelectionStrategy) strategy;
        assertEquals(new PropertySorting("x", Sorting.SortOrder.DESC), criteriaSelectionStrategy.getSorting());

    }

    @Test
    public void testThatLimitWithoutLevelsIsTakenFromRequestParams() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getIntegerRequestParam(eq("querqy.rules1.criteria.limit"))).thenReturn(Optional.of(12));
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter")))
                .thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        assertTrue(collector instanceof FlatTopRewritingActionCollector);
        assertEquals(collector.getLimit(), 12);

    }

    @Test
    public void testThatLimitWithoutLevelIsTakenFromRequestParamsIfUseLevelsIsFalse() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getIntegerRequestParam(eq("querqy.rules1.criteria.limit"))).thenReturn(Optional.of(12));
        when(requestAdapter.getBooleanRequestParam(eq("querqy.rules1.criteria.limitByLevel")))
                .thenReturn(Optional.of(false));
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter"))).thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        assertTrue(collector instanceof FlatTopRewritingActionCollector);
        assertEquals(collector.getLimit(), 12);

    }

    @Test
    public void testThatLimitWithLevelIsTakenFromRequestParamsIfUseLevelsIsFalse() {

        when(requestAdapter.getRequestParam(any())).thenReturn(Optional.empty());
        when(requestAdapter.getIntegerRequestParam(eq("querqy.rules1.criteria.limit"))).thenReturn(Optional.of(1));
        when(requestAdapter.getBooleanRequestParam(eq("querqy.rules1.criteria.limitByLevel")))
                .thenReturn(Optional.of(true));
        when(requestAdapter.getRequestParams(eq("querqy.rules1.criteria.filter"))).thenReturn(new String[] {});

        final SelectionStrategy strategy = factory.createSelectionStrategy("rules1", requestAdapter);
        assertTrue(strategy instanceof CriteriaSelectionStrategy);

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();
        assertTrue(collector instanceof TopLevelRewritingActionCollector);
        assertEquals(collector.getLimit(), 1);

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