package querqy.rewrite.rules.input.skeleton;

import lombok.NoArgsConstructor;
import querqy.rewrite.rules.SkeletonComponentParser;

@NoArgsConstructor(staticName = "create")
public class InputSkeletonParser implements SkeletonComponentParser<String> {

    public static final String INPUT_INDICATOR = "=>";

    private String content = null;
    private String parsedInput = null;

    public void setContent(final String content) {
        this.content = content;
    }

    public boolean isParsable() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before calling isParsable()");
        }

        return content.endsWith(INPUT_INDICATOR);
    }

    public void parse() {
        parsedInput = content.substring(0, content.length() - 2).trim();
    }

    public String finish() {
        if (content == null) {
            throw new IllegalStateException("Content must be parsed before finishing");
        }

        return parsedInput;
    }

    public static String toTextDefinition(final String inputDefinition) {
        return inputDefinition + " " + INPUT_INDICATOR;
    }
}