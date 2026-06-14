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
import querqy.rewrite.logging.RewriterLog;

import java.util.Optional;

public class RewriterOutput {

    private final ExpandedQuery expandedQuery;
    private final RewriterLog rewriterLog;

    private RewriterOutput(final ExpandedQuery expandedQuery, final RewriterLog rewriterLog) {
        this.expandedQuery = expandedQuery;
        this.rewriterLog = rewriterLog;
    }

    public ExpandedQuery getExpandedQuery() {
        return expandedQuery;
    }

    public Optional<RewriterLog> getRewriterLog() {
        return Optional.ofNullable(rewriterLog);
    }

    public static RewriterOutputBuilder builder() {
        return new RewriterOutputBuilder();
    }

    public static class RewriterOutputBuilder {

        private ExpandedQuery expandedQuery;
        private RewriterLog rewriterLog;

        public RewriterOutputBuilder expandedQuery(final ExpandedQuery expandedQuery) {
            this.expandedQuery = expandedQuery;
            return this;
        }

        public RewriterOutputBuilder rewriterLog(final RewriterLog rewriterLog) {
            this.rewriterLog = rewriterLog;
            return this;
        }

        public RewriterOutput build() {
            return new RewriterOutput(expandedQuery, rewriterLog);
        }
    }
}
