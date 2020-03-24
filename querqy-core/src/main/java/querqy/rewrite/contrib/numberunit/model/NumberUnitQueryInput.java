package querqy.rewrite.contrib.numberunit.model;

import querqy.model.DisjunctionMaxQuery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NumberUnitQueryInput {

    private final BigDecimal number;
    private List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions;

    private final Set<DisjunctionMaxQuery> originDisjunctionMaxQueries = new HashSet<>();

    public NumberUnitQueryInput(final BigDecimal number) {
        this(number, null);
    }

    public NumberUnitQueryInput(final BigDecimal number,
                                final List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        this.number = number;
        this.perUnitNumberUnitDefinitions = perUnitNumberUnitDefinitions;
    }

    public boolean hasUnit() {
        return this.perUnitNumberUnitDefinitions != null;
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

    public List<PerUnitNumberUnitDefinition> getPerUnitNumberUnitDefinitions() {
        return perUnitNumberUnitDefinitions;
    }

    public void setPerUnitNumberUnitDefinitions(List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        this.perUnitNumberUnitDefinitions = perUnitNumberUnitDefinitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumberUnitQueryInput that = (NumberUnitQueryInput) o;
        return Objects.equals(number.doubleValue(), that.number.doubleValue()) &&
                Objects.equals(perUnitNumberUnitDefinitions, that.perUnitNumberUnitDefinitions) &&
                Objects.equals(originDisjunctionMaxQueries, that.originDisjunctionMaxQueries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, perUnitNumberUnitDefinitions, originDisjunctionMaxQueries);
    }

    @Override
    public String toString() {
        return "NumberUnitQueryInput{" +
                "number=" + number +
                ", perUnitNumberUnitDefinitions=" + perUnitNumberUnitDefinitions +
                '}';
    }
}
