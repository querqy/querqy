package querqy.rewrite.commonrules;

import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.SelectionStrategyFactory;
import querqy.rewrite.commonrules.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CriteriaSelectionStrategyFactory implements SelectionStrategyFactory {

    @Override
    public SelectionStrategy createSelectionStrategy(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final Criteria criteria = retrieveCriteriaFromRequest(searchEngineRequestAdapter);
        return criteria.isEmpty() ? DEFAULT_SELECTION_STRATEGY : new CriteriaSelectionStrategy(criteria);
    }

    public Criteria retrieveCriteriaFromRequest(final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final Optional<Sorting> sorting = searchEngineRequestAdapter
                .getRequestParam("rules.criteria.sort")
                .map(sortStr -> {
                    String[] sortCriterion = sortStr.split("\\s+"); // FIXME: precompile regex
                    if (sortCriterion.length == 2) {
                        // FIXME: check for empty name
                        return new Sorting(sortCriterion[0], Sorting.SortOrder.fromString(sortCriterion[1]));
                    } else {
                        throw new IllegalArgumentException("Invalid value for rules.criteria.sort: " + sortStr);
                    }
                });


        final Optional<Integer> limit = searchEngineRequestAdapter
                .getRequestParam("rules.criteria.limit")
                .map(Integer::valueOf);

        final List<FilterCriterion> filterCriteria = Arrays.stream(searchEngineRequestAdapter
                .getRequestParams("rules.criteria.filter"))
                .map(filterStr -> {
                    // FIXME: check for empty name/value
                    // FIXME: precompile regex
                    final String[] filterArr = filterStr.split(":");
                    if (filterArr.length == 2) {
                        return new FilterCriterion(filterArr[0].trim(),
                                TypeDetector.getTypedObjectFromString(filterArr[1].trim()));
                    } else {
                        throw new IllegalArgumentException("Invalid value for rules.criteria.filter: " + filterStr);
                    }
                })
                .collect(Collectors.toList());


        // This isn't nice but Java guide lines tell us that we shouldn't use Optional as method arguments
        if (limit.isPresent()) {
            return sorting
                    .map(sort -> new Criteria(sort, limit.get(), filterCriteria))
                    .orElseGet(() -> new Criteria(limit.get(), filterCriteria));
        } else {
            return sorting
                    .map(sort -> new Criteria(sort, filterCriteria))
                    .orElseGet(() -> new Criteria(filterCriteria));
        }


    }
}
