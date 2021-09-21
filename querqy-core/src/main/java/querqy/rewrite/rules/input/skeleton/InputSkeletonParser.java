package querqy.rewrite.rules.input.skeleton;

import lombok.NoArgsConstructor;
import querqy.rewrite.rules.SkeletonComponentParser;

@NoArgsConstructor(staticName = "create")
public class InputSkeletonParser implements SkeletonComponentParser<String> {

    public static final String INPUT_INDICATOR = "=>";

    private String rawInput = null;
    private String parsedInput = null;

    public void setContent(final String content) {
        rawInput = content;
    }

    public boolean isParsable() {
        return rawInput.endsWith(INPUT_INDICATOR);
    }

    public void parse() {
        parsedInput = rawInput.substring(0, rawInput.length() - 2).trim();
    }

    public String finish() {
        return parsedInput;
    }

    public static String toTextDefinition(final String inputDefinition) {
        return inputDefinition + " " + INPUT_INDICATOR;
    }
}