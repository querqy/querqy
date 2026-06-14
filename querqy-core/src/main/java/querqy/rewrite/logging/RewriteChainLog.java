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
package querqy.rewrite.logging;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class RewriteChainLog {

    private final List<RewriteLogEntry> rewriteChain;

    private RewriteChainLog(final List<RewriteLogEntry> rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    public List<RewriteLogEntry> getRewriteChain() {
        return rewriteChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RewriteChainLog that = (RewriteChainLog) o;
        return Objects.equals(rewriteChain, that.rewriteChain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rewriteChain);
    }

    @Override
    public String toString() {
        return "RewriteChainLog{" +
                "rewriteChain=" + rewriteChain +
                '}';
    }

    public static class RewriteLogEntry {

        private final String rewriterId;
        private final List<ActionLog> actions;

        private RewriteLogEntry(final String rewriterId, final List<ActionLog> actions) {
            this.rewriterId = rewriterId;
            this.actions = actions;
        }

        public String getRewriterId() {
            return rewriterId;
        }

        public List<ActionLog> getActions() {
            return actions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RewriteLogEntry that = (RewriteLogEntry) o;
            return Objects.equals(rewriterId, that.rewriterId) && Objects.equals(actions, that.actions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rewriterId, actions);
        }

        @Override
        public String toString() {
            return "RewriteLogEntry{" +
                    "rewriterId='" + rewriterId + '\'' +
                    ", actions=" + actions +
                    '}';
        }
    }

    public static RewriteChainLogBuilder builder() {
        return new RewriteChainLogBuilder();
    }

    public static class RewriteChainLogBuilder {

        private final List<RewriteLogEntry> rewriteChain = new LinkedList<>();

        public RewriteChainLogBuilder add(final String rewriterId) {
            return add(rewriterId, Collections.emptyList());
        }

        public RewriteChainLogBuilder add(final String rewriterId, final List<ActionLog> actions) {
            rewriteChain.add(new RewriteLogEntry(rewriterId, actions));
            return this;
        }

        public RewriteChainLog build() {
            return new RewriteChainLog(rewriteChain);
        }
    }

}
