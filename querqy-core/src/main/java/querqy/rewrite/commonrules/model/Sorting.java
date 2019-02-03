package querqy.rewrite.commonrules.model;

import java.util.Comparator;
import java.util.Optional;

public class Sorting implements Comparator<Instructions> {

    private String name;
    private String order;
    private final int factor;

    public Sorting(final String name, final String order) {
        this.name = name;
        this.order = order;
        switch (order) {
            case "asc": factor = 1; break;
            case "desc": factor = -1; break;
            default:
                throw new IllegalArgumentException("Invalid sort order: " + order);
        }
    }

    @Override
    public int compare(final Instructions instructions1, final Instructions instructions2) {

        final Optional<String> property1 = instructions1.getProperty(name);
        final Optional<String> property2 = instructions2.getProperty(name);

        final int c =  property1.map(o1Value ->
            property2.map(o2Value -> o1Value.compareTo(o2Value) * factor)
                    .orElse(-1) // p1 exist, p2 doesn't -> sort p1 before p2,  TODO: always sort missing last?
        ).orElseGet(() -> property2.isPresent() ? 1 : 0);

        // The contract is that we can only return 0 if both params are equal. Use Instructions.ord to ensure this:
        return (c == 0) ? instructions1.ord - instructions2.ord : c;

    }

    @Override
    public String toString() {
        return "Sorting{" +
                "field='" + name + '\'' +
                ", order='" + order + '\'' +
                '}';
    }


}
