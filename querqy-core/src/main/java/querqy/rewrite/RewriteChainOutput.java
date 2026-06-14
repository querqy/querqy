/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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

import querqy.model.ExpandedQuery;
import querqy.rewrite.logging.RewriteChainLog;

import java.util.Optional;

public class RewriteChainOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriteChainLog rewriteLog;

    private RewriteChainOutput(final ExpandedQuery expandedQuery, final RewriteChainLog rewriteLog) {
        this.expandedQuery = expandedQuery;
        this.rewriteLog = rewriteLog;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    // TODO: needed to be optional?
    public Optional<RewriteChainLog> getRewriteLog() {
        return Optional.ofNullable(rewriteLog);
    }

    public static RewriteChainOutputBuilder builder() {
        return new RewriteChainOutputBuilder();
    }

    public static class RewriteChainOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriteChainLog rewriteLog;

        public RewriteChainOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriteChainOutputBuilder rewriteLog(final RewriteChainLog rewriteLog) {
            this.rewriteLog = rewriteLog;
            return this;
        }

        public RewriteChainOutput build() {
            return new RewriteChainOutput(expandedQuery, rewriteLog);
        }
    }
}
