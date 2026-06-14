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
package querqy.rewrite;

import java.util.Collections;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;

/**
 * <p>A query rewriter.</p>
 * <p>Query rewriter implementations shall guarantee to preserve the number of top-level query clauses of the
 * {@link ExpandedQuery#userQuery} when rewriting the query.</p>
 * 
 * @author rene
 *
 */
public interface QueryRewriter {
    
    Set<Term> EMPTY_GENERABLE_TERMS = Collections.emptySet();

    /**
     * Rewrite the query. The caller of this method should expect that the query that was passed as an argument to this
     * method could be modified.
     *
     * @param query The query to be rewritten
     * @param searchEngineRequestAdapter Encapsulates the request context.
     * @return The rewritten query.
     *
     */
    RewriterOutput rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter);
}
