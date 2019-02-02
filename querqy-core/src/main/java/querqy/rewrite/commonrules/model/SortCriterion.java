package querqy.rewrite.commonrules.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortCriterion implements Criterion {

    private String name;
    private String type; // TODO: change this into an enum ASC(factor=1), DESC(factor=-1) and use factor * .compare()

    public SortCriterion(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public List<Action> apply(final List<Action> actions) {
        // FIXME: avoid changing the input list
        Collections.sort(actions, new Comparator<Action>() {
            @Override
            public int compare(Action o1, Action o2) {

                final List<Instructions> instructions1 = o1.getInstructions();
                if (instructions1.isEmpty()) {
                    return 0;
                }
                final List<Instructions> instructions2 = o2.getInstructions();
                if (instructions2.isEmpty()) {
                    return 0;
                }

                return instructions1.get(0)
                        .getProperty(name)
                        .map( o1Value ->
                            instructions2.get(0)
                                    .getProperty(name)
                                    .map(o2Value -> {

                                        if (type.equals("asc")) {
                                            return o1Value.compareTo(o2Value);
                                        } else if (type.equals("desc")) {
                                            return o2Value.compareTo(o1Value);
                                        }

                                        return 0;
                                    }).orElse(0)

                            ).orElse(0);



            }
        });
        return actions;
    }

    @Override
    public boolean isValid(final Action action) {

        final List<Instructions> instructions = action.getInstructions();
        if (instructions.isEmpty()) {
            return false;
        }

        // FIXME: I don't think this is the correct semantics for sorting (this seems to filter)
        return instructions.get(0).getProperty(name).isPresent();

    }

    @Override
    public String toString() {
        return "SortCriterion{" +
                "field='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
