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
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;

public class BoostedDelegatingFieldBoost implements FieldBoost {

    private final FieldBoost delegate;
    private final float boost;

    public BoostedDelegatingFieldBoost(final FieldBoost delegate, final float boost) {
        this.delegate = delegate;
        this.boost = boost;
    }

    @Override
    public float getBoost(final String fieldname, final IndexReader indexReader) throws IOException {
        final float computedBoost = delegate.getBoost(fieldname, indexReader);
        return computedBoost * boost;
    }

    @Override
    public void registerTermSubQuery(final TermSubQueryFactory termSubQueryFactory) {
        delegate.registerTermSubQuery(termSubQueryFactory);
    }

    @Override
    public String toString(final String fieldname) {
        return "^BoostedDelegatingFieldBoost(" 
            + delegate.toString(fieldname) 
            + "^" + boost + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoostedDelegatingFieldBoost other = (BoostedDelegatingFieldBoost) obj;
        if (!delegate.equals(other.delegate))
            return false;
        return Float.compare(boost, other.boost) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate.hashCode(), boost);
    }

}
