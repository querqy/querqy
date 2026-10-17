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
package querqy.rewriter.commonrules.rules.property.skeleton;

import querqy.rewrite.RuleParseException;

import java.util.HashSet;
import java.util.Set;

/**
 * Rejects duplicate keys within a single JSON object literal (at any nesting depth), replicating the
 * rejection that the old, directly-configured Jackson {@code ObjectMapper} used to provide via
 * {@code JsonParser.Feature.STRICT_DUPLICATE_DETECTION}.
 * <p>
 * This runs as a lightweight structural scan over the already-normalized, strict JSON text (see
 * {@link LenientJsonNormalizer}), <em>before</em> it reaches a {@link querqy.json.JsonProvider}. By the time
 * a {@code JsonProvider} has returned a {@code Map}, an in-object duplicate key has already been silently
 * collapsed (e.g. last-key-wins for a plain Jackson {@code ObjectMapper}), and different {@code JsonProvider}
 * implementations are not guaranteed to agree on how it gets collapsed - so this check deliberately works on
 * the raw text and must not depend on any particular {@code JsonProvider}'s behavior.
 * <p>
 * The scan is lenient about anything that isn't itself a duplicate-key finding: malformed JSON is left for
 * the downstream {@code JsonProvider} to reject with its own, more specific syntax error.
 */
final class DuplicateJsonKeyChecker {

    private DuplicateJsonKeyChecker() {
    }

    /**
     * @throws RuleParseException if any JSON object literal in {@code json} contains the same key twice
     */
    static void check(final String json) {
        new Scanner(json).scanValue();
    }

    private static final class Scanner {

        private final String json;
        private final int n;
        private int pos;

        Scanner(final String json) {
            this.json = json;
            this.n = json.length();
        }

        void scanValue() {
            skipWhitespace();
            if (pos >= n) {
                return;
            }
            switch (json.charAt(pos)) {
                case '{':
                    scanObject();
                    break;
                case '[':
                    scanArray();
                    break;
                case '"':
                    scanStringLiteral();
                    break;
                default:
                    scanScalar();
            }
        }

        private void scanObject() {
            pos++; // consume '{'
            final Set<String> seenKeys = new HashSet<>();
            skipWhitespace();
            if (pos < n && json.charAt(pos) == '}') {
                pos++;
                return;
            }
            while (pos < n) {
                skipWhitespace();
                if (pos >= n || json.charAt(pos) != '"') {
                    return; // not well-formed; let the JsonProvider report the real syntax error
                }
                final String key = scanStringLiteral();
                if (!seenKeys.add(key)) {
                    throw new RuleParseException(
                            String.format("Duplicate key \"%s\" in JSON property object", key));
                }
                skipWhitespace();
                if (pos < n && json.charAt(pos) == ':') {
                    pos++;
                }
                scanValue();
                skipWhitespace();
                if (pos < n && json.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                if (pos < n && json.charAt(pos) == '}') {
                    pos++;
                }
                return;
            }
        }

        private void scanArray() {
            pos++; // consume '['
            skipWhitespace();
            if (pos < n && json.charAt(pos) == ']') {
                pos++;
                return;
            }
            while (pos < n) {
                scanValue();
                skipWhitespace();
                if (pos < n && json.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                if (pos < n && json.charAt(pos) == ']') {
                    pos++;
                }
                return;
            }
        }

        /** Scans a well-formed double-quoted JSON string starting at the current (opening-quote) position. */
        private String scanStringLiteral() {
            final StringBuilder sb = new StringBuilder();
            pos++; // consume opening quote
            while (pos < n) {
                final char c = json.charAt(pos);
                if (c == '\\' && pos + 1 < n) {
                    sb.append(c).append(json.charAt(pos + 1));
                    pos += 2;
                } else if (c == '"') {
                    pos++;
                    return sb.toString();
                } else {
                    sb.append(c);
                    pos++;
                }
            }
            return sb.toString(); // unterminated; let the JsonProvider report the real syntax error
        }

        private void scanScalar() {
            while (pos < n) {
                final char c = json.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                pos++;
            }
        }

        private void skipWhitespace() {
            while (pos < n && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
