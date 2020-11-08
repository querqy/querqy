package querqy.rewrite.commonrules;

import static querqy.rewrite.commonrules.PropertiesBuilder.JsonObjState.IN_OBJECT;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.rewrite.commonrules.ValidationError;
import querqy.rewrite.commonrules.model.InstructionsProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PropertiesBuilder {

    enum JsonObjState {BEFORE, IN_OBJECT, AFTER_OBJECT}

    private JsonObjState jsonObjState = JsonObjState.BEFORE;

    private java.util.Map<String, Object> primitiveProperties;
    private StringBuilder jsonObjectString;
    private final ObjectMapper objectMapper;
    private final Configuration jsonPathConfiguration;

    public PropertiesBuilder() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);



        jsonPathConfiguration = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider())
                .mappingProvider(new JacksonMappingProvider()).build();
        jsonPathConfiguration.addOptions(Option.ALWAYS_RETURN_LIST);

        primitiveProperties = new HashMap<>();
        jsonObjectString =  new StringBuilder();

    }

    /**
     * Consumes the next line and returns an optional validation error.
     *
     * @param line The next line from the input
     * @return An optional validation error.
     * @throws RuleParseException if a JSON property cannot be parsed.
     */
    public Optional<ValidationError> nextLine(final String line) throws RuleParseException {

        String str = line.trim();
        if (str.length() > 0) {

            if (jsonObjState != IN_OBJECT) {


                // left trim only (as this might be the start of an object)
                str = ltrim(line);

                // TODO: The generic fallback error message should be handled outside of PropertiesBuilder
                if (!str.startsWith("@")) {
                    return Optional.of(new ValidationError("not able to read the following statement: " + line));
                }

                str = ltrim(str.substring(1));
                if (str.length() == 0) {
                    return Optional.of(new ValidationError("property expected after @"));
                }

                if (str.charAt(0) == '{') {
                    // start of object
                    if (jsonObjState == JsonObjState.AFTER_OBJECT) {
                        return Optional.of(new ValidationError("Only one property object can be defined"));
                    }


                    final String rStr = rtrim(str);
                    if (rStr.endsWith("}@") && !rStr.endsWith("\\}@")) {
                        // the entire JSON object is on a single line
                        jsonObjectString.append(rStr.substring(0, rStr.length() - 1));
                        mergeObjectWithPrimitiveProperties();

                    } else {
                        jsonObjState = IN_OBJECT;
                        jsonObjectString.append(str);
                    }

                } else {
                    // primitive property
                    try {
                        final Map<String, Object> map = stringToProperty(str);
                        for (final Map.Entry<String, Object> entry : map.entrySet()) {
                            addProperty(entry.getKey(), entry.getValue(), primitiveProperties);
                        }
                    } catch (final IOException | RuleParseException e) {
                        return Optional.of(new ValidationError("Line could not be read as a property: " +
                                e.getMessage() + ", " + line));
                    }

                }

            } else {
                str = rtrim(line);
                if (str.endsWith("\\}@") || !str.endsWith("}@")) {
                    jsonObjectString.append('\n').append(line);
                } else {
                    jsonObjectString.append('\n').append(str.substring(0, str.length() - 1));
                    mergeObjectWithPrimitiveProperties();

                }
            }
        }

        return Optional.empty();
    }

    public void reset() {
        jsonObjState = JsonObjState.BEFORE;
        primitiveProperties = new HashMap<>();
        jsonObjectString = new StringBuilder();
    }

    public Optional<Object> addPropertyIfAbsent(final String name, final Object value) {
        return Optional.ofNullable(primitiveProperties.putIfAbsent(name, value));
    }

    private void mergeObjectWithPrimitiveProperties() throws RuleParseException {
        final Map<String, Object> propertyMap;
        try {
            propertyMap = objectMapper.readValue(jsonObjectString.toString(), Map.class);
        } catch (IOException e) {
            throw new RuleParseException("Cannot parse Json: " + jsonObjectString.toString(), e);
        }
        for (final Map.Entry<String, Object> entry : primitiveProperties.entrySet()) {
            addProperty(entry.getKey(), entry.getValue(), propertyMap);
        }
        primitiveProperties = propertyMap;
        jsonObjState = JsonObjState.AFTER_OBJECT;

    }


    public InstructionsProperties build() throws RuleParseException {

        if (jsonObjState == IN_OBJECT) {
            throw new RuleParseException("Cannot parse Json: " + jsonObjectString.toString());
        }

        return new InstructionsProperties(primitiveProperties, jsonPathConfiguration);
    }

    private void addProperty(final String name, final Object value, final Map<String, Object> target)
            throws RuleParseException {
        final Object valueSoFar = target.get(name);
        if (valueSoFar == null) {
            target.put(name, value);
        } else {
            throw new RuleParseException("Duplicate property " + name);
        }
    }

    private Map<String, Object> stringToProperty(final String str) throws IOException {

        try {
            return objectMapper.readValue("{" + str + "}", Map.class);
        } catch (final IOException e) {
            // trying to repair JSON
            final int sep = str.indexOf(':');
            if (sep < 1 || sep == str.length() - 1) {
                throw e;
            }
            final String name = str.substring(0, sep).trim();
            final String value = str.substring(sep + 1).trim();
            if (value.length() == 0) {
                throw e;
            }

            for (int i = 0, len = value.length(); i < len; i++) {
                if ("'[{\"}]".indexOf(value.charAt(i)) > -1) {
                    throw e;
                }
            }

            return objectMapper.readValue("{" + name + ":" + '"' + value + '"' + "}", Map.class);

        }


    }

    public static String ltrim(final String string) {
        for (int ofs = 0, len = string.length(); ofs < len; ofs++) {
            if (!Character.isWhitespace(string.charAt(ofs))) {
                return ofs == 0 ? string : string.substring(ofs);
            }
        }
        return "";
    }


    public static String rtrim(final String string) {
        for (int ofs = string.length() - 1; ofs > 0; ofs--) {
            if (!Character.isWhitespace(string.charAt(ofs))) {
                return ofs == string.length() - 1 ? string : string.substring(0, ofs);
            }
        }
        return "";
    }


}
