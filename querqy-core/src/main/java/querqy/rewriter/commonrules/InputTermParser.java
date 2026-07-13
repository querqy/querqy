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
package querqy.rewriter.commonrules;

import static querqy.rewriter.commonrules.EscapeUtil.endsWithBoundary;
import static querqy.rewriter.commonrules.EscapeUtil.endsWithWildcard;
import static querqy.rewriter.commonrules.EscapeUtil.indexOfWildcard;
import static querqy.rewriter.commonrules.EscapeUtil.unescape;
import static querqy.rewriter.commonrules.SpecialChars.BOUNDARY;
import static querqy.rewriter.commonrules.SpecialChars.WILDCARD;

import querqy.model.Input;
import querqy.rewriter.commonrules.model.PrefixTerm;
import querqy.rewriter.commonrules.model.SuffixTerm;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewrite.rules.RuleParseException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Parses a rule's input pattern (the left-hand side of a rule, e.g. {@code abc *def}) into an
 * {@link Input.SimpleInput}, and parses the individual terms of that pattern.
 */
public class InputTermParser {

    public static Input.SimpleInput parseInput(final String inputString) {

        boolean requiresLeftBoundary = false;
        boolean requiresRightBoundary = false;

        final String trimmed = inputString.trim();
        String s = trimmed;
        if (s.length() > 0 && s.charAt(0) == BOUNDARY) {
            requiresLeftBoundary = true;
            s = s.substring(1).trim();
        }

        if (endsWithBoundary(s)) {
            requiresRightBoundary = true;
            s = s.substring(0, s.length() - 1).trim();
        }

        validateWildcardPlacement(s, requiresLeftBoundary, requiresRightBoundary);

        final List<Term> terms = parseTermExpression(s);
        validateSuffixTermFieldNames(terms, s);

        return new Input.SimpleInput(terms, requiresLeftBoundary, requiresRightBoundary, trimmed);

    }

    /**
     * A rule input containing a leading-wildcard term ({@link SuffixTerm}) does not support field names on any
     * *other* term of the same input: the fixed terms surrounding the wildcard are matched via a dedicated,
     * field-name-agnostic lookup (see {@code querqy.rewrite.lookup.triemap.suffix}), so a field restriction on one
     * of them cannot currently be honored. Reject explicitly rather than silently ignoring the restriction.
     */
    private static void validateSuffixTermFieldNames(final List<Term> terms, final String s) {

        final boolean hasSuffixTerm = terms.stream().anyMatch(SuffixTerm.class::isInstance);
        if (!hasSuffixTerm) {
            return;
        }

        final boolean hasFieldNamesOnOtherTerm = terms.stream()
                .anyMatch(term -> !(term instanceof SuffixTerm) && term.hasFieldNames());
        if (hasFieldNamesOnOtherTerm) {
            throw new RuleParseException(
                    "Field names are not supported on terms other than the wildcard term itself in an input "
                            + "with a leading wildcard: " + s);
        }
    }

    /**
     * Validates that the input string contains at most one wildcard and that it is placed either at the very
     * start of a term (leading wildcard, e.g. {@code *hemd}, allowed on any term of the input) or at the very
     * end of the whole input (trailing wildcard, e.g. {@code shirt*}, only allowed on the last term - unchanged
     * legacy restriction).
     */
    static void validateWildcardPlacement(final String s, final boolean requiresLeftBoundary,
                                          final boolean requiresRightBoundary) {

        final int pos = indexOfWildcard(s);
        if (pos == -1) {
            return;
        }

        if (indexOfWildcard(s.substring(pos + 1)) > -1) {
            throw new RuleParseException("Only one " + WILDCARD + " is allowed per rule input: " + s);
        }

        final boolean atStartOfTerm = (pos == 0) || Character.isWhitespace(s.charAt(pos - 1));
        final boolean atEndOfTerm = (pos == s.length() - 1) || Character.isWhitespace(s.charAt(pos + 1));

        if (atStartOfTerm && atEndOfTerm) {
            throw new RuleParseException("Missing term text for wildcard " + WILDCARD + ": " + s);
        }

        if (atEndOfTerm) {
            if (pos < (s.length() - 1)) {
                throw new RuleParseException(WILDCARD + " is only allowed at the end of the input: " + s);
            }
            if (requiresRightBoundary) {
                throw new RuleParseException(WILDCARD + " cannot be combined with right boundary");
            }
            return;
        }

        if (atStartOfTerm) {
            if (pos == 0 && requiresLeftBoundary) {
                throw new RuleParseException(WILDCARD + " cannot be combined with left boundary");
            }
            return;
        }

        throw new RuleParseException(WILDCARD + " is only allowed at the start or end of a term: " + s);
    }

    static List<Term> parseTermExpression(final String s) {

        final int len = s.length();

        if (len == 1) {
            final char ch = s.charAt(0);
            if (ch == WILDCARD) {
                throw new RuleParseException("Missing prefix for wildcard " + WILDCARD);
            }
            final Term term = new Term(new char[]{ch}, 0, 1, null);
            return Collections.singletonList(term);
        }

        final List<Term> terms = new LinkedList<>();

        for (final String part : s.split("\\s+")) {
            if (part.length() > 0) {
                terms.add(parseTerm(part));
            }
        }

        return terms;

    }

    public static Term parseTerm(final String s) {

        final int len = s.length();

        if (len == 1) {
            final char ch = s.charAt(0);
            if (ch == WILDCARD) {
                throw new IllegalArgumentException("Missing prefix for wildcard " + WILDCARD);
            }
            return new Term(new char[]{ch}, 0, 1, null);
        }

        final int pos = s.indexOf(':');

        final boolean fieldNamesPossible = (pos > 0 && pos < (len - 1));

        final List<String> fieldNames = fieldNamesPossible ? parseFieldNames(s.substring(0, pos)) : null;

        final String remaining = fieldNamesPossible ? s.substring(pos + 1).trim() : s;
        if (fieldNamesPossible && remaining.length() == 1 && remaining.charAt(0) == WILDCARD) {
            throw new IllegalArgumentException("Missing prefix for wildcard " + WILDCARD);
        }

        if (remaining.charAt(0) == WILDCARD) {
            final String suffixUnescaped = unescape(remaining.substring(1));
            return new SuffixTerm(suffixUnescaped.toCharArray(), 0, suffixUnescaped.length(), fieldNames);
        }

        final String remainingUnescaped = unescape(remaining);

        return endsWithWildcard(remaining)
                ? new PrefixTerm(remainingUnescaped.toCharArray(), 0, remainingUnescaped.length() - 1, fieldNames)
                : new Term(remainingUnescaped.toCharArray(), 0, remainingUnescaped.length(), fieldNames);

    }

    public static List<String> parseFieldNames(final String s) {

        final int len = s.length();

        if (len == 1) {
            return Collections.singletonList(s);
        }

        final List<String> result = new LinkedList<>();

        if (s.charAt(0) == '{' && s.charAt(len - 1) == '}') {
            if (len > 2) {
                final String[] parts = s.substring(1, len - 1).split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.length() > 0) {
                        result.add(part);
                    }
                }
            }

        } else {
            result.add(s);
        }

        return result;

    }

}
