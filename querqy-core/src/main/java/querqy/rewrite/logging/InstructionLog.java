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

public class InstructionLog {

    private final String type;
    private final String param;
    private final String value;

    private InstructionLog(final String type, final String param, final String value) {
        this.type = type;
        this.param = param;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Object getParam() {
        return param;
    }

    public String getValue() {
        return value;
    }

    public static InstructionLogBuilder builder() {
        return new InstructionLogBuilder();
    }

    public static class InstructionLogBuilder {

        private String type;
        private String param;
        private String value;

        public InstructionLogBuilder type(final String type) {
            this.type = type;
            return this;
        }

        public InstructionLogBuilder param(final String param) {
            this.param = param;
            return this;
        }

        public InstructionLogBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public InstructionLog build() {
            return new InstructionLog(type, param, value);
        }
    }
}
