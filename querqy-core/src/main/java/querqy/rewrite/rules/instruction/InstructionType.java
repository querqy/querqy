package querqy.rewrite.rules.instruction;

import querqy.rewrite.rules.RuleParseException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum InstructionType {

    SYNONYM("synonym"),
    DELETE("delete"),
    REPLACE("replace"),
    DECORATE("decorate"),
    UP("up"),
    DOWN("down"),
    FILTER("filter");

    private final String typeName;

    InstructionType(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public static InstructionType of(final String type) {
        try {
            return InstructionType.valueOf(type.toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException e) {
            throw new RuleParseException(String.format("Rule type %s is not supported by Querqy", type));
        }
    }

    public static Set<InstructionType> getAll() {
        return Arrays.stream(values()).collect(Collectors.toSet());
    }
}
