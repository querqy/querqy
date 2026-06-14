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
package querqy.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParametrizedRawQuery extends RawQuery {
    private final List<Part> parts;

    public ParametrizedRawQuery(final BooleanParent parent,
                                final List<Part> parts,
                                final Occur occur,
                                final boolean isGenerated) {
        super(parent, occur, isGenerated);
        this.parts = parts;
    }

    public String buildQueryString(final Function<String, String> parameterRewriter) {
        return parts.stream()
                .map(part -> part.type == Part.Type.PARAMETER ? parameterRewriter.apply(part.part) : part.part)
                .collect(Collectors.joining());
    }

    public List<Part> getParts() {
        return this.parts;
    }

    @Override
    public RawQuery clone(final BooleanParent newParent) {
        return clone(newParent, this.generated);
    }

    @Override
    public RawQuery clone(final BooleanParent newParent, final boolean generated) {
        return new ParametrizedRawQuery(newParent, parts, occur, generated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ParametrizedRawQuery that = (ParametrizedRawQuery) o;
        return Objects.equals(parts, that.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parts);
    }

    @Override
    public String toString() {
        return "RawQuery [parts=" + parts + "]";
    }

    public static class Part {
        public enum Type {
            QUERY_PART, PARAMETER
        }

        public final String part;
        public final Type type;

        public Part(String part, Type type) {
            this.part = part;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Part part1 = (Part) o;
            return Objects.equals(part, part1.part) &&
                    type == part1.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(part, type);
        }

        @Override
        public String toString() {
            return "(" + part + ", " + type + ")";
        }
    }
}
