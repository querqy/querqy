package querqy.model;


import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;

import java.util.List;
import java.util.stream.Collectors;

public class FilterCriteria implements Criteria {

    private final String name;
    private final String value;

    public FilterCriteria(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean isValid(final Action action) {

        final List<Instructions> instructions = action.getInstructions();
        if (instructions.isEmpty()) {
            return false;
        }

        return instructions.get(0)
                .getProperty(name)
                .filter(value::equals)
                .isPresent();

    }

    @Override
    public List<Action> apply(List<Action> actions) {
        return actions.parallelStream().filter(this::isValid).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "FilterCriteria{" +
                "field='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
