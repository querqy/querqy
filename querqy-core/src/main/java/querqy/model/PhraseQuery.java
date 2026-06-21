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
 * <p>A phrase query: an ordered sequence of terms that must appear together (with
 * optional proximity slop) to match a document.</p>
 *
 * <p>Because {@link DisjunctionMaxQuery} and {@link BooleanQuery} both implement
 * {@link BooleanParent}, a {@code PhraseQuery} can appear as a clause inside
 * either — or it can be used standalone as a {@link QuerqyQuery} (e.g. wrapped in
 * a {@link BoostQuery}, or added as a filter to an {@link ExpandedQuery}).</p>
 */
public class PhraseQuery extends Clause<BooleanParent>
        implements QuerqyQuery<BooleanParent>, BooleanClause, DisjunctionMaxClause {

    private final List<String> terms;
    private final int slop;

    public PhraseQuery(final BooleanParent parent, final Occur occur, final boolean generated,
                       final List<String> terms, final int slop) {
        super(parent, occur, generated);
        if (terms == null || terms.isEmpty()) {
            throw new IllegalArgumentException("terms must not be null or empty");
        }
        if (slop < 0) {
            throw new IllegalArgumentException("slop must not be negative");
        }
        this.terms = terms;
        this.slop = slop;
    }

    public List<String> getTerms() {
        return terms;
    }

    public int getSlop() {
        return slop;
    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    // --- QuerqyQuery<BooleanParent> ---

    @Override
    public PhraseQuery clone(final BooleanParent newParent) {
        return new PhraseQuery(newParent, occur, generated, terms, slop);
    }

    @Override
    public PhraseQuery clone(final BooleanParent newParent, final boolean newGenerated) {
        return new PhraseQuery(newParent, occur, newGenerated, terms, slop);
    }

    // --- DisjunctionMaxClause ---

    @Override
    public DisjunctionMaxClause clone(final DisjunctionMaxQuery newParent, final boolean newGenerated) {
        return new PhraseQuery(newParent, occur, newGenerated, terms, slop);
    }

    // --- BooleanClause ---

    @Override
    public BooleanClause clone(final BooleanQuery newParent) {
        return new PhraseQuery(newParent, occur, generated, terms, slop);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final boolean newGenerated) {
        return new PhraseQuery(newParent, occur, newGenerated, terms, slop);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur newOccur) {
        return new PhraseQuery(newParent, newOccur, generated, terms, slop);
    }

    @Override
    public BooleanClause clone(final BooleanQuery newParent, final Occur newOccur, final boolean newGenerated) {
        return new PhraseQuery(newParent, newOccur, newGenerated, terms, slop);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        final PhraseQuery other = (PhraseQuery) obj;
        return slop == other.slop && Objects.equals(terms, other.terms);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + slop;
        result = 31 * result + Objects.hashCode(terms);
        return result;
    }

    @Override
    public String toString() {
        return "PhraseQuery{terms=" + terms + ", slop=" + slop + ", occur=" + occur + "}";
    }
}
