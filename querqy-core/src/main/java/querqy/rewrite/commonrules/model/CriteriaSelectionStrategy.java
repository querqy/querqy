package querqy.rewrite.commonrules.model;

import java.util.Comparator;

public class CriteriaSelectionStrategy implements SelectionStrategy {

    protected final Comparator<Instructions> sortingComparator;
    protected final Criteria criteria;


    public CriteriaSelectionStrategy(final Criteria criteria) {
        this.sortingComparator = criteria.getSorting()
                .map(sorting -> (Comparator<Instructions>) sorting)
                .orElse(ConfigurationOrderSelectionStrategy.COMPARATOR);
        this.criteria = criteria;
    }


    @Override
    public TopRewritingActionCollector getTopRewritingActionCollector() {
        return new TopRewritingActionCollector(sortingComparator, criteria.getLimit(), criteria.getFilters());
    }


}
