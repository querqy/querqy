/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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
package querqy.rewrite.lookup.model;

import querqy.rewrite.commonrules.model.TermMatches;

import java.util.Objects;

public class Match<T> {

    private final TermMatches termMatches;
    private final T value;

    private Match(final TermMatches termMatches, final T value) {
        this.termMatches = termMatches;
        this.value = value;
    }

    public TermMatches getTermMatches() {
        return termMatches;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match<?> match = (Match<?>) o;
        return Objects.equals(termMatches, match.termMatches) && Objects.equals(value, match.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(termMatches, value);
    }

    @Override
    public String toString() {
        return "Match{" +
                "termMatches=" + termMatches +
                ", value=" + value +
                '}';
    }

    public static <T> Match<T> of(final TermMatches termMatches, final T value) {
        return new Match<>(termMatches, value);
    }
}
