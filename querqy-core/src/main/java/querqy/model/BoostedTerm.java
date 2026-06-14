/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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

import java.util.Objects;

public class BoostedTerm extends Term {

    private final float boost;

    public BoostedTerm(final DisjunctionMaxQuery parentQuery, final String field, final CharSequence value,
            float boost) {
        super(parentQuery, field, value, true);
        
        this.boost = boost;
    }

    public BoostedTerm(final DisjunctionMaxQuery parentQuery, final CharSequence value, float boost) {
        this(parentQuery, null, value, boost);
    }

    public float getBoost() {
        return boost;
    }

    @Override
    public <T> T accept(final NodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return ((field == null) ? "*" : field) + "(" + boost + "):" + getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boost);
    }
}
