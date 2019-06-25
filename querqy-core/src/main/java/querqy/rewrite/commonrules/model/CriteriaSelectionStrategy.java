package querqy.rewrite.commonrules.model;

import java.util.List;

/**
 * Select rules based on {@link Criteria}.
 *
 * @author René Kriegler, @renekrie
 */
public class CriteriaSelectionStrategy implements SelectionStrategy {

    private final Sorting sorting;
    private final Limit limit;
    private final List<FilterCriterion> filters;


    public CriteriaSelectionStrategy(final Criteria criteria) {
        sorting = criteria.getSorting();
        limit = criteria.getLimit();
        filters = criteria.getFilters();

    }

    @Override
    public TopRewritingActionCollector createTopRewritingActionCollector() {

        final int count = limit.getCount();
        if (count < 1 || !limit.isUseLevels()) {
            return new FlatTopRewritingActionCollector(sorting.getComparators(), count, filters);
        } else {
            return new TopLevelRewritingActionCollector(sorting.getComparators(), count, filters);
        }
    }

    public Sorting getSorting() {
        return sorting;
    }

    public Limit getLimit() {
        return limit;
    }

    public List<FilterCriterion> getFilters() {
        return filters;
    }
}
