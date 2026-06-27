/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.commonrules.select;

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
