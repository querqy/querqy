package querqy.rewrite.rules.factory.config;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

@Builder(builderClassName = "Builder")
@Getter
public class TextParserConfig {

    @Default private final Reader rulesContentReader = emtpyReader();
    @Default private final Map<Integer, Integer> lineNumberMappings = Collections.emptyMap();
    @Default private final boolean isMultiLineRulesConfig = true;

    public static TextParserConfig defaultConfig() {
        return TextParserConfig.builder().build();
    }

    private static Reader emtpyReader() {
        try (final StringReader stringReader = new StringReader("")) {
            return stringReader;
        }
    }

}
