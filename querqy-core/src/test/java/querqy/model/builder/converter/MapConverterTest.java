package querqy.model.builder.converter;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MapConverterTest {

    @Test
    public void testParseBooleanToString() {
        Map<String, Object> map = new HashMap<>();
        final MapConverter mapConverter = new MapConverter().enableParseBooleanToString();
        mapConverter.convertAndPut(map, "field", false, MapConverter.DEFAULT_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", "false"));
    }

    @Test
    public void testInclusionOfNullValues() {
        Map<String, Object> map = new HashMap<>();
        final MapConverter mapConverter = new MapConverter().enableIncludeNullValues();
        mapConverter.convertAndPut(map, "field", null, MapConverter.DEFAULT_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", null));
    }


}
