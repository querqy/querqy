package querqy.rewrite.commonrules.select.booleaninput.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BooleanInputElement {

    public final String term;
    public final Type type;

    public BooleanInputElement(String term, Type type) {
        this.term = term;
        this.type = type;
    }

    public enum Type {
        OR("OR", 4),
        AND("AND", 3),
        NOT("NOT", 2),
        TERM("TERM", 1),
        LEFT_PARENTHESIS("(", -1),
        RIGHT_PARENTHESIS(")", -1);

        private final String name;
        private final int priority;

        Type(final String name, final int priority) {
            this.name = name;
            this.priority = priority;
        }

        public static Type getType(final String name) {
            return TYPE_MAP.getOrDefault(name, TERM);
        }

        public String getName() {
            return name;
        }

        public int getPriority() {
            return priority;
        }
    }

    private static final Map<String, Type> TYPE_MAP = Arrays.stream(Type.values())
            .collect(Collectors.toMap(Type::getName, v -> v));

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanInputElement that = (BooleanInputElement) o;
        return Objects.equals(term, that.term) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, type);
    }

    @Override
    public String toString() {
        return "Element{" +
                "term='" + term + '\'' +
                ", type=" + type +
                '}';
    }



}
