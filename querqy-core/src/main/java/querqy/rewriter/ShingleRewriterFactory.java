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
package querqy.rewriter;

import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Set;

/**
 * Factory for {@link ShingleRewriter}
 */
public class ShingleRewriterFactory extends RewriterFactory {

    protected final boolean acceptGeneratedTerms;

    public ShingleRewriterFactory(final String rewriterId) {

        this(rewriterId, false);
    }

    public ShingleRewriterFactory(final String rewriterId, final boolean acceptGeneratedTerms) {
        super(rewriterId);
        this.acceptGeneratedTerms = acceptGeneratedTerms;
    }

    @Override
    public QueryRewriter createRewriter(SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new ShingleRewriter(acceptGeneratedTerms);
    }

    @Override
    public Set<Term> getCacheableGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }

    public boolean isAcceptGeneratedTerms() {
        return acceptGeneratedTerms;
    }
}
