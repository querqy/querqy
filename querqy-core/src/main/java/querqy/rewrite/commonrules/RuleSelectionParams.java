package querqy.rewrite.commonrules;

public interface RuleSelectionParams {

    String PARAM_QUERQY_PREFIX = "querqy.";
    String PARAM_SUFFIX_SELECTION_STRATEGY = ".criteria.strategy";
    String PARAM_SUFFIX_SELECTION_LIMIT = ".criteria.limit";
    String PARAM_SUFFIX_SELECTION_USE_LEVELS_FOR_LIMIT = ".criteria.limitByLevel";
    String PARAM_SUFFIX_SELECTION_SORT = ".criteria.sort";
    String PARAM_SUFFIX_SELECTION_FILTER = ".criteria.filter";

    static String getStrategyParamName(final String rewriterId) {
        return getParamName(rewriterId, PARAM_SUFFIX_SELECTION_STRATEGY);
    }

    static String getLimitParamName(final String rewriterId) {
        return getParamName(rewriterId, PARAM_SUFFIX_SELECTION_LIMIT);
    }

    static String getSortParamName(final String rewriterId) {
        return getParamName(rewriterId, PARAM_SUFFIX_SELECTION_SORT);
    }

    static String getFilterParamName(final String rewriterId) {
        return getParamName(rewriterId, PARAM_SUFFIX_SELECTION_FILTER);
    }

    static String getIsUseLevelsForLimitParamName(final String rewriterId) {
        return getParamName(rewriterId, PARAM_SUFFIX_SELECTION_USE_LEVELS_FOR_LIMIT);
    }

    static String getParamName(final String rewriterId, final String suffix) {
        return PARAM_QUERQY_PREFIX + rewriterId + suffix;
    }




}
