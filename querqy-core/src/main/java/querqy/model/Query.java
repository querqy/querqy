/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
package querqy.model;

/**
 * The top-level query as entered by the user or rewritten by the rewrite chain.
 *
 * @author René Kriegler, @renekrie
 */
public class Query extends BooleanQuery {

    public Query() {
        this(false);
    }

    public Query(final boolean generated) {
        super(null, Occur.SHOULD, generated);
    }

    @Override
    public Query clone(final BooleanParent newParent, final Occur occur, final boolean generated) {
        final Query q = new Query(generated);
        for (final BooleanClause clause : clauses) {
            q.addClause(clause.clone(q, generated));
        }
        return q;

    }

    public boolean isEmpty() {
        return clauses.isEmpty();
    }

}
