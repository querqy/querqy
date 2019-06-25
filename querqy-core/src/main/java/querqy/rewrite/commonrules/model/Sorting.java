package querqy.rewrite.commonrules.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author René Kriegler, @renekrie
 */
public interface Sorting {

    enum SortOrder {

        ASC(1), DESC(-1);

        public final int factor;

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


    Comparator<Instructions> DEFAULT_COMPARATOR = new Sorting.ConfigOrderComparator(SortOrder.ASC);

    Sorting DEFAULT_SORTING = new Sorting() {

        private final List<Comparator<Instructions>> comparators = Collections.singletonList(DEFAULT_COMPARATOR);

        @Override
        public List<Comparator<Instructions>> getComparators() {
            return comparators;
        }

    };


    List<Comparator<Instructions>> getComparators();


    class ConfigOrderComparator implements Comparator<Instructions> {

        private final int factor;

        ConfigOrderComparator(final SortOrder sortOrder) {
            this.factor = sortOrder.factor;
        }


        @Override
        public int compare(final Instructions instructions1, final Instructions instructions2) {
            return (instructions1.getOrd() - instructions2.getOrd()) * factor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigOrderComparator)) return false;
            ConfigOrderComparator that = (ConfigOrderComparator) o;
            return factor == that.factor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(factor);
        }

    }

}
