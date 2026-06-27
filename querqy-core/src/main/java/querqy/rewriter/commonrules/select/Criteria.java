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
