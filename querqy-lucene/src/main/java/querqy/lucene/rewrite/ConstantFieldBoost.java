/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2016 Querqy Contributors
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

import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Created by rene on 11/01/2016.
 */
public class ConstantFieldBoost implements FieldBoost {

    public static final ConstantFieldBoost NORM_BOOST = new ConstantFieldBoost(1f);

    final float boost;

    public ConstantFieldBoost(float boost) { this.boost = boost; }

    @Override
    public float getBoost(String fieldname, IndexReader indexReader)
            throws IOException {
        return boost;
    }

    @Override
    public void registerTermSubQuery(final TermSubQueryFactory termSubQueryFactory) {
    }

    @Override
    public String toString(String fieldname) {
        return "^ConstantFieldBoost(" + fieldname + "^" + boost + ")";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConstantFieldBoost that = (ConstantFieldBoost) o;

        return Float.compare(that.boost, boost) == 0;

    }

    @Override
    public int hashCode() {
        return (boost != +0.0f ? Float.floatToIntBits(boost) : 0);
    }
}
