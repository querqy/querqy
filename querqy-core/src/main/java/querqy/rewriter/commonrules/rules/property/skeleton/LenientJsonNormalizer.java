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

/**
 * Normalizes the CommonRules "property" block's lenient, JSON5-ish dialect into strict JSON text, so the
 * actual parsing can be delegated to a plain {@link querqy.json.JsonProvider} - no JSON library needs to
 * understand this dialect itself. Replicates exactly the three syntax-leniency features the property
 * parser's old, directly-configured Jackson {@code ObjectMapper} used to grant (not Jackson's
 * {@code STRICT_DUPLICATE_DETECTION}, which is a semantic duplicate-key *rejection*, not a syntax leniency
 * - that stays as application-level validation in {@link PropertySkeletonParser} itself):
 * <ul>
 *     <li>{@code #}-to-end-of-line comments (YAML-style - never {@code //} or {@code /* *}{@code /});</li>
 *     <li>single-quoted strings (converted to double-quoted);</li>
 *     <li>unquoted object keys (bare identifiers immediately followed by {@code :}).</li>
 * </ul>
 * Correctly tracks "inside a string literal" state throughout, so none of the above misfires on
 * comment-like or quote-like characters that are themselves part of a string value.
 */
final class LenientJsonNormalizer {

    private LenientJsonNormalizer() {
    }

    static String normalize(final String input) {
        final StringBuilder out = new StringBuilder(input.length());
        final int n = input.length();
        int i = 0;
        while (i < n) {
            final char c = input.charAt(i);
            if (c == '#') {
                while (i < n && input.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '"') {
                i = copyDoubleQuotedString(input, i, out);
            } else if (c == '\'') {
                i = convertSingleQuotedString(input, i, out);
            } else if (isIdentifierStart(c)) {
                i = copyIdentifierOrQuoteIfKey(input, i, out);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Copies a double-quoted JSON string through unchanged (it's already valid), honoring escapes so an
     * escaped quote ({@code \"}) doesn't terminate the string early. Returns the position just after the
     * closing quote. */
    private static int copyDoubleQuotedString(final String input, final int start, final StringBuilder out) {
        final int n = input.length();
        int i = start;
        out.append(input.charAt(i));
        i++;
        while (i < n) {
            final char c = input.charAt(i);
            out.append(c);
            if (c == '\\' && i + 1 < n) {
                out.append(input.charAt(i + 1));
                i += 2;
            } else {
                i++;
                if (c == '"') {
                    break;
                }
            }
        }
        return i;
    }

    /** Converts a single-quoted string to a double-quoted one: {@code \'} becomes a literal {@code '}, and
     * any literal {@code "} inside must be escaped, since double quotes are now the delimiter. Returns the
     * position just after the closing single quote. */
    private static int convertSingleQuotedString(final String input, final int start, final StringBuilder out) {
        final int n = input.length();
        int i = start + 1;
        out.append('"');
        while (i < n) {
            final char c = input.charAt(i);
            if (c == '\\' && i + 1 < n) {
                final char next = input.charAt(i + 1);
                if (next == '\'') {
                    out.append('\'');
                } else {
                    out.append('\\').append(next);
                }
                i += 2;
            } else if (c == '\'') {
                i++;
                break;
            } else if (c == '"') {
                out.append("\\\"");
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        out.append('"');
        return i;
    }

    /** A bareword immediately followed (after optional whitespace) by {@code :} is an unquoted object key -
     * quote it. Any other bareword (a JSON literal like {@code true}/{@code false}/{@code null}, or just an
     * unexpected token the downstream parser will reject) is copied through unchanged. Returns the position
     * just after the identifier. */
    private static int copyIdentifierOrQuoteIfKey(final String input, final int start, final StringBuilder out) {
        final int n = input.length();
        int end = start;
        while (end < n && isIdentifierPart(input.charAt(end))) {
            end++;
        }
        final String identifier = input.substring(start, end);

        int afterWhitespace = end;
        while (afterWhitespace < n && Character.isWhitespace(input.charAt(afterWhitespace))) {
            afterWhitespace++;
        }

        if (afterWhitespace < n && input.charAt(afterWhitespace) == ':') {
            out.append('"').append(identifier).append('"');
        } else {
            out.append(identifier);
        }
        return end;
    }

    private static boolean isIdentifierStart(final char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(final char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
