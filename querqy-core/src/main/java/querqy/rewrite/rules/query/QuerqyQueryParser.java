package querqy.rewrite.rules.query;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import querqy.model.Clause;
import querqy.model.ParametrizedRawQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.parser.QuerqyParser;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.rules.RuleParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class QuerqyQueryParser {

    public static final String RAW_QUERY_INDICATOR = "*";
    public static final String RAW_QUERY_PARAMETER_INDICATOR = "%%";

    private final QuerqyParserFactory querqyParserFactory;

    private final String value;
    private final Clause.Occur occur;

    public static QuerqyQueryParser createPrototypeOf(final QuerqyParserFactory querqyParserFactory) {
        return QuerqyQueryParser.of(querqyParserFactory, null, null);
    }

    public QuerqyQueryParser with(final String value, final Clause.Occur occur) {
        return QuerqyQueryParser.of(querqyParserFactory, value, occur);
    }

    public QuerqyQuery<?> parse() {
        assertThatThisIsNotPrototype();

        if (isRawQuery()) {
            return parseAsRawQuery();

        } else {
            return parseAsQuery();
        }
    }

    private void assertThatThisIsNotPrototype() {
        if (value == null || occur == null) {
            throw new UnsupportedOperationException("Methods cannot be used on prototype");
        }
    }

    private boolean isRawQuery() {
        return value.startsWith(RAW_QUERY_INDICATOR);
    }

    private RawQuery parseAsRawQuery() {
        if (isParameterizedRawQuery()) {
            return parseAsParameterizedRawQuery();

        } else {
            return new StringRawQuery(null, value.substring(1).trim(), occur, false);
        }
    }

    private boolean isParameterizedRawQuery() {
        return value.contains(RAW_QUERY_PARAMETER_INDICATOR);
    }

    private RawQuery parseAsParameterizedRawQuery() {
        final List<String> rawQueryParts = Arrays.stream(value
                        .substring(1)
                        .split(RAW_QUERY_PARAMETER_INDICATOR, -1))
                .map(String::trim)
                .collect(Collectors.toList());

        if (rawQueryParts.size() % 2 == 0) {
            throw new RuleParseException("Invalid use of parametrization in the definition of a RawQuery. " +
                    "Parameters must begin and end with %%");
        }

        final List<ParametrizedRawQuery.Part> parametrizedParts = parseParametrizedRawQueryParts(rawQueryParts);
        return new ParametrizedRawQuery(null, parametrizedParts, occur, false);
    }

    private List<ParametrizedRawQuery.Part> parseParametrizedRawQueryParts(List<String> rawQueryParts) {
        final List<ParametrizedRawQuery.Part> parametrizedParts = new ArrayList<>();

        ParametrizedRawQuery.Part.Type type = ParametrizedRawQuery.Part.Type.QUERY_PART;
        for (final String part : rawQueryParts) {
            parametrizedParts.add(new ParametrizedRawQuery.Part(part, type));
            type = getOtherType(type);
        }

        return parametrizedParts;
    }

    private ParametrizedRawQuery.Part.Type getOtherType(ParametrizedRawQuery.Part.Type type) {
        return type == ParametrizedRawQuery.Part.Type.PARAMETER
                ? ParametrizedRawQuery.Part.Type.QUERY_PART
                : ParametrizedRawQuery.Part.Type.PARAMETER;
    }

    protected Query parseAsQuery() {
        final QuerqyParser querqyQueryParser = querqyParserFactory.createParser();
        return querqyQueryParser.parse(value);
    }

}
