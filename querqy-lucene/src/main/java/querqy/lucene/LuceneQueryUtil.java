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
package querqy.lucene;

import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import querqy.LowerCaseCharSequence;

/**
 * Created by rene on 09/04/2016.
 */
public interface LuceneQueryUtil {

    /**
     * Wrap a query with a {@link BoostQuery} if the boost factor doesn't equal 1.
     *
     * @param query The query to boost
     * @param boostFactor The boost factor
     * @return A BoostQuery if boostFactor != 1 or the original query in all other cases.
     */
    static Query boost(final Query query, final float boostFactor) {
        return boostFactor == 1f ? query : new BoostQuery(query, boostFactor);
    }

    static ValueSource queryToValueSource(final Query query) {
        return (query instanceof FunctionQuery)
                ? ((FunctionQuery)query).getValueSource()
                : new QueryValueSource(query, 1.0f);
    }

    static DoubleValuesSource queryToDoubleValueSource(final Query query) {
        return (query instanceof FunctionScoreQuery)
                ? ((FunctionScoreQuery)query).getSource()
                : DoubleValuesSource.fromQuery(query);
    }

    static org.apache.lucene.index.Term toLuceneTerm(final String fieldname, final CharSequence value,
                                                            final boolean lowerCaseInput) {

        final BytesRef bytesRef = (value instanceof LowerCaseCharSequence || !lowerCaseInput)
                ? new BytesRef(value)
                : new BytesRef(new LowerCaseCharSequence(value));

        return new org.apache.lucene.index.Term(fieldname, bytesRef);

    }
}
