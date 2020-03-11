package querqy.rewrite.contrib.numberunit;

import querqy.model.BooleanParent;
import querqy.model.BoostQuery;
import querqy.model.QuerqyQuery;
import querqy.rewrite.contrib.numberunit.model.LinearFunction;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public abstract class NumberUnitQueryCreator {

    private final int scale;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal n100 = new BigDecimal(100);
    private final BigDecimal n0 = new BigDecimal(0);

    protected NumberUnitQueryCreator(int scale) {
        this.scale = scale;
    }

    public int getScale() {
        return this.scale;
    }

    public RoundingMode getRoundingMode() {
        return ROUNDING_MODE;
    }

    public LinearFunction createLinearFunctionParameters(BigDecimal x1, BigDecimal y1,
                                                            BigDecimal x2, BigDecimal y2) {
        final BigDecimal x = x1.subtract(x2);
        final BigDecimal y = y1.subtract(y2);

        final BigDecimal m = x.compareTo(n0) != 0 ? y.divide(x, this.scale, ROUNDING_MODE) : n0;
        final BigDecimal b = y1.subtract(x1.multiply(m)).setScale(this.scale, ROUNDING_MODE);

        return new LinearFunction(m, b);
    }

    public BigDecimal calculatePercentageChange(BigDecimal number, BigDecimal percentage) {
        return number.multiply(percentage).divide(n100, this.scale, ROUNDING_MODE);
    }

    public BigDecimal subtractPercentage(BigDecimal number, BigDecimal percentage) {
        return number.subtract(calculatePercentageChange(number, percentage)).setScale(this.scale, ROUNDING_MODE);
    }

    public BigDecimal addPercentage(BigDecimal number, BigDecimal percentage) {
        return number.add(calculatePercentageChange(number, percentage)).setScale(this.scale, ROUNDING_MODE);
    }

    public abstract QuerqyQuery<BooleanParent> createFilterQuery(BigDecimal value, List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions);
    public abstract BoostQuery createBoostQuery(BigDecimal value, List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions);
}
