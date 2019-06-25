package querqy.rewrite.commonrules.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author René Kriegler, @renekrie
 * @author Lucky Sharma, @MighTguY
 */
public class PropertySorting implements Sorting {

    private String name;
    private SortOrder order;

    public PropertySorting(final String name, final SortOrder order) {
        this.name = name;
        this.order = order;
    }

    @Override
    public List<Comparator<Instructions>> getComparators() {
        return Arrays.asList(new PropertyComparator(name, order),
                new Sorting.ConfigOrderComparator(order));
    }

    @Override
    public String toString() {
        return "PropertySorting{" +
                "field='" + name + '\'' +
                ", order='" + order + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PropertySorting sorting = (PropertySorting) o;
        return Objects.equals(name, sorting.name) && order == sorting.order;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, order);
    }


    static class PropertyComparator implements Comparator<Instructions> {

        private final String propertyName;
        private final int factor;

        PropertyComparator(final String propertyName, final SortOrder sortOrder) {
            this.propertyName = propertyName;
            this.factor = sortOrder.factor;
        }


        @Override
        public int compare(Instructions instructions1, Instructions instructions2) {
            final Optional<Object> property1 = instructions1.getProperty(propertyName);
            final Optional<Object> property2 = instructions2.getProperty(propertyName);

            // p1 exist, p2 doesn't -> sort p1 before p2,  TODO: always sort missing last?
            return property1.map(o1Value ->
                    property2.map(o2Value -> ((Comparable) o1Value).compareTo(o2Value) * factor)
                            .orElse(-1)
            ).orElseGet(() -> property2.isPresent() ? 1 : 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PropertyComparator)) return false;
            PropertyComparator that = (PropertyComparator) o;
            return factor == that.factor && Objects.equals(propertyName, that.propertyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(propertyName, factor);
        }
    }


}
