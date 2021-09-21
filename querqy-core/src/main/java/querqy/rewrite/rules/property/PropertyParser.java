package querqy.rewrite.rules.property;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.NoArgsConstructor;
import querqy.rewrite.commonrules.model.InstructionsProperties;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(staticName = "create")
public class PropertyParser {

    public static final String ID = "_id";
    public static final String LOG_MESSAGE = "_log";

    public InstructionsProperties parse(final Map<String, Object> properties,
                                        final String defaultId) {

        final Map<String, Object> propertiesWithDefaults = new HashMap<>(properties);
        propertiesWithDefaults.putIfAbsent(ID, defaultId);
        propertiesWithDefaults.putIfAbsent(LOG_MESSAGE, propertiesWithDefaults.get(ID));

        return new InstructionsProperties(propertiesWithDefaults, createJsonPathConfiguration());
    }

    private Configuration createJsonPathConfiguration() {
        final Configuration configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .build();
        configuration.addOptions(Option.ALWAYS_RETURN_LIST);

        return configuration;
    }
}
