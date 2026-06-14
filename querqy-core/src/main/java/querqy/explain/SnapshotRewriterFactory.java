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
package querqy.explain;

import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Map;

public class SnapshotRewriterFactory extends RewriterFactory {

    final static String NAME = "__EXPLAIN_SNAPSHOT";

    private SnapshotRewriter rewriter = null;

    private final String previousRewriterId;

    public SnapshotRewriterFactory(final String previousRewriterId) {
        super(NAME + "/" + previousRewriterId);
        this.previousRewriterId = previousRewriterId;
    }

    @Override
    public synchronized QueryRewriter createRewriter(final ExpandedQuery input,
                                                     final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        if (rewriter != null) {
            throw new IllegalStateException("This factory can only be used once!");
        }
        this.rewriter = new SnapshotRewriter();
        return rewriter;
    }

    public Map<String, Object> getSnapshot() {
        return rewriter.getSnapshot();
    }

    public String getPreviousRewriterId() {
        return previousRewriterId;
    }
}
