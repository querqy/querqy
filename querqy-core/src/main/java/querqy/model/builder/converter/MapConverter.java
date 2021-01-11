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

    private boolean parseBooleanToString = false;
    private boolean includeNullValues = false;

    public MapConverter enableParseBooleanToString() {
        this.parseBooleanToString = true;
        return this;
    }

    public MapConverter enableIncludeNullValues() {
        this.includeNullValues = true;
        return this;
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
            map.put(fieldName, String.valueOf(value));
            return;
        }

        map.put(fieldName, mapValueConverter.toMapValue(value, this));
    }

    public static final MapValueConverter DEFAULT_CONVERTER = (obj, converter) -> obj;

    public static final MapValueConverter QUERY_NODE_CONVERTER = (obj, converter) ->
            TypeCastingUtils.castQueryNodeBuilder(obj).toMap(converter);

    public static final MapValueConverter LIST_OF_QUERY_NODE_CONVERTER = (obj, converter) ->
            TypeCastingUtils.castListOfQueryNodeBuilders(obj).stream()
                    .map(queryNodeBuilder -> queryNodeBuilder.toMap(converter))
                    .collect(Collectors.toList());

    public static final MapValueConverter OCCUR_CONVERTER = (obj, converter) -> ((Occur) obj).typeName;

    public static final MapValueConverter FLOAT_CONVERTER = (obj, converter) -> {
        final Optional<Float> optionalFloat = TypeCastingUtils.castFloatOrDoubleToFloat(obj);

        if (optionalFloat.isPresent()) {
            return optionalFloat.get();
        } else {
            throw new QueryBuilderException(String.format("Float value expected: %s", obj.toString()));
        }
    };
}
