package querqy.rewrite.commonrules.select;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import querqy.rewrite.commonrules.model.Limit;

import java.util.Collections;

public class CriteriaSelectionStrategyTest {

    @Test
    public void testThatFlatCollectorIsUsedIfThereIsNotMaxCount() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(-1, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatFlatCollectorIsUsedIfCountIsZero() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(0, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatFlatCollectorIsUsedIfLevelsNotSetInLimit() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(1, false), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof FlatTopRewritingActionCollector);
    }

    @Test
    public void testThatLevelCollectorIsUsedIfLevelsSetInLimit() {
        final CriteriaSelectionStrategy strategy = new CriteriaSelectionStrategy(new Criteria(new PropertySorting("f1",
                Sorting.SortOrder.ASC), new Limit(1, true), Collections.emptyList()));
        final TopRewritingActionCollector topRewritingActionCollector = strategy.createTopRewritingActionCollector();
        assertTrue(topRewritingActionCollector instanceof TopLevelRewritingActionCollector);
    }

}
