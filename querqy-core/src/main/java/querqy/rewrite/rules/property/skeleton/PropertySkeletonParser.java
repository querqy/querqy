package querqy.rewrite.rules.property.skeleton;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.SkeletonComponentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(staticName = "create")
public class PropertySkeletonParser implements SkeletonComponentParser<Map<String, Object>> {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
            .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);

    private final Map<String, Object> properties = new HashMap<>();

    private String content = null;
    private StringBuilder multiLineInput = null;
    private PropertySkeletonInput currentPropertySkeletonInput = null;


    public void setContent(final String content) {
        this.content = content;
    }

    public boolean isParsable() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before calling isParsable()");
        }

        return isInMultiLineParsingMode() || content.startsWith(PropertySkeletonInputParser.PROPERTY_INDICATOR);
    }

    private boolean isInMultiLineParsingMode() {
        return multiLineInput != null;
    }

    public void parse() {
        parsePropertyInput();

        if (isInMultiLineParsingMode() || currentPropertySkeletonInput.isMultiLineInitiation()) {
            parseAsMultiLine();

        } else if (currentPropertySkeletonInput.isPropertyInitiation()) {
            parseAsSingleLine();

        } else {
            throw new RuleParseException(String.format("Cannot parse property %s", currentPropertySkeletonInput.getRawInput()));
        }
    }

    private void parsePropertyInput() {
        if (content == null) {
            throw new IllegalStateException("Content must be set before parsing");
        }

        final PropertySkeletonInputParser propertySkeletonInputParser = PropertySkeletonInputParser.of(content);
        currentPropertySkeletonInput = propertySkeletonInputParser.parse();
    }

    private void parseAsMultiLine() {
        if (multiLineInput == null) {
            multiLineInput = new StringBuilder();
        }

        appendMultiLineInput();

        if (currentPropertySkeletonInput.isMultiLineFinishing()) {
            finishMultiLineParsing();
        }
    }

    private void appendMultiLineInput() {
        multiLineInput.append(currentPropertySkeletonInput.getStrippedInput());
        multiLineInput.append("\n");
    }

    private void finishMultiLineParsing() {
        final Map<String, Object> additionalProperties = deserializeMultiLineInput();
        putAllProperties(additionalProperties);

        multiLineInput = null;
    }

    private Map<String, Object> deserializeMultiLineInput() {
        final String jsonString = multiLineInput.toString();
        return deserializeJsonString(jsonString);
    }

    private Map<String, Object> deserializeJsonString(final String jsonString) {
        final TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};

        try {
            return mapper.readValue(jsonString, typeRef);

        } catch (IOException e) {
            throw new RuleParseException(
                    String.format("Could not parse multiline properties for input %s", jsonString), e);
        }
    }

    private void putAllProperties(final Map<String, Object> additionalProperties) {
        additionalProperties.forEach(this::putProperty);
    }

    private void putProperty(final String key, final Object value) {
        if (properties.containsKey(key)) {
            throw new RuleParseException(
                    String.format("Error adding property [%s, %s]: Property is already set", key, value));
        }

        properties.put(key, value);
    }

    private void parseAsSingleLine() {
        final String propertyAsJsonString = "{" + currentPropertySkeletonInput.getStrippedInput() + "}";
        final Map<String, Object> additionalProperties = deserializeJsonString(propertyAsJsonString);
        putAllProperties(additionalProperties);
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public Map<String, Object> finish() {
        if (isInMultiLineParsingMode()) {
            throw new RuleParseException(String.format("Multiline properties input in incomplete %s", multiLineInput));
        }

        final Map<String, Object> propertiesToReturn = new HashMap<>(properties);

        content = null;
        properties.clear();

        return propertiesToReturn;
    }

    public static String toTextDefinition(final String key, final String value) {
        return "@" + key + ": " + value;
    }

    public static String toTextDefinition(final Map<String, Object> properties) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(properties);
            return "@" + jsonString + "@";

        } catch (JsonProcessingException e) {
            throw new RuleParseException("Could not transform properties to JSON", e);
        }
    }
}
