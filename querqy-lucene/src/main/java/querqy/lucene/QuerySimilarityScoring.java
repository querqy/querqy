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

import querqy.lucene.rewrite.DependentTermQueryBuilder;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.FieldBoostTermQueryBuilder;
import querqy.lucene.rewrite.SimilarityTermQueryBuilder;
import querqy.lucene.rewrite.TermQueryBuilder;

public enum QuerySimilarityScoring {

    DFC(dfc -> dfc == null
            ? new DependentTermQueryBuilder(new DocumentFrequencyCorrection())
            : new DependentTermQueryBuilder(dfc)),

    SIMILARITY_SCORE_OFF(dfc ->  new FieldBoostTermQueryBuilder()),

    SIMILARITY_SCORE_ON(dfc -> new SimilarityTermQueryBuilder());

    private TermQueryBuilderFactory termQueryBuilderFactory;

    QuerySimilarityScoring(final TermQueryBuilderFactory termQueryBuilderFactory) {
        this.termQueryBuilderFactory = termQueryBuilderFactory;
    }

    TermQueryBuilder createTermQueryBuilder(final DocumentFrequencyCorrection dfc) {
        return termQueryBuilderFactory.createTermQueryBuilder(dfc);
    }
}
