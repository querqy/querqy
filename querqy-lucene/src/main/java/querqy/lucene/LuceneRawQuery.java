/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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
import querqy.model.BooleanParent;
import querqy.model.ParsedRawQuery;
import querqy.model.QuerqyQuery;

public class LuceneRawQuery extends ParsedRawQuery<Query> {
    public LuceneRawQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated, final Query query) {
        super(parent, occur, isGenerated, query);
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent) {
        return new LuceneRawQuery(newParent, occur, isGenerated(), query);
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent, final boolean generated) {
        return new LuceneRawQuery(newParent, occur, generated, query);
    }
}
