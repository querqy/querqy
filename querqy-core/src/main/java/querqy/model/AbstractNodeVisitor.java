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
 * @author René Kriegler, @renekrie
 *
 */
public abstract class AbstractNodeVisitor<T> implements NodeVisitor<T> {
    
    @Override
    public T visit(final Query query) {
        for (final BooleanClause clause : query.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final MatchAllQuery query) {
        return null;
    }

    @Override
    public T visit(final DisjunctionMaxQuery disjunctionMaxQuery) {
        for (final DisjunctionMaxClause clause : disjunctionMaxQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final BooleanQuery booleanQuery) {
        for (final BooleanClause clause : booleanQuery.getClauses()) {
            clause.accept(this);
        }
        return null;
    }

    @Override
    public T visit(final Term term) {
        return null;
    }

    @Override
    public T visit(final RawQuery rawQuery) {
        return null;
    }

    @Override
    public T visit(final PhraseQuery phraseQuery) {
        return null;
    }

}
