/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewriter.commonrules.select.booleaninput.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BooleanInputElement {

    public final String term;
    public final Type type;

    public BooleanInputElement(String term, Type type) {
        this.term = term;
        this.type = type;
    }

    public enum Type {
        OR("OR", 4),
        AND("AND", 3),
        NOT("NOT", 2),
        TERM("TERM", 1),
        LEFT_PARENTHESIS("(", -1),
        RIGHT_PARENTHESIS(")", -1);

        private final String name;
        private final int priority;

        Type(final String name, final int priority) {
            this.name = name;
            this.priority = priority;
        }

        public static Type getType(final String name) {
            return TYPE_MAP.getOrDefault(name, TERM);
        }

        public String getName() {
            return name;
        }

        public int getPriority() {
            return priority;
        }
    }

    private static final Map<String, Type> TYPE_MAP = Arrays.stream(Type.values())
            .collect(Collectors.toMap(Type::getName, v -> v));

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanInputElement that = (BooleanInputElement) o;
        return Objects.equals(term, that.term) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, type);
    }

    @Override
    public String toString() {
        return "Element{" +
                "term='" + term + '\'' +
                ", type=" + type +
                '}';
    }



}
