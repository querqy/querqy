package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;

public class CriteriaSelectionStrategyTest {

    @Test
    public void testThatInstructionsOrdIsDefaultSortOrder() {
        final CriteriaSelectionStrategy strategy =
                new CriteriaSelectionStrategy(new Criteria(null, 1, Collections.emptyList()));

        final Comparator<Instructions> comparator = strategy.getSortingComparator();

        final Instructions instructions1 = new Instructions(1);
        final Instructions instructions2 = new Instructions(2);

        assertEquals(0, comparator.compare(instructions1, instructions1));
        assertEquals(0, comparator.compare(instructions2, instructions2));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));

    }

    @Test
    public void testThatComparatorLimitAndFiltersArePassedToTopRewritingActionCollector() {
        final Sorting sorting = new Sorting("name1", Sorting.SortOrder.DESC);
        final FilterCriterion filter = new FilterCriterion("f1", "v1");
        final CriteriaSelectionStrategy strategy =
                new CriteriaSelectionStrategy(new Criteria(sorting, 17, Collections.singletonList(filter)));

        final TopRewritingActionCollector collector = strategy.createTopRewritingActionCollector();

        assertEquals(sorting, collector.getComparator());
        assertEquals(17, collector.getLimit());
        assertThat(collector.getFilters(), contains(filter));
    }

}
