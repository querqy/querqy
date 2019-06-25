package querqy.rewrite.commonrules;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import querqy.rewrite.commonrules.model.Criteria;
import querqy.rewrite.commonrules.model.CriteriaSelectionStrategy;
import querqy.rewrite.commonrules.model.FlatTopRewritingActionCollector;
import querqy.rewrite.commonrules.model.Limit;
import querqy.rewrite.commonrules.model.PropertySorting;
import querqy.rewrite.commonrules.model.Sorting;
import querqy.rewrite.commonrules.model.TopLevelRewritingActionCollector;
import querqy.rewrite.commonrules.model.TopRewritingActionCollector;

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
