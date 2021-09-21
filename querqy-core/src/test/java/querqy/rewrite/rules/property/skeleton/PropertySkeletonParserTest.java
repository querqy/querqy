package querqy.rewrite.rules.property.skeleton;

import org.junit.Test;
import querqy.rewrite.rules.RuleParseException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class PropertySkeletonParserTest {

    @Test
    public void testThat_exceptionIsThrown_forDuplicatePropertyKey() {
        assertThrows(RuleParseException.class, () ->
                        parse(
                                "@d: true",
                                "@d: false"
                        )
                );
    }

    @Test
    public void testThat_exceptionIsThrown_forUnfinishedMultiLineProperties() {
        assertThrows(RuleParseException.class, () ->
                        parse(
                                "@{",
                                "d: true"
                        )
                );
    }

    @Test
    public void testThat_inputIsParsedProperly_forCombinedPropertyInput() {
        assertThat(
                parse(
                        "@ a : \"b\" ",
                        "@{",
                        "d: true",
                        "}@",
                        "@c:true"
                )
        ).isEqualTo(
                properties(
                        "a", "b",
                        "c", true,
                        "d", true
                )
        );
    }

    @Test
    public void testThat_inputIsParsedProperly_forMultipleLinesOfSingleProperties() {
        assertThat(
                parse(
                        "@ a : \"b\" ",
                        "@c:true"
                )
        ).isEqualTo(
                properties(
                        "a", "b",
                        "c", true
                )
        );
    }

    @Test
    public void testThat_inputIsParsedProperly_forMultipleLinesOfMultiLineProperties() {
        assertThat(
                parse(
                        "@{",
                        "a: \"b\",",
                        "\"c\": true",
                        "}@"
                )
        ).isEqualTo(
                properties(
                        "a", "b",
                        "c", true
                )
        );
    }

    @Test
    public void testThat_inputIsParsedProperly_forSingleLineOfMultiLineProperties() {
        assertThat(
                parse(
                        "@{ a: \"b\" }@"
                )
        ).isEqualTo(
                properties(
                        "a", "b"
                )
        );
    }

    @Test
    public void testThat_inputIsParsedProperly_forNestedJson() {
        assertThat(
                parse(
                        "@{",
                        "outer: {",
                        "  \"inner\": \"value\"",
                        "} ",
                        "}@"
                )
        ).isEqualTo(
                properties(
                        "outer", Collections.singletonMap("inner", "value")
                )
        );
    }

    private Map<String, Object> parse(final String... lines) {
        final PropertySkeletonParser parser = PropertySkeletonParser.create();
        Arrays.stream(lines).forEach(line -> {
                parser.setContent(line);
                parser.parse();
        });


        return parser.finish();
    }

    private Map<String, Object> properties(final Object... rawProperties) {
        assert rawProperties.length % 2 == 0;

        final Map<String, Object> properties = new HashMap<>();
        IntStream.range(0, rawProperties.length/2)
                .map(index -> index * 2)
                .forEach(
                        index -> properties.put((String) rawProperties[index], rawProperties[index + 1]));

        return properties;
    }


}
