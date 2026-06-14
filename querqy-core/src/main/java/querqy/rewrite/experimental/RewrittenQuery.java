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
package querqy.rewrite.experimental;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import querqy.model.convert.builder.ExpandedQueryBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

@Setter
@RequiredArgsConstructor
public class RewrittenQuery {

    private final ExpandedQueryBuilder expandedQueryBuilder;

    private Set<Object> decorations;
    private Map<String, Object> namedDecorations;

    public ExpandedQueryBuilder getQuery() {
        return expandedQueryBuilder;
    }

    public Set<Object> getDecorations() {
        return isNull(decorations) ? Collections.emptySet() : decorations;
    }

    public Map<String, Object> getNamedDecorations() {
        return isNull(namedDecorations) ? Collections.emptyMap() : namedDecorations;
    }

}
