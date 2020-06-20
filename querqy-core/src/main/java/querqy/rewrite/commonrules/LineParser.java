package querqy.rewrite.commonrules;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import querqy.model.Clause.Occur;
import querqy.model.StringRawQuery;
import querqy.parser.QuerqyParser;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;

import javax.swing.text.html.Option;

/**
 * @author René Kriegler, @renekrie
 */
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

    public static Object parse(final String line, final Input previousInput,
                               final QuerqyParserFactory querqyParserFactory) {


        if (line.endsWith("=>")) {
            if (line.length() == 2) {
                return new ValidationError("Empty input");
            }
            return parseInput(line.substring(0, line.length() - 2));
        }

        if (previousInput == null) {
            return new ValidationError("Missing input for instruction");
        }

        final String lcLine = line.toLowerCase(Locale.ROOT).trim();

        if (lcLine.startsWith(INSTR_DELETE)) {

            if (lcLine.length() == 6) {
                return new DeleteInstruction(previousInput.getInputTerms());
            }

            String instructionTerms = line.substring(6).trim();
            if (instructionTerms.charAt(0) != ':') {
                return new ValidationError("Cannot parse line: " + line);
            }

            if (instructionTerms.length() == 1) {
                return new DeleteInstruction(previousInput.getInputTerms());
            }

            instructionTerms = instructionTerms.substring(1).trim();
            final Object expr = parseTermExpression(instructionTerms);
            if (expr instanceof ValidationError) {
                return new ValidationError("Cannot parse line: " + line + " : " + ((ValidationError) expr).getMessage());
            }
            @SuppressWarnings("unchecked")
            final List<Term> deleteTerms = (List<Term>) expr;
            final List<Term> inputTerms = previousInput.getInputTerms();
            for (final Term term : deleteTerms) {
                if (term.findFirstMatch(inputTerms) == null) {
                    return new ValidationError("Condition doesn't contain the term to delete: " + term);
                }
            }

            return new DeleteInstruction(deleteTerms);

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
                String rawQuery = filterString.substring(1).trim();
                return new FilterInstruction(new StringRawQuery(null, rawQuery, Occur.MUST, false));
            } else if (querqyParserFactory == null) {
                return new ValidationError("No querqy parser factory to parse filter query. Prefix '*' if you want to pass this line as a raw query String to your search engine. Line: " + line);
            } else {
                QuerqyParser parser = querqyParserFactory.createParser();
                return new FilterInstruction(parser.parse(filterString));
            }
        }

        if (lcLine.startsWith(INSTR_BOOST_DOWN)) {
            return parseBoostInstruction(line, lcLine, 4, BoostDirection.DOWN, querqyParserFactory);
        }

        if (lcLine.startsWith(INSTR_BOOST_UP)) {
            return parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, querqyParserFactory);
        }

        if (lcLine.startsWith(INSTR_SYNONYM)) {

            if (lcLine.length() == 7) {
                return new ValidationError("Cannot parse line: " + line);
            }

            String synonymString = line.substring(7).trim();
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
                return new SynonymInstruction(synonymTerms);
            }

        }

        if (lcLine.startsWith(INSTR_DECORATE)) {
            return parseDecorateInstruction(line);
        }

        return line;

    }

    private static final String DECORATE_ERROR_MESSAGE_TEMPLATE = "Invalid decorate rule %s. Decorate rules must either " +
            "be defined only with a value, e. g. DECORATE: value, or with a key surrounded by brackets and a value, " +
            "e. g. DECORATE(key): value.";

    private static String createDecorateErrorMessage(final String line) {
        return String.format(DECORATE_ERROR_MESSAGE_TEMPLATE, line);
    }

    private static Pattern DECORATE_KEY_PATTERN = Pattern.compile("^\\(([\\w\\d_]+)\\):");

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

    public static Object parseBoostInstruction(String line, String lcLine, int lengthPredicate, BoostDirection direction, QuerqyParserFactory querqyParserFactory) {

        if (lcLine.length() == lengthPredicate) {
            return new ValidationError("Cannot parse line: " + line);
        }

        String boostLine = line.substring(lengthPredicate).trim();
        char ch = boostLine.charAt(0);
        switch (ch) {

            case '(':
                if (line.length() < 5) {
                    return new ValidationError("Cannot parse line, expecting boost factor and ':' after '(' in " + line);
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

            String rawQuery = boostLine.substring(1).trim();
            return new BoostInstruction(
                    new StringRawQuery(null, rawQuery, Occur.SHOULD, false),
                    direction, boost);

        } else if (querqyParserFactory == null) {

            return new ValidationError("No querqy parser factory to parse filter query. Prefix '" + RAWQUERY + "' you want to pass this line as a raw query String to your search engine. Line: " + line);

        } else {
            QuerqyParser parser = querqyParserFactory.createParser();
            return new BoostInstruction(parser.parse(boostLine), direction, boost);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object parseInput(String s) {

        boolean requiresLeftBoundary = false;
        boolean requiresRightBoundary = false;

        s = s.trim();
        String rawInput = s;
        if (s.length() > 0 && s.charAt(0) == BOUNDARY) {
            requiresLeftBoundary = true;
            s = s.substring(1).trim();
        }

        if (s.length() > 0 && s.charAt(s.length() - 1) == BOUNDARY) {
            requiresRightBoundary = true;
            s = s.substring(0, s.length() - 1).trim();
        }

        int pos = s.indexOf('*');
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
            return new Input((List<Term>) expr, requiresLeftBoundary, requiresRightBoundary, rawInput);
        }

    }

    static Object parseTermExpression(String s) {

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

        return (remaining.charAt(remaining.length() - 1) == WILDCARD)
                ? new PrefixTerm(remaining.toCharArray(), 0, remaining.length() - 1, fieldNames)
                : new Term(remaining.toCharArray(), 0, remaining.length(), fieldNames);


    }

    public static List<String> parseFieldNames(String s) {

        int len = s.length();

        if (len == 1) {
            return Arrays.asList(s);
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