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
        MapConverter.MAP_CONVERTER_BOOL_STRING.convertAndPut(map, "field", false, MapConverter.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", "false"));
    }

    @Test
    public void testInclusionOfNullValues() {
        Map<String, Object> map = new HashMap<>();
        final MapConverter mapConverter = MapConverter.MAP_CONVERTER_NULL_VALUES;
        mapConverter.convertAndPut(map, "field", null, MapConverter.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field", null));
    }

    @Test
    public void testInclusionOfNullValuesAndBooleanString() {
        Map<String, Object> map = new HashMap<>();
        final MapConverter mapConverter = MapConverter.MAP_CONVERTER_BOOL_STRING_NULL_VALUES;
        mapConverter.convertAndPut(map, "field1", null, MapConverter.DEFAULT_MV_CONVERTER);
        mapConverter.convertAndPut(map, "field2", true, MapConverter.DEFAULT_MV_CONVERTER);
        assertThat(map).containsExactly(new AbstractMap.SimpleEntry<>("field1", null),
                new AbstractMap.SimpleEntry<>("field2", "true"));
    }


}
