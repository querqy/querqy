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

import querqy.rewriter.commonrules.model.Limit;

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
