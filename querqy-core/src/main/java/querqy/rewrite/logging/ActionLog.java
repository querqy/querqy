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

import java.util.List;

public class ActionLog {

    private final String message;
    private final MatchLog match;
    private final List<InstructionLog> instructions;

    private ActionLog(final String message, final MatchLog match, final List<InstructionLog> instructions) {
        this.message = message;
        this.match = match;
        this.instructions = instructions;
    }

    public String getMessage() {
        return message;
    }

    public MatchLog getMatch() {
        return match;
    }

    public List<InstructionLog> getInstructions() {
        return instructions;
    }

    public static ActionLogBuilder builder() {
        return new ActionLogBuilder();
    }

    public static class ActionLogBuilder {

        private String message;
        private MatchLog match;
        private List<InstructionLog> instructions;

        public ActionLogBuilder message(final String message) {
            this.message = message;
            return this;
        }

        public ActionLogBuilder match(final MatchLog match) {
            this.match = match;
            return this;
        }

        public ActionLogBuilder instructions(final List<InstructionLog> instructions) {
            this.instructions = instructions;
            return this;
        }

        public ActionLog build() {
            return new ActionLog(message, match, instructions);
        }
    }
}
