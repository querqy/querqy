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
package querqy.rewriter.commonrules.model;

import java.util.Optional;

public class InstructionDescription {

    private final String typeName;
    private final Object param;
    private final String value;

    private InstructionDescription(final String typeName, final Object param, final String value) {
        this.typeName = typeName;
        this.param = param;
        this.value = value;
    }

    public String getTypeName() {
        return typeName;
    }

    public Optional<Object> getParam() {
        return Optional.ofNullable(param);
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public String toString() {
        return "InstructionDescription{" +
                "typeName='" + typeName + '\'' +
                ", param=" + param +
                ", value='" + value + '\'' +
                '}';
    }

    public static InstructionDescription empty() {
        return InstructionDescription.builder().typeName("").build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String typeName;
        private Object param;
        private String value;

        public Builder typeName(final String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder param(final Object param) {
            this.param = param;
            return this;
        }

        public Builder value(final String value) {
            this.value = value;
            return this;
        }

        public InstructionDescription build() {
            return new InstructionDescription(typeName, param, value);
        }
    }
}
