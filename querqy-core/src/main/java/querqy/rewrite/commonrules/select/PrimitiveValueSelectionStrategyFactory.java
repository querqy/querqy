package querqy.rewrite.commonrules.select;

/**
 * This class allows to select rules by using filter expressions for primitive values. Syntax: &quot;name:value&quot;
 */
public class PrimitiveValueSelectionStrategyFactory extends ExpressionCriteriaSelectionStrategyFactory {

    @Override
    public FilterCriterion stringToFilterCriterion(final String s) {

        return criteriaToJsonPathExpressionCriterion(s);

    }

    public static FilterCriterion criteriaToJsonPathExpressionCriterion(final String s) {

        final String str = s.trim();
        if (str.length() < 3) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        final int pos = str.indexOf(':');
        if (pos < 1) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        final String name = str.substring(0, pos).trim();
        final String value = str.substring(pos + 1).trim();

        if (name.isEmpty() || value.isEmpty()) {
            throw new IllegalArgumentException("Invalid criterion string " + s);
        }

        return new ExpressionFilterCriterion("$.[?(@." + name + " == '" + value.replace("'", "\\'") + "')]");
    }

}
