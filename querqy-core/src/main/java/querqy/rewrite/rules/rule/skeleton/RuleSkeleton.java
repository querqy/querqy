package querqy.rewrite.rules.rule.skeleton;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
public class RuleSkeleton {

    private final String inputSkeleton;
    private final List<InstructionSkeleton> instructionSkeletons;
    private final Map<String, Object> properties;

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Builder
    public static RuleSkeleton create(final String inputSkeleton,
                                      @Singular final List<InstructionSkeleton> instructionSkeletons,
                                      @Singular final Map<String, Object> properties) {

        if (inputSkeleton == null) {
            throw new RuleParseException("Rule has no input");
        }

        if (instructionSkeletons.isEmpty()) {
            throw new RuleParseException(String.format("Rule with input %s has no instructions", inputSkeleton));
        }

        return RuleSkeleton.of(inputSkeleton, instructionSkeletons, properties);
    }

    // make javadoc happy when working with Lombok
    public static class RuleSkeletonBuilder {}
}
