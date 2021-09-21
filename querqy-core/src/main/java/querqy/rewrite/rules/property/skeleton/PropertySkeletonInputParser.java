package querqy.rewrite.rules.property.skeleton;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "of")
public class PropertySkeletonInputParser {

    public static final String PROPERTY_INDICATOR = "@";
    public static final String MULTILINE_INITIATION_INDICATOR = "{";
    public static final String MULTILINE_FINISHING_INDICATOR = "}@";
    public static final String ESCAPED_MULTILINE_FINISHING_INDICATOR = "\\}@";

    private final String propertyInput;

    private final PropertySkeletonInput.Builder builder = PropertySkeletonInput.builder();
    private String parsedPropertyInput;

    public PropertySkeletonInput parse() {
        builder.rawInput(propertyInput);
        parsedPropertyInput = propertyInput;

        parsePrefix();
        parseSuffix();

        return builder.strippedInput(parsedPropertyInput).build();
    }

    private void parsePrefix() {
        if (parsedPropertyInput.startsWith(PROPERTY_INDICATOR)) {
            builder.isPropertyInitiation(true);
            removeFirstCharFromInput();
            checkForMultiLineInitiation();
        }
    }

    private void removeFirstCharFromInput() {
        parsedPropertyInput = parsedPropertyInput.substring(1);
    }

    private void checkForMultiLineInitiation() {
        if (parsedPropertyInput.startsWith(MULTILINE_INITIATION_INDICATOR)) {
            builder.isMultiLineInitiation(true);
        }
    }

    private void parseSuffix() {
        if (parsedPropertyInput.endsWith(MULTILINE_FINISHING_INDICATOR) &&
                !parsedPropertyInput.endsWith(ESCAPED_MULTILINE_FINISHING_INDICATOR)) {
            removeLastCharFromInput();
            builder.isMultiLineFinishing(true);
        }
    }

    private void removeLastCharFromInput() {
        parsedPropertyInput = parsedPropertyInput.substring(0, parsedPropertyInput.length() - 1);
    }
}
