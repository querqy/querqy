package querqy.rewrite.rules.instruction.skeleton;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.Optional;

@Builder
@EqualsAndHashCode
@ToString
public class InstructionSkeleton {

    @Getter private final InstructionType type;
    private final String parameter;
    private final String value;

    public Optional<String> getParameter() {
        return Optional.ofNullable(parameter);
    }
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

}
