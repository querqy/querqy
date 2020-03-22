package querqy.rewrite.contrib.numberunit.model;

import querqy.ComparableCharSequence;
import querqy.model.DisjunctionMaxQuery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NumberUnitQueryInput {

    private final BigDecimal number;
    private ComparableCharSequence unit;

    private final Set<DisjunctionMaxQuery> originDisjunctionMaxQueries = new HashSet<>();

    public NumberUnitQueryInput(final BigDecimal number) {
        this(number, null);
    }

    public NumberUnitQueryInput(final BigDecimal number, ComparableCharSequence unit) {
        this.number = number;
        this.unit = unit;
    }

    public boolean hasUnit() {
        return this.unit != null;
    }

    public void addOriginDisjunctionMaxQuery(final DisjunctionMaxQuery dmq) {
        this.originDisjunctionMaxQueries.add(dmq);
    }

    public Set<DisjunctionMaxQuery> getOriginDisjunctionMaxQuery() {
        return Collections.unmodifiableSet(this.originDisjunctionMaxQueries);
    }

    public BigDecimal getNumber() {
        return this.number;
    }

    public ComparableCharSequence getUnit() {
        return this.unit;
    }

    public void setUnit(ComparableCharSequence unit) {
        this.unit = unit;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NumberUnitQueryInput numberUnitQueryInput = (NumberUnitQueryInput) o;
        if (this.getNumber().compareTo(numberUnitQueryInput.getNumber()) == 0) {

            if (!this.hasUnit()) {
                return !numberUnitQueryInput.hasUnit();

            } else {
                return this.getUnit().equals(numberUnitQueryInput.getUnit());
            }

        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return this.getNumber() + " " + this.getUnit();
    }

}
