package querqy.rewrite.commonrules.model;


import java.util.Objects;

public class FilterCriterion implements Criterion {

    private final String name;
    private final Object value;

    public FilterCriterion(final String name, final Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean isValid(final Instructions instructions) {

        return instructions
                .getProperty(name)
                .filter(value::equals)
                .isPresent();
    }

    @Override
    public String toString() {
        return "FilterCriterion{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FilterCriterion)) return false;
        final FilterCriterion that = (FilterCriterion) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
