package querqy.model.builder.converter;

import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.TypeCastingUtils;
import querqy.model.builder.model.QueryNodeBuilder;
import querqy.model.builder.model.Occur;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class MapConverter {

    public static final MapConverter DEFAULT_MAP_CONVERTER = new MapConverter(false, false);
    public static final MapConverter MAP_CONVERTER_NULL_VALUES = new MapConverter(false, true);
    public static final MapConverter MAP_CONVERTER_BOOL_STRING = new MapConverter(true, false);
    public static final MapConverter MAP_CONVERTER_BOOL_STRING_NULL_VALUES = new MapConverter(true, true);

    private final boolean parseBooleanToString;
    private final boolean includeNullValues;

    protected MapConverter(final boolean parseBooleanToString, final boolean includeNullValues) {
        this.parseBooleanToString = parseBooleanToString;
        this.includeNullValues = includeNullValues;
    }

    public Map convertQueryBuilderToMap(final QueryNodeBuilder queryBuilder) {
        return queryBuilder.toMap(this);
    }

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
