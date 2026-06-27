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
package querqy.rewrite.regexreplace;

import querqy.regex.MatchResult.GroupMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Replacement {

    protected final float weight;
    protected final List<Symbol> symbols;

    protected Replacement(final List<Symbol> symbols, final float weight) {
        this.weight = weight;
        this.symbols = symbols;
    }


    public static Replacement build(final String input, final float weight) {
        return new Replacement(parse(input), weight);
    }

    public String apply(final Map<Integer, GroupMatch> groups) {
        return symbols.stream().map(symbol -> symbol.get(groups))
                .collect(Collectors.joining(""));
    }


    protected sealed interface Symbol permits CharSeq, Placeholder {
        CharSequence get(Map<Integer, GroupMatch> groups);
    }

    protected record CharSeq(CharSequence value) implements Symbol {
        @Override
        public CharSequence get(final Map<Integer, GroupMatch> groups) {
            return value;
        }
    }

    protected record Placeholder(int index) implements Symbol {

        @Override
        public CharSequence get(final Map<Integer, GroupMatch> groups) {
            GroupMatch groupMatch = groups.get(index);
            return groupMatch == null ? "" : groupMatch.match();
        }
    }

    protected static List<Symbol> parse(final String input) {
        Objects.requireNonNull(input);

        List<Symbol> result = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            //  Escape
            if (c == '\\') {
                if (i + 1 < input.length()) {
                    currentText.append(input.charAt(i + 1));
                    i += 2;
                } else {
                    currentText.append('\\');
                    i++;
                }
                continue;
            }

            // Placeholder ${digits}
            if (c == '$' && i + 1 < input.length() && input.charAt(i + 1) == '{') {

                int startDigits = i + 2;
                int j = startDigits;

                while (j < input.length() && Character.isDigit(input.charAt(j))) {
                    j++;
                }

                if (j > startDigits && j < input.length() && input.charAt(j) == '}') {

                    // flush text
                    if (!currentText.isEmpty()) {
                        result.add(new CharSeq(currentText.toString()));
                        currentText.setLength(0);
                    }

                    int index = Integer.parseInt(input.substring(startDigits, j));
                    result.add(new Placeholder(index));

                    i = j + 1;
                    continue;
                }
            }

            currentText.append(c);
            i++;
        }

        if (!currentText.isEmpty()) {
            result.add(new CharSeq(currentText.toString()));
        }

        return result;

    }





}
