package querqy.model.convert.converter;

import lombok.Builder;
import querqy.model.convert.QueryBuilderException;
import querqy.model.convert.TypeCastingUtils;
import querqy.model.convert.model.Occur;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Builder
public class MapConverterConfig {

    public static final MapConverterConfig DEFAULT_MAP_CONVERTER_CONFIG = MapConverterConfig.builder().build();

    @Builder.Default
    private boolean parseBooleanToString = false;

    @Builder.Default
    private boolean includeNullValues = false;

    public void convertAndPut(final Map<String, Object> map, final String fieldName, final Object value,
                              final MapValueConverter mapValueConverter) {

        if (isNull(value)) {
            if (includeNullValues) {
                map.put(fieldName, null);
            }

            return;
        }

        if (parseBooleanToString && value instanceof Boolean) {
            map.put(fieldName, value.toString());
            return;
        }

        map.put(fieldName, mapValueConverter.toMapValue(value, this));
    }

    public static final MapValueConverter DEFAULT_MV_CONVERTER = (obj, converter) -> obj;

    public static final MapValueConverter QUERY_NODE_MV_CONVERTER = (obj, converter) ->
            TypeCastingUtils.castQueryNodeBuilder(obj).toMap(converter);

    public static final MapValueConverter LIST_OF_QUERY_NODE_MV_CONVERTER = (obj, converter) ->
            TypeCastingUtils.castListOfQueryNodeBuilders(obj).stream()
                    .map(queryNodeBuilder -> queryNodeBuilder.toMap(converter))
                    .collect(Collectors.toList());

    public static final MapValueConverter OCCUR_MV_CONVERTER = (obj, converter) -> ((Occur) obj).typeName;

    public static final MapValueConverter FLOAT_MV_CONVERTER = (obj, converter) -> {
        final Optional<Float> optionalFloat = TypeCastingUtils.castFloatOrDoubleToFloat(obj);

        if (optionalFloat.isPresent()) {
            return optionalFloat.get();
        } else {
            throw new QueryBuilderException(String.format("Float value expected: %s", obj.toString()));
        }
    };
}
