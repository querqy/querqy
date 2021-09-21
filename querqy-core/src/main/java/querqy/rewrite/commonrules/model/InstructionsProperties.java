package querqy.rewrite.commonrules.model;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InstructionsProperties {

    @EqualsAndHashCode.Include private final Map<String, Object> propertyMap;
    private final DocumentContext documentContext;

    public InstructionsProperties(final Map<String, Object> propertyMap, final Configuration jsonPathConfig) {
        this.propertyMap = propertyMap;
        documentContext = JsonPath.using(jsonPathConfig).parse(propertyMap);
    }

    public InstructionsProperties(final Map<String, Object> propertyMap) {
        this(propertyMap, Configuration.defaultConfiguration());
    }


    public Optional<Object> getProperty(final String name) {
        return Optional.ofNullable(propertyMap.get(name));
    }

    public boolean matches(final String jsonPath) {
        final List read = documentContext.read(jsonPath);
        return read.size() > 0;
    }
}
