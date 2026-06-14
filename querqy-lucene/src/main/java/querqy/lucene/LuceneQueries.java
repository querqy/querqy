/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018 Querqy Contributors
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
package querqy.lucene;

import org.apache.lucene.search.Query;

import java.util.List;
import java.util.Objects;

public class LuceneQueries {

    public final Query mainQuery;
    public final List<Query> filterQueries;
    public final Query userQuery;
    public final List<Query> querqyBoostQueries;
    public final Query rankQuery;
    public final boolean areQueriesInterdependent;
    public final boolean isMainQueryBoosted;


    public LuceneQueries(final Query mainQuery, final List<Query> filterQueries, final List<Query> querqyBoostQueries,
                         final Query userQuery, final Query rankQuery, final boolean areQueriesInterdependent,
                         final boolean isMainQueryBoosted) {
        this.mainQuery = Objects.requireNonNull(mainQuery);
        this.filterQueries = filterQueries;
        this.querqyBoostQueries = querqyBoostQueries;
        this.userQuery =  Objects.requireNonNull(userQuery);
        this.rankQuery = rankQuery;
        this.areQueriesInterdependent = areQueriesInterdependent;
        this.isMainQueryBoosted = isMainQueryBoosted;
    }
}
