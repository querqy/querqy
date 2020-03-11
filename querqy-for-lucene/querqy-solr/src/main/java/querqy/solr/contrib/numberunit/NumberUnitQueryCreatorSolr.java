package querqy.solr.contrib.numberunit;

import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.RawQuery;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.LinearFunction;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NumberUnitQueryCreatorSolr extends NumberUnitQueryCreator {

    public NumberUnitQueryCreatorSolr(int scale) {
        super(scale);
    }

    private static final String FUNC = "{!func}";
    private static final String MAX = "max(%s)";
    private static final String IF = "if(%s,%s,%s)";
    private static final String QUERY = "query(%s)";
    private static final String LINEAR_FUNCTION = "rint(linear(%s,%s,%s))";

    private static final String RANGE_QUERY = "{!frange l=%s u=%s v='%s'}";
    private static final String RANGE_QUERY_EXCLUDE_UPPER = "{!frange l=%s u=%s incu='false' v='%s'}";
    private static final String RANGE_QUERY_EXCLUDE_LOWER = "{!frange l=%s u=%s incl='false' v='%s'}";
    private static final String EXACT_MATCH_QUERY_TEMPLATE= "{!term f=%s v=%s}";

    private static final String RANGE_QUERY_TEMPLATE = "%s:[%s TO %s]";
    private static final String BOOLEAN_STRING_CONCATENATION_OR = " OR ";

    protected RawQuery createRawBoostQuery(BigDecimal value, List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        List<String> queryParts = new ArrayList<>();

        perUnitNumberUnitDefinitions.forEach(perUnitDef -> {
            NumberUnitDefinition numberUnitDef = perUnitDef.numberUnitDefinition;

            BigDecimal multipliedValue = value.multiply(perUnitDef.multiplier);

            BigDecimal upperBound = addPercentage(multipliedValue, numberUnitDef.boostPercentageUpperBoundary);
            BigDecimal lowerBound = subtractPercentage(multipliedValue, numberUnitDef.boostPercentageLowerBoundary);

            BigDecimal upperBoundExactMatch = addPercentage(multipliedValue, numberUnitDef.boostPercentageUpperBoundaryExactMatch);
            BigDecimal lowerBoundExactMatch = subtractPercentage(multipliedValue, numberUnitDef.boostPercentageLowerBoundaryExactMatch);

            LinearFunction linearFunctionLower = super.createLinearFunctionParameters(
                    lowerBound, numberUnitDef.minScoreAtLowerBoundary,
                    multipliedValue, numberUnitDef.maxScoreForExactMatch);

            LinearFunction linearFunctionUpper = super.createLinearFunctionParameters(
                    upperBound, numberUnitDef.minScoreAtUpperBoundary,
                    multipliedValue, numberUnitDef.maxScoreForExactMatch);

            perUnitDef.numberUnitDefinition.fields.forEach(field -> queryParts.add(
                    String.format(
                            IF,
                            String.format(
                                    QUERY,
                                    String.format(
                                            RANGE_QUERY_EXCLUDE_UPPER,
                                            lowerBound.setScale(field.scale, super.getRoundingMode()),
                                            multipliedValue.setScale(field.scale, super.getRoundingMode()),
                                            field.fieldName)),
                            String.format(
                                    LINEAR_FUNCTION,
                                    field.fieldName,
                                    linearFunctionLower.m,
                                    linearFunctionLower.b),
                            String.format(
                                    IF,
                                    String.format(
                                            QUERY,
                                            String.format(
                                                    RANGE_QUERY,
                                                    lowerBoundExactMatch.setScale(field.scale, super.getRoundingMode()),
                                                    upperBoundExactMatch.setScale(field.scale, super.getRoundingMode()),
                                                    field.fieldName)),
                                    numberUnitDef.maxScoreForExactMatch.add(numberUnitDef.additionalScoreForExactMatch).intValue(),
                                    String.format(
                                            IF,
                                            String.format(
                                                    QUERY,
                                                    String.format(
                                                            RANGE_QUERY_EXCLUDE_LOWER,
                                                            multipliedValue.setScale(field.scale, super.getRoundingMode()),
                                                            upperBound.setScale(field.scale, super.getRoundingMode()),
                                                            field.fieldName)),
                                            String.format(
                                                    LINEAR_FUNCTION,
                                                    field.fieldName,
                                                    linearFunctionUpper.m,
                                                    linearFunctionUpper.b),
                                            "0"))))); });

        String queryString = queryParts.size() == 1 ? queryParts.get(0) : String.format(MAX, String.join(",", queryParts));
        return new RawQuery(null, FUNC + queryString, Clause.Occur.MUST, true);
    }

    public BoostQuery createBoostQuery(BigDecimal value, List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        return new BoostQuery(createRawBoostQuery(value, perUnitNumberUnitDefinitions), 1.0f);
    }


    public RawQuery createFilterQuery(BigDecimal value, List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        List<String> queryParts = new ArrayList<>();

        perUnitNumberUnitDefinitions.forEach(def -> {
            BigDecimal multipliedValue = value.multiply(def.multiplier);

            BigDecimal lowerBound = def.numberUnitDefinition.filterPercentageLowerBoundary.compareTo(BigDecimal.ZERO) >= 0
                    ? subtractPercentage(multipliedValue, def.numberUnitDefinition.filterPercentageLowerBoundary)
                    : def.numberUnitDefinition.filterPercentageLowerBoundary;

            BigDecimal upperBound = def.numberUnitDefinition.filterPercentageUpperBoundary.compareTo(BigDecimal.ZERO) >= 0
                    ? addPercentage(multipliedValue, def.numberUnitDefinition.filterPercentageUpperBoundary)
                    : def.numberUnitDefinition.filterPercentageUpperBoundary;

            def.numberUnitDefinition.fields.forEach(field ->
                    queryParts.add(String.format(
                            RANGE_QUERY_TEMPLATE,
                            field.fieldName,
                            lowerBound.compareTo(BigDecimal.ZERO) >= 0 ? lowerBound.setScale(field.scale, super.getRoundingMode()) : "*",
                            upperBound.compareTo(BigDecimal.ZERO) >= 0 ? upperBound.setScale(field.scale, super.getRoundingMode()) : "*"))); });

        return new RawQuery(null, String.join(BOOLEAN_STRING_CONCATENATION_OR, queryParts), Clause.Occur.MUST, true);
    }
}
