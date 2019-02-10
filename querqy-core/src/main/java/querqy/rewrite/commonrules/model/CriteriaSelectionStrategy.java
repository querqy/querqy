package querqy.rewrite.commonrules.model;

import java.util.Comparator;

public class CriteriaSelectionStrategy implements SelectionStrategy {

    private final Comparator<Instructions> sortingComparator;
    private final Criteria criteria;


    public CriteriaSelectionStrategy(final Criteria criteria) {
        this.sortingComparator = criteria.getSorting()
                .map(sorting -> (Comparator<Instructions>) sorting)
                .orElse(ConfigurationOrderSelectionStrategy.COMPARATOR);
        this.criteria = criteria;
    }


    @Override
    public TopRewritingActionCollector createTopRewritingActionCollector() {
        return new TopRewritingActionCollector(sortingComparator, criteria.getLimit(), criteria.getFilters());
    }

    public Comparator<Instructions> getSortingComparator() {
        return sortingComparator;
    }


}
