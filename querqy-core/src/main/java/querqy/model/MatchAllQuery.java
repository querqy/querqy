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
package querqy.model;

import lombok.EqualsAndHashCode;

/**
 * A top-level {@link Query} matching all documents
 */
@EqualsAndHashCode(callSuper = true)
public class MatchAllQuery extends Clause<BooleanParent> implements QuerqyQuery<BooleanParent> {

    public MatchAllQuery() {
        this(null, Occur.SHOULD, false);
    }

    public MatchAllQuery(final boolean isGenerated) {
        this(null, Occur.SHOULD, isGenerated);
    }

    public MatchAllQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated) {

        super(parent, occur, isGenerated);

    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }


    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent) {
        return new MatchAllQuery(newParent, getOccur(), isGenerated());
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent, boolean generated) {
        return new MatchAllQuery(newParent, getOccur(), generated);
    }
}
