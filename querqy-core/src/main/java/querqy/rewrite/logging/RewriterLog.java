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

import java.util.LinkedList;
import java.util.List;

public class RewriterLog {

    private final boolean hasAppliedRewriting;
    private final List<ActionLog> actionLogs;

    private RewriterLog(final boolean hasAppliedRewriting, final List<ActionLog> actionLog) {
        this.hasAppliedRewriting = hasAppliedRewriting;
        this.actionLogs = actionLog;
    }

    public boolean hasAppliedRewriting() {
        return hasAppliedRewriting;
    }

    public List<ActionLog> getActionLogs() {
        return actionLogs;
    }

    public static RewriterLogBuilder builder() {
        return new RewriterLogBuilder();
    }

    public static class RewriterLogBuilder {

        private boolean hasAppliedRewriting;
        private List<ActionLog> actionLogs;

        public RewriterLogBuilder hasAppliedRewriting(final boolean hasAppliedRewriting) {
            this.hasAppliedRewriting = hasAppliedRewriting;
            return this;
        }

        public RewriterLogBuilder addActionLogs(final ActionLog actionLogs) {
            if (this.actionLogs == null) {
                this.actionLogs = new LinkedList<>();
            }

            this.actionLogs.add(actionLogs);
            return this;
        }

        public RewriterLogBuilder actionLogs(final List<ActionLog> actionLogs) {
            this.actionLogs = actionLogs;
            return this;
        }

        public RewriterLog build() {
            if (actionLogs == null) {
                actionLogs = List.of();
            }

            return new RewriterLog(hasAppliedRewriting, actionLogs);
        }
    }
}
