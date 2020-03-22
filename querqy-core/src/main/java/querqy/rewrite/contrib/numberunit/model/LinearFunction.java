package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;

public class LinearFunction {

    public final BigDecimal m;
    public final BigDecimal b;

    public LinearFunction(final BigDecimal m, final BigDecimal b) {
        this.m = m;
        this.b = b;
    }

}
