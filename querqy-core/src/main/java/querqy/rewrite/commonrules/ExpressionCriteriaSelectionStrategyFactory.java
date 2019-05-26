package querqy.rewrite.commonrules;

import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExpressionCriteriaSelectionStrategyFactory implements SelectionStrategyFactory {

    private final Pattern SORT_SPLIT_PARAM_PATTERN = Pattern.compile("[ ,]+");

    @Override
    public SelectionStrategy createSelectionStrategy(final String rewriterId,
                                                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        final Criteria criteria = retrieveCriteriaFromRequest(rewriterId, searchEngineRequestAdapter);
        return criteria.isEmpty() ? DEFAULT_SELECTION_STRATEGY : new CriteriaSelectionStrategy(criteria);
    }

    public Criteria retrieveCriteriaFromRequest(final String rewriterId,
                                                final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final Optional<Sorting> sorting = searchEngineRequestAdapter
                .getRequestParam(RuleSelectionParams.getSortParamName(rewriterId))
                .map(sortStr -> {

                    final String[] sortCriterion = SORT_SPLIT_PARAM_PATTERN.split(sortStr.trim());
                    if (sortCriterion.length == 2) {
                        if (sortCriterion[0].length() < 1) {
                            throw new IllegalArgumentException("Invalid value for rules.criteria.sort: " + sortStr);
                        }
                        return new Sorting(sortCriterion[0], Sorting.SortOrder.fromString(sortCriterion[1]));
                    } else {
                        throw new IllegalArgumentException("Invalid value for rules.criteria.sort: " + sortStr);
                    }
                });


        final Optional<Integer> limit = searchEngineRequestAdapter
                .getIntegerRequestParam(RuleSelectionParams.getLimitParamName(rewriterId));

        final List<FilterCriterion> filterCriteria = getFilterCriteriaFromRequest(rewriterId,
                searchEngineRequestAdapter);


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

    public List<FilterCriterion> getFilterCriteriaFromRequest(final String rewriterId,
                                                              final SearchEngineRequestAdapter
                                                                         searchEngineRequestAdapter) {

        return Arrays.stream(searchEngineRequestAdapter
                .getRequestParams(RuleSelectionParams.getFilterParamName(rewriterId)))
                .map(this::stringToFilterCriterion)
                .collect(Collectors.toList());
    }


    public FilterCriterion stringToFilterCriterion(final String s) {

        final String str = s.trim();
        if (str.length() < 1) {
            throw new IllegalArgumentException("Invalid criterion string: " + s);
        }

        return new ExpressionFilterCriterion(str);

    }

}
