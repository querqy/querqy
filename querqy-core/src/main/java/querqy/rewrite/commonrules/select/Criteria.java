package querqy.rewrite.commonrules.select;


import querqy.rewrite.commonrules.model.Limit;

import java.util.List;
import java.util.Objects;

public class Criteria {

    private final Sorting sorting;
    private final Limit limit;
    private final List<FilterCriterion> filters;

    public Criteria(final Sorting sorting, final Limit limit, final List<FilterCriterion> filters) {
        this.sorting = Objects.requireNonNull(sorting);
        this.limit = Objects.requireNonNull(limit);
        this.filters = Objects.requireNonNull(filters);
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
