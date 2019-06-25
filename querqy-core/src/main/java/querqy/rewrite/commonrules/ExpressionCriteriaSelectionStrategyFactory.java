package querqy.rewrite.commonrules;

import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExpressionCriteriaSelectionStrategyFactory implements SelectionStrategyFactory {

    public static final Limit DEFAULT_LIMIT = new Limit(-1, false);

    private final Pattern SORT_SPLIT_PARAM_PATTERN = Pattern.compile("[ ]+");

    @Override
    public SelectionStrategy createSelectionStrategy(final String rewriterId,
                                                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new CriteriaSelectionStrategy(retrieveCriteriaFromRequest(rewriterId, searchEngineRequestAdapter));
    }

    protected Criteria retrieveCriteriaFromRequest(final String rewriterId,
                                                final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final Sorting sorting = getSortingFromRequest(rewriterId, searchEngineRequestAdapter);

        final Limit limit = getLimitFromRequest(rewriterId, searchEngineRequestAdapter);

        final List<FilterCriterion> filterCriteria = getFilterCriteriaFromRequest(rewriterId,
                searchEngineRequestAdapter);

        return new Criteria(sorting, limit, filterCriteria);

    }

    protected Sorting getSortingFromRequest(final String rewriterId,
                                         final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return searchEngineRequestAdapter
                .getRequestParam(RuleSelectionParams.getSortParamName(rewriterId))
                .map(sortStr -> {

                    final String[] sortCriterion = SORT_SPLIT_PARAM_PATTERN.split(sortStr.trim());
                    if (sortCriterion.length == 2) {
                        if (sortCriterion[0].length() < 1) {
                            throw new IllegalArgumentException("Invalid value for rules.criteria.sort: " + sortStr);
                        }
                        return (Sorting) new PropertySorting(sortCriterion[0],
                                Sorting.SortOrder.fromString(sortCriterion[1]));
                    } else {
                        throw new IllegalArgumentException("Invalid value for rules.criteria.sort: " + sortStr);
                    }
                }).orElse(Sorting.DEFAULT_SORTING);
    }

    protected Limit getLimitFromRequest(final String rewriterId,
                                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return searchEngineRequestAdapter
                .getIntegerRequestParam(RuleSelectionParams.getLimitParamName(rewriterId))
                .map(count -> new Limit(count, searchEngineRequestAdapter.getBooleanRequestParam(
                        RuleSelectionParams.getIsUseLevelsForLimitParamName(rewriterId)).orElse(false)))
                .orElse(DEFAULT_LIMIT);



    }

    protected List<FilterCriterion> getFilterCriteriaFromRequest(final String rewriterId,
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
