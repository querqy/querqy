package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;

public class PerUnitNumberUnitDefinition {

    public final NumberUnitDefinition numberUnitDefinition;
    public final BigDecimal multiplier;

    public PerUnitNumberUnitDefinition(final NumberUnitDefinition numberUnitDefinition, final BigDecimal multiplier) {
        this.numberUnitDefinition = numberUnitDefinition;
        this.multiplier = multiplier;
    }

}
