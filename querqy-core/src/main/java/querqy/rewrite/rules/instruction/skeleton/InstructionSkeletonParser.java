package querqy.rewrite.rules.instruction.skeleton;

import lombok.NoArgsConstructor;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.SkeletonComponentParser;
import querqy.rewrite.rules.instruction.InstructionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(staticName="create")
public class InstructionSkeletonParser implements SkeletonComponentParser<InstructionSkeleton> {
    /**
     * The subsequent regex parses instructions, e.g.
     * UP(1.0): notebook
     *
     * It contains three capture groups:
     *  (1) The first capture group extracts the type: UP
     *
     *  (2) The second capture group extracts the parameter between the round brackets: 1.0
     *      Notice that it is embedded in an optional non-capture group: (?: ... )?
     *      Parameters may contain letters, digits, dots, dashes and underscores
     *
     *  (3) The third capture group extracts the value (notebook). This capture group is also optional.
     */
    private static final Pattern INSTRUCTION_PATTERN = Pattern.compile(
            String.join(
                    "\\s*",
                    Arrays.asList(
                            "^",
                            "([\\w]+)",
                            "(?:\\(", "([\\w.\\-_\\d]+)", "\\))?",
                            "(?::(.*))?",
                            "$"
                    )
            )
    );

    private String content;
    private Matcher matcher;
    private InstructionSkeleton instructionSkeleton;

    public void setContent(final String content) {
        this.content = content;
    }

    public boolean isParsable() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before calling isParsable()");
        }

        matcher = INSTRUCTION_PATTERN.matcher(content);
        return matcher.find();
    }

    public void parse() {
        if (matcher == null) {
            throw new IllegalStateException("isParsable() must be called before parsing");
        }

        parseInstruction();
    }

    public void parseInstruction() {
        final String typeInput = matcher.group(1);
        final String parameterInput = matcher.group(2);
        final String valueInput = matcher.group(3);

        final InstructionType parsedInstructionType = parseType(typeInput);
        final String parsedValue = parseValue(valueInput);

        instructionSkeleton = InstructionSkeleton.builder()
                .type(parsedInstructionType)
                .parameter(parameterInput)
                .value(parsedValue)
                .build();
    }

    private InstructionType parseType(final String type) {
        assertNotBlank(type);
        return InstructionType.of(type.trim());
    }

    private String parseValue(final String value) {
        if (value == null) {
            return null;
        }

        final String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private void assertNotBlank(final String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new RuleParseException(createExceptionMessage());
        }
    }

    private String createExceptionMessage() {
        return String.format("Could not parse instruction %s", content);
    }

    public InstructionSkeleton finish() {
        if (instructionSkeleton == null) {
            throw new IllegalStateException("Content must be parsed before finishing");
        }

        return instructionSkeleton;
    }

    public static String toTextDefinition(final InstructionSkeleton instructionSkeleton) {
        final List<String> parts = new ArrayList<>();

        parts.add(instructionSkeleton.getType().name());

        instructionSkeleton.getParameter().ifPresent(
                parameter -> parts.add("(" + parameter + ")"));

        instructionSkeleton.getValue().ifPresent(
                value -> parts.add(": " + value));

        return String.join("", parts);
    }
}
