/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.regex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CharacterClass {

    record Range(char from, char to) {
        boolean contains(final char c) {
            return c >= from && c <= to;
        }
    }

    final Set<Character> singles = new HashSet<>();
    final List<Range> ranges = new ArrayList<>();
    final List<CharacterClass> intersections = new ArrayList<>();
    boolean negated = false;

    boolean matches(final char c) {
        boolean base = singles.contains(c) || ranges.stream().anyMatch(r -> r.contains(c));

        for (final CharacterClass cc: intersections) {
            base &= cc.matches(c);
        }

        return negated != base;
    }
}
