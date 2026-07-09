/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import querqy.model.Clause;
import querqy.model.Clause.Occur;
import querqy.model.Input;
import querqy.model.ParametrizedRawQuery;
import querqy.model.PhraseQuery;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.parser.QuerqyParser;
import querqy.rewrite.rules.instruction.InstructionParser;
import querqy.rewriter.commonrules.model.BoostInstruction;
import querqy.rewriter.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewriter.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewriter.commonrules.model.DecorateInstruction;
import querqy.rewriter.commonrules.model.DeleteInstruction;
import querqy.rewriter.commonrules.model.FilterInstruction;
import querqy.rewriter.commonrules.model.InstructionDescription;
import querqy.rewriter.commonrules.model.PrefixTerm;
import querqy.rewriter.commonrules.model.SuffixTerm;
import querqy.rewriter.commonrules.model.SynonymInstruction;
import querqy.rewriter.commonrules.model.Term;

/**
 * @author René Kriegler, @renekrie
 */
@Deprecated
public class LineParser {

    public static final char BOUNDARY = '"';
    public static final char WILDCARD = '*';

    public static final String INSTR_BOOST_DOWN = "down";
    public static final String INSTR_BOOST_UP = "up";
    public static final String INSTR_DECORATE = "decorate";
    public static final String INSTR_DELETE = "delete";
    public static final String INSTR_FILTER = "filter";
    public static final String INSTR_SYNONYM = "synonym";


    static final char RAWQUERY = '*';

    public static Object parse(final String line, final Input inputPattern,
                               final QuerqyParserFactory querqyParserFactory) {
        return parse(line, inputPattern, querqyParserFactory, BoostMethod.ADDITIVE);
    }

    public static Object parse(final String line, final Input inputPattern,
                               final QuerqyParserFactory querqyParserFactory,
                               final BoostMethod boostMethod) {


        if (line.endsWith("=>")) {

            final String trimmed = line.substring(0, line.length() - 2).trim();
            return (trimmed.isEmpty())
                    ? new ValidationError("Empty input")
                    : new InputString(trimmed);

        }

        if (inputPattern == null ) {
            return new ValidationError("Missing input for instruction");
        }

        final String lcLine = line.toLowerCase(Locale.ROOT).trim();
        final List<Term> inputTerms = inputPattern.getInputTerms();

        if (lcLine.startsWith(INSTR_DELETE)) {

            if (inputTerms.isEmpty()) {
                return new ValidationError("DELETE instruction is not allowed for boolean input");
            } else {

                if (lcLine.length() == 6) {
                    return new DeleteInstruction(inputTerms);
                }

                String instructionTerms = line.substring(6).trim();
                if (instructionTerms.charAt(0) != ':') {
                    return new ValidationError("Cannot parse line: " + line);
                }

                if (instructionTerms.length() == 1) {
                    return new DeleteInstruction(inputTerms);
                }

                instructionTerms = instructionTerms.substring(1).trim();
                final Object expr = parseTermExpression(instructionTerms);
                if (expr instanceof ValidationError) {
                    return new ValidationError("Cannot parse line: " + line + " : " + ((ValidationError) expr)
                            .getMessage());
                }
                @SuppressWarnings("unchecked")
                final List<Term> deleteTerms = (List<Term>) expr;
                for (final Term term : deleteTerms) {
                    if (term.findFirstMatch(inputTerms) == null) {
                        return new ValidationError("Condition doesn't contain the term to delete: " + term);
                    }
                }

                return new DeleteInstruction(deleteTerms);

            }

        }

        if (lcLine.startsWith(INSTR_FILTER)) {

            if (lcLine.length() == 6) {
                return new ValidationError("Cannot parse line: " + line);
            }

            String filterString = line.substring(6).trim();
            if (filterString.charAt(0) != ':') {
                return new ValidationError("Cannot parse line: " + line);
            }

            filterString = filterString.substring(1).trim();
            if (filterString.length() == 0) {
                return new ValidationError("Cannot parse line: " + line);
            }

            if (filterString.charAt(0) == RAWQUERY) {
                if (filterString.length() == 1) {
                    return new ValidationError("Missing raw query after * in line: " + line);
                }

                final String rawQueryString = filterString.substring(1).trim();
                try {
                    return new FilterInstruction(parseRawQuery(rawQueryString, Occur.MUST));
                } catch (RuleParseException e) {
                    return new ValidationError(e.getMessage());
                }

            } else if (InstructionParser.isPhraseValue(filterString)) {
                try {
                    return new FilterInstruction(InstructionParser.parsePhraseQuery(filterString, Occur.MUST));
                } catch (querqy.rewrite.rules.RuleParseException e) {
                    return new ValidationError(e.getMessage());
                }
            } else if (querqyParserFactory == null) {
                return new ValidationError("No querqy parser factory to parse filter query. Prefix '*' if you want " +
                        "to pass this line as a raw query String to your search engine. Line: " + line);
            } else {
                QuerqyParser parser = querqyParserFactory.createParser();
                return new FilterInstruction(parser.parse(filterString));
            }
        }

        //BoostMethod boostMethod = multiplicativeBoosts ? BoostMethod.MULTIPLICATIVE : BoostMethod.ADDITIVE;

        if (lcLine.startsWith(INSTR_BOOST_DOWN)) {
            return parseBoostInstruction(line, lcLine, 4, BoostDirection.DOWN, boostMethod, querqyParserFactory);
        }

        if (lcLine.startsWith(INSTR_BOOST_UP)) {
            return parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, boostMethod, querqyParserFactory);
        }

        if (lcLine.startsWith(INSTR_SYNONYM)) {

            if (inputTerms.isEmpty()) {
                return new ValidationError("SYNONYM instruction is not allowed for boolean input");
            } else {

                if (lcLine.length() == 7) {
                    return new ValidationError("Cannot parse line: " + line);
                }

                String synonymString = line.substring(7).trim();
                float boost = SynonymInstruction.DEFAULT_TERM_BOOST;

                // check for boost (optional)
                if (synonymString.charAt(0) == '(') {
                    synonymString = synonymString.substring(1).trim();
                    int boostEndBracket = synonymString.indexOf(')');
                    if (boostEndBracket < 0) {
                        return new ValidationError("Cannot parse line. No closing bracket found: " + line);
                    }
                    try {
                        boost = Float.parseFloat(synonymString.substring(0, boostEndBracket));
                    } catch (final NumberFormatException e) {
                        return new ValidationError("Cannot parse line. Invalid boost in: " + line + ". Threw: "
                                + e.getMessage());
                    }

                    if (boost < 0) {
                        return new ValidationError("Cannot parse line. Negative boost not allowed: " + line);
                    }
                    synonymString = synonymString.substring(boostEndBracket + 1).trim();
                }

                if (synonymString.charAt(0) != ':') {
                    return new ValidationError("Cannot parse line, ':' expetcted in " + line);
                }

                synonymString = synonymString.substring(1).trim();
                if (synonymString.length() == 0) {
                    return new ValidationError("Cannot parse line: " + line);
                }

                List<Term> synonymTerms = new LinkedList<>();
                for (String token : synonymString.split("\\s+")) {
                    if (token.length() > 0) {
                        Term term = parseTerm(token);
                        if (term.getMaxPlaceHolderRef() > 1) {
                            return new ValidationError("Max. wild card reference is 1: " + line);
                        }
                        synonymTerms.add(term);
                    }
                }
                if (synonymTerms.isEmpty()) {
                    // should never happen
                    return new ValidationError("Cannot parse line: " + line);
                } else {
                    return new SynonymInstruction(synonymTerms, boost, InstructionDescription.empty());
                }
            }
        }

        if (lcLine.startsWith(INSTR_DECORATE)) {
            return parseDecorateInstruction(line);
        }

        return line;

    }

    protected static RawQuery parseRawQuery(final String rawQuery, final Clause.Occur occur) throws RuleParseException {
        final List<String> rawQueryParts = Arrays.asList(rawQuery.split("%%", -1));

        final int size = rawQueryParts.size();

        if (size % 2 == 0) {
            throw new RuleParseException("Invalid use of parametrization in the definition of a RawQuery. " +
                    "Parameters must begin and end with %%");
        }

        if (size == 1) {
            return new StringRawQuery(null, rawQuery, occur, false);
        } else {
            final List<ParametrizedRawQuery.Part> parts = IntStream.range(0, size)
                    .mapToObj(index -> index % 2 == 0
                            ? new ParametrizedRawQuery.Part(rawQueryParts.get(index),
                                ParametrizedRawQuery.Part.Type.QUERY_PART)
                            : new ParametrizedRawQuery.Part(rawQueryParts.get(index),
                                ParametrizedRawQuery.Part.Type.PARAMETER))
                    .collect(Collectors.toList());

            return new ParametrizedRawQuery(null, parts, occur, false);
        }
    }

    private static final String DECORATE_ERROR_MESSAGE_TEMPLATE = "Invalid decorate rule %s. Decorate rules must " +
            "either be defined only with a value, e.g. DECORATE: value, or with a key surrounded by brackets and a " +
            "value, e.g. DECORATE(key): value.";

    private static String createDecorateErrorMessage(final String line) {
        return String.format(DECORATE_ERROR_MESSAGE_TEMPLATE, line);
    }

    private static final Pattern DECORATE_KEY_PATTERN = Pattern.compile("^\\(([\\w\\d_]+)\\):");

    public static Object parseDecorateInstruction(String line) {
        if (line.length() == INSTR_DECORATE.length()) {
            return new ValidationError(createDecorateErrorMessage(line));
        }

        String decKey = null;
        String linePart = line.substring(INSTR_DECORATE.length()).trim();

        if (linePart.charAt(0) == '(') {
            final Matcher matcher = DECORATE_KEY_PATTERN.matcher(linePart);
            if (matcher.find()) {
                decKey = matcher.group(1);
                linePart = linePart.substring(decKey.length() + 2);
            } else {
                return new ValidationError(createDecorateErrorMessage(line));
            }
        }

        if (linePart.charAt(0) != ':') {
            return new ValidationError(createDecorateErrorMessage(line));
        }

        final String decValue = linePart.substring(1).trim();
        if (decValue.length() == 0) {
            return new ValidationError(createDecorateErrorMessage(line));
        }

        return new DecorateInstruction(decKey, decValue);

    }

    public static Object parseBoostInstruction(final String line, final String lcLine, final int lengthPredicate,
                                               final BoostDirection direction, final BoostMethod boostMethod,
                                               final QuerqyParserFactory querqyParserFactory) {

        if (lcLine.length() == lengthPredicate) {
            return new ValidationError("Cannot parse line: " + line);
        }

        String boostLine = line.substring(lengthPredicate).trim();
        char ch = boostLine.charAt(0);
        switch (ch) {

            case '(':
                if (line.length() < 5) {
                    return new ValidationError("Cannot parse line, expecting boost factor and ':' after '(' in " +
                            line);
                }
                break;

            case ':':
                if (line.length() == 1) {
                    return new ValidationError("Query expected: " + line);
                }
                break;

            default:
                return new ValidationError("Cannot parse line, '(' or ':' expected: " + line);
        }


        boostLine = boostLine.substring(1).trim();

        float boost = 1f;
        if (ch == '(') {
            int pos = boostLine.indexOf(')');
            if (pos < 1 || (pos == boostLine.length() - 1)) {
                return new ValidationError("Cannot parse line: " + line);
            }
            boost = Float.parseFloat(boostLine.substring(0, pos));
            boostLine = boostLine.substring(pos + 1).trim();
            if (boostLine.charAt(0) != ':') {
                return new ValidationError("Query expected: " + line);
            }
            boostLine = boostLine.substring(1).trim();
        }

        if (boostLine.length() == 0) {
            return new ValidationError("Query expected: " + line);
        }

        if (boostLine.charAt(0) == RAWQUERY) {

            if (boostLine.length() == 1) {
                return new ValidationError("Missing raw query after " + RAWQUERY + " in line: " + line);
            }

            final String rawQueryString = boostLine.substring(1).trim();
            try {
                return new BoostInstruction(parseRawQuery(rawQueryString, Occur.SHOULD), direction, boostMethod, boost);
            } catch (RuleParseException e) {
                return new ValidationError(e.getMessage());
            }

        } else if (InstructionParser.isPhraseValue(boostLine)) {
            try {
                final PhraseQuery phraseQuery = InstructionParser.parsePhraseQuery(boostLine, Occur.SHOULD);
                return new BoostInstruction(phraseQuery, direction, boostMethod, boost);
            } catch (querqy.rewrite.rules.RuleParseException e) {
                return new ValidationError(e.getMessage());
            }

        } else if (querqyParserFactory == null) {

            return new ValidationError("No querqy parser factory to parse filter query. Prefix '" + RAWQUERY +
                    "' you want to pass this line as a raw query String to your search engine. Line: " + line);

        } else {
            QuerqyParser parser = querqyParserFactory.createParser();
            return new BoostInstruction(parser.parse(boostLine), direction, boostMethod, boost);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object parseInput(final String inputString) {

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

        final Optional<ValidationError> wildcardValidation =
                validateWildcardPlacement(s, requiresLeftBoundary, requiresRightBoundary);
        if (wildcardValidation.isPresent()) {
            return wildcardValidation.get();
        }

        final Object expr = parseTermExpression(s);
        if (expr instanceof ValidationError) {
            return expr;
        }

        final List<Term> terms = (List<Term>) expr;
        final Optional<ValidationError> fieldNameValidation = validateSuffixTermFieldNames(terms, s);
        if (fieldNameValidation.isPresent()) {
            return fieldNameValidation.get();
        }

        return new Input.SimpleInput(terms, requiresLeftBoundary, requiresRightBoundary, trimmed);

    }

    /**
     * A rule input containing a leading-wildcard term ({@link SuffixTerm}) does not support field names on any
     * *other* term of the same input: the fixed terms surrounding the wildcard are matched via a dedicated,
     * field-name-agnostic lookup (see {@code querqy.rewrite.lookup.triemap.suffix}), so a field restriction on one
     * of them cannot currently be honored. Reject explicitly rather than silently ignoring the restriction.
     */
    private static Optional<ValidationError> validateSuffixTermFieldNames(final List<Term> terms, final String s) {

        final boolean hasSuffixTerm = terms.stream().anyMatch(SuffixTerm.class::isInstance);
        if (!hasSuffixTerm) {
            return Optional.empty();
        }

        final boolean hasFieldNamesOnOtherTerm = terms.stream()
                .anyMatch(term -> !(term instanceof SuffixTerm) && term.hasFieldNames());
        if (hasFieldNamesOnOtherTerm) {
            return Optional.of(new ValidationError(
                    "Field names are not supported on terms other than the wildcard term itself in an input "
                            + "with a leading wildcard: " + s));
        }

        return Optional.empty();
    }

    /**
     * Validates that the input string contains at most one wildcard and that it is placed either at the very
     * start of a term (leading wildcard, e.g. {@code *hemd}, allowed on any term of the input) or at the very
     * end of the whole input (trailing wildcard, e.g. {@code shirt*}, only allowed on the last term - unchanged
     * legacy restriction).
     *
     * @return a {@link ValidationError} if the input is invalid, or an empty {@link Optional} if the wildcard
     * placement is fine (including the case where there is no wildcard at all).
     */
    static Optional<ValidationError> validateWildcardPlacement(final String s, final boolean requiresLeftBoundary,
                                                               final boolean requiresRightBoundary) {

        final int pos = indexOfWildcard(s);
        if (pos == -1) {
            return Optional.empty();
        }

        if (indexOfWildcard(s.substring(pos + 1)) > -1) {
            return Optional.of(new ValidationError("Only one " + WILDCARD + " is allowed per rule input: " + s));
        }

        final boolean atStartOfTerm = (pos == 0) || Character.isWhitespace(s.charAt(pos - 1));
        final boolean atEndOfTerm = (pos == s.length() - 1) || Character.isWhitespace(s.charAt(pos + 1));

        if (atStartOfTerm && atEndOfTerm) {
            return Optional.of(new ValidationError("Missing term text for wildcard " + WILDCARD + ": " + s));
        }

        if (atEndOfTerm) {
            if (pos < (s.length() - 1)) {
                return Optional.of(new ValidationError(WILDCARD + " is only allowed at the end of the input: " + s));
            }
            if (requiresRightBoundary) {
                return Optional.of(new ValidationError(WILDCARD + " cannot be combined with right boundary"));
            }
            return Optional.empty();
        }

        if (atStartOfTerm) {
            if (pos == 0 && requiresLeftBoundary) {
                return Optional.of(new ValidationError(WILDCARD + " cannot be combined with left boundary"));
            }
            return Optional.empty();
        }

        return Optional.of(new ValidationError(WILDCARD + " is only allowed at the start or end of a term: " + s));
    }

    static Object parseTermExpression(final String s) {

        int len = s.length();

        if (len == 1) {
            char ch = s.charAt(0);
            if (ch == WILDCARD) {
                return new ValidationError("Missing prefix for wildcard " + WILDCARD);
            }
            Term term = new Term(new char[]{ch}, 0, 1, null);
            return Collections.singletonList(term);
        }


        List<Term> terms = new LinkedList<>();


        for (String part : s.split("\\s+")) {
            if (part.length() > 0) {
                terms.add(parseTerm(part));
            }
        }

        return terms;

    }

    public static Term parseTerm(String s) {

        int len = s.length();

        if (len == 1) {
            char ch = s.charAt(0);
            if (ch == WILDCARD) {
                throw new IllegalArgumentException("Missing prefix for wildcard " + WILDCARD);
            }
            return new Term(new char[]{ch}, 0, 1, null);
        }

        int pos = s.indexOf(':');

        boolean fieldNamesPossible = (pos > 0 && pos < (len - 1));

        List<String> fieldNames = fieldNamesPossible ? parseFieldNames(s.substring(0, pos)) : null;

        String remaining = fieldNamesPossible ? s.substring(pos + 1).trim() : s;
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

    public static List<String> parseFieldNames(String s) {

        int len = s.length();

        if (len == 1) {
            return Collections.singletonList(s);
        }


        List<String> result = new LinkedList<>();

        if (s.charAt(0) == '{' && s.charAt(len - 1) == '}') {
            if (len > 2) {
                String[] parts = s.substring(1, len - 1).split(",");
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
