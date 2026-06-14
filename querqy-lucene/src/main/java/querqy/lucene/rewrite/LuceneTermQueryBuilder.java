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
package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import querqy.lucene.rewrite.DocumentFrequencyCorrection;
import querqy.lucene.rewrite.FieldBoost;
import querqy.lucene.rewrite.TermQueryBuilder;

import java.util.Optional;

public class LuceneTermQueryBuilder implements TermQueryBuilder {
    @Override
    public Optional<DocumentFrequencyCorrection> getDocumentFrequencyCorrection() {
        return Optional.empty();
    }

    @Override
    public TermQuery createTermQuery(final Term term, final FieldBoost boost) {
        return new TermQuery(term);
    }
}
