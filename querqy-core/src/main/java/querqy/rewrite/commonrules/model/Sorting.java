package querqy.rewrite.commonrules.model;

import java.util.Comparator;
import java.util.Optional;

public class Sorting implements Comparator<Instructions> {

    public enum SortOrder {

        ASC(1), DESC(-1);

        private final int factor;

        SortOrder(final int factor) {
            this.factor = factor;
        }

        public static SortOrder fromString(final String s) {
            switch (s) {
                case "asc": return ASC;
                case "desc": return DESC;
                default:
                    throw new IllegalArgumentException("Invalid sort order: " + s);
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private String name;
    private SortOrder order;

    public Sorting(final String name, final SortOrder order) {
        this.name = name;
        this.order = order;
    }

    @Override
    public int compare(final Instructions instructions1, final Instructions instructions2) {

        final Optional<String> property1 = instructions1.getProperty(name);
        final Optional<String> property2 = instructions2.getProperty(name);

        final int c =  property1.map(o1Value ->
            property2.map(o2Value -> o1Value.compareTo(o2Value) * order.factor)
                    .orElse(-1) // p1 exist, p2 doesn't -> sort p1 before p2,  TODO: always sort missing last?
        ).orElseGet(() -> property2.isPresent() ? 1 : 0);

        // The contract is that we can only return 0 if both params are equal. Use Instructions.ord to ensure this:
        return (c == 0) ? (instructions1.ord - instructions2.ord) * order.factor : c;

    }

    @Override
    public String toString() {
        return "Sorting{" +
                "field='" + name + '\'' +
                ", order='" + order + '\'' +
                '}';
    }


}
