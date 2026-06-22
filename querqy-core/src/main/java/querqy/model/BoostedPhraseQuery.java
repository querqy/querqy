/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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

import java.util.List;
import java.util.Objects;

/**
 * A {@link PhraseQuery} that carries an additional per-phrase boost factor.
 *
 * <p>In the visitor pattern this class dispatches to {@link NodeVisitor#visit(PhraseQuery)},
 * consistent with how {@link BoostedTerm} dispatches to {@link NodeVisitor#visit(Term)}.
 * The boost is read in {@code LuceneQueryBuilder#createLucenePhraseQuery} via an
 * {@code instanceof} check, so no new visitor method is required.</p>
 */
public class BoostedPhraseQuery extends PhraseQuery {

    private final float boost;

    public BoostedPhraseQuery(final BooleanParent parent, final Occur occur,
                               final String field, final List<String> terms, final int slop,
                               final float boost) {
        super(parent, occur, true, field, terms, slop);
        this.boost = boost;
    }

    public float getBoost() {
        return boost;
    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    // --- QuerqyQuery<BooleanParent> ---

    @Override
    public BoostedPhraseQuery clone(final BooleanParent newParent) {
        return new BoostedPhraseQuery(newParent, occur, getField(), getTerms(), getSlop(), boost);
    }

    @Override
    public BoostedPhraseQuery clone(final BooleanParent newParent, final boolean newGenerated) {
        return new BoostedPhraseQuery(newParent, occur, getField(), getTerms(), getSlop(), boost);
    }

    // --- DisjunctionMaxClause ---

    @Override
    public DisjunctionMaxClause clone(final DisjunctionMaxQuery newParent, final boolean newGenerated) {
        return new BoostedPhraseQuery(newParent, occur, getField(), getTerms(), getSlop(), boost);
    }

    // --- BooleanClause ---

    @Override
    public BooleanClause clone(final BooleanQuery newParent) {
        return new BoostedPhraseQuery(newParent, occur, getField(), getTerms(), getSlop(), boost);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final boolean newGenerated) {
        return new BoostedPhraseQuery(newParent, occur, getField(), getTerms(), getSlop(), boost);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur newOccur) {
        return new BoostedPhraseQuery(newParent, newOccur, getField(), getTerms(), getSlop(), boost);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur newOccur, final boolean newGenerated) {
        return new BoostedPhraseQuery(newParent, newOccur, getField(), getTerms(), getSlop(), boost);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        return Float.compare(boost, ((BoostedPhraseQuery) obj).boost) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boost);
    }

    @Override
    public String toString() {
        return "BoostedPhraseQuery{field=" + getField() + ", terms=" + getTerms()
                + ", slop=" + getSlop() + ", boost=" + boost + ", occur=" + occur + "}";
    }
}
