package querqy.rewrite.commonrules.model;


public class FilterCriterion implements Criterion {

    private final String name;
    private final String value;

    public FilterCriterion(final String name, final String value) {
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
}
