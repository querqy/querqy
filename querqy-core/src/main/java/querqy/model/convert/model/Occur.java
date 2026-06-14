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
package querqy.model.convert.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import querqy.model.Clause;
import querqy.model.convert.QueryBuilderException;

@AllArgsConstructor
public enum Occur {
    MUST("must", Clause.Occur.MUST),
    SHOULD("should", Clause.Occur.SHOULD),
    MUST_NOT("must_not", Clause.Occur.MUST_NOT);

    private static final Map<String, Occur> MAP_FROM_TYPE_NAME = new HashMap<>(3);
    static {
        MAP_FROM_TYPE_NAME.put(SHOULD.typeName, SHOULD);
        MAP_FROM_TYPE_NAME.put(MUST.typeName, MUST);
        MAP_FROM_TYPE_NAME.put(MUST_NOT.typeName, MUST_NOT);
    }

    public final String typeName;
    public final Clause.Occur objectForClause;

    public static Occur getOccurByTypeName(final String typeName) {
        return MAP_FROM_TYPE_NAME.computeIfAbsent(typeName, key -> {
            throw new QueryBuilderException(String.format("Occur of type %s is unknown", typeName));
        });
    }

    public static Occur getOccurByClauseObject(final Clause.Occur clauseObject) {

        if (SHOULD.objectForClause == clauseObject) {
            return SHOULD;

        } else if (MUST.objectForClause == clauseObject) {
            return MUST;

        } else if (MUST_NOT.objectForClause == clauseObject) {
            return MUST_NOT;

        }

        throw new QueryBuilderException(String.format("Occur of type %s is unknown", clauseObject.toString()));
    }


}
