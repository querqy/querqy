package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.EscapeUtil.endsWithBoundary;
import static querqy.rewrite.commonrules.EscapeUtil.endsWithWildcard;
import static querqy.rewrite.commonrules.EscapeUtil.indexOfWildcard;
import static querqy.rewrite.commonrules.EscapeUtil.unescape;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import querqy.model.Clause;
import querqy.model.Clause.Occur;
import querqy.model.Input;
import querqy.model.ParametrizedRawQuery;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.parser.QuerqyParser;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostMethod;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.PrefixTerm;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;

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
                    return new SynonymInstruction(synonymTerms, boost);
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

        int pos = indexOfWildcard(s);
        if (pos > -1) {
            if (pos < (s.length() - 1)) {
                return new ValidationError(WILDCARD + " is only allowed at the end of the input: " + s);
            } else if (requiresRightBoundary) {
                return new ValidationError(WILDCARD + " cannot be combined with right boundary");
            }
        }
        final Object expr = parseTermExpression(s);
        if (expr instanceof ValidationError) {
            return expr;
        } else {
            return new Input.SimpleInput((List<Term>) expr, requiresLeftBoundary, requiresRightBoundary,
                    trimmed);
        }

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
