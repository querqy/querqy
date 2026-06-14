/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2015 Querqy Contributors
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
package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Set;

/**
 * Factory for {@link NumberConcatenationRewriter}
 */
public class NumberConcatenationRewriterFactory extends RewriterFactory {

    public static final int DEFAULT_MIN_LENGTH_OF_RESULTING_QUERY_TERM = 3;

    protected final boolean acceptGeneratedTerms;
    protected int minimumLengthOfResultingQueryTerm;

    public NumberConcatenationRewriterFactory(final String rewriterId) {

        this(rewriterId, false);
    }

    public NumberConcatenationRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms) {
        this(rewriterId, acceptGeneratedTerms, DEFAULT_MIN_LENGTH_OF_RESULTING_QUERY_TERM);
    }

    public NumberConcatenationRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms,
                                              final int minimumLengthOfResultingQueryTerm) {
        super(rewriterId);
        this.acceptGeneratedTerms = acceptGeneratedTerms;
        this.minimumLengthOfResultingQueryTerm = minimumLengthOfResultingQueryTerm;
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberConcatenationRewriter(acceptGeneratedTerms, minimumLengthOfResultingQueryTerm);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

}
