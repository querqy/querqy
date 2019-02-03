package querqy.rewrite.commonrules.model;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Criteria {

    private final Sorting sorting;
    private final int limit;
    private final List<FilterCriterion> filters;


    public Criteria(final Sorting sorting, final int limit, final List<FilterCriterion> filters) {
        this.sorting = sorting;
        this.limit = (limit < 0) ? -1 : limit;
        this.filters = Objects.requireNonNull(filters);
    }

    public Criteria(final Sorting sorting, final List<FilterCriterion> filters) {
        this(sorting, -1, filters);
    }

    public Criteria(final List<FilterCriterion> filters) {
        this(null, -1, filters);
    }

    public Criteria(final int limit, final List<FilterCriterion> filters) {
        this(null, limit, filters);
    }

    public boolean isEmpty() {
        return sorting == null && limit == -1 && filters.isEmpty();
    }

    public Optional<Sorting> getSorting() {
        return Optional.ofNullable(sorting);
    }

    public int getLimit() {
        return limit;
    }

    public List<FilterCriterion> getFilters() {
        return filters;
    }

}
