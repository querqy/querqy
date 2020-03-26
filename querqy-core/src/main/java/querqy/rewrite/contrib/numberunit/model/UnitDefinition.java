package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;

public class UnitDefinition {
    public final String term;
    public final BigDecimal multiplier;

    public UnitDefinition(final String term, final BigDecimal multiplier) {
        this.term = term;
        this.multiplier = multiplier;
    }

    @Override
    public String toString() {
        return String.format("UnitDefinition(%s, %f)", term, multiplier);
    }
}
