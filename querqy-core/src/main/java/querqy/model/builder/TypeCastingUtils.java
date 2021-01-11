package querqy.model.builder;

import querqy.model.builder.model.Occur;
import querqy.model.builder.model.QueryNodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static querqy.model.builder.model.Occur.getOccurByTypeName;

public class TypeCastingUtils {

    private TypeCastingUtils() {}

    public static <T> List<T> castAndParseListOfMaps(final Object rawList, final Function<Map, T> objectMapper) {
        final List<T> parsedMaps = new ArrayList<>();

        final List listOfMaps = castList(rawList).orElse(Collections.emptyList());

        for (final Object rawMap : listOfMaps) {
            final Optional<Map> optionalMap = TypeCastingUtils.castMap(rawMap);

            optionalMap.ifPresent(map -> parsedMaps.add(objectMapper.apply(map)));
        }

        return Collections.unmodifiableList(parsedMaps);
    }

    public static Optional<Float> castFloatOrDoubleToFloat(final Object obj) {
        if (obj instanceof Double) {
            return Optional.of(((Double) obj).floatValue());

        } else if (obj instanceof Float) {
            return Optional.of((Float) obj);

        } else if (isNull(obj)) {
            return Optional.empty();

        } else {
            throw new QueryBuilderException(String.format("Element %s is expected to be of type Float", obj.toString()));
        }
    }

    public static String expectMapToContainExactlyOneEntryAndGetKey(final Map map) {
        if (map.size() == 1) {
            return map.keySet().iterator().next().toString();
        }

        throw new QueryBuilderException(String.format("Map is expected to contain exactly one element: %s", map.toString()));
    }

    public static Optional<Occur> castOccurByTypeName(final Object obj) {
        final Optional<String> optionalOccurName = castString(obj);

        if (optionalOccurName.isPresent()) {
            final Occur occur = getOccurByTypeName(optionalOccurName.get());
            return Optional.of(occur);

        } else {
            return Optional.empty();
        }
    }

    public static Optional<Map> castMap(final Object obj) {
        if (obj instanceof Map) {
            return Optional.of((Map) obj);
        } else if (isNull(obj)) {
            return Optional.empty();

        } else {
            throw new QueryBuilderException(String.format("Element %s is expected to be of type Map", obj.toString()));
        }
    }

    public static Optional<List> castList(final Object obj) {
        if (obj instanceof List) {
            return Optional.of((List) obj);
        } else if (isNull(obj)) {
            return Optional.empty();

        } else {
            throw new QueryBuilderException(String.format("Element %s is expected to be of type List", obj.toString()));
        }
    }

    public static Optional<String> castString(final Object obj) {
        if (obj instanceof String) {
            return Optional.of((String) obj);

        } else if (isNull(obj)) {
            return Optional.empty();

        } else {
            throw new QueryBuilderException(String.format("Element %s is expected to be of type String", obj.toString()));
        }
    }

    // TODO: Not a casting method anymore; could be put into an own class?
    public static List<QueryNodeBuilder> castListOfQueryNodeBuilders(final Object obj) {
        final Optional<List> optionalList = castList(obj);
        if (optionalList.isPresent()) {
            final List<QueryNodeBuilder> queryNodeBuilders = new ArrayList<>();

            for (final Object rawNode : optionalList.get()) {
                queryNodeBuilders.add(castQueryNodeBuilder(rawNode));
            }

            return queryNodeBuilders;
        } else {
            throw new QueryBuilderException("Unexpected error happened parsing list of QueryNodeBuilder");
        }
    }

    public static QueryNodeBuilder castQueryNodeBuilder(final Object obj) {
        if (obj instanceof QueryNodeBuilder) {
            return (QueryNodeBuilder) obj;
        } else {
            throw new QueryBuilderException(String.format("Object %s was expected to be of type QueryNodeBuilder", obj.toString()));
        }
    }

    public static Optional<Boolean> castStringOrBooleanToBoolean(final Object obj) {
        if (obj instanceof Boolean) {
            return Optional.of((Boolean) obj);

        } else if (obj instanceof String) {
            return Optional.of(Boolean.valueOf((String) obj));

        } else if (isNull(obj)) {
            return Optional.empty();

        } else {
            throw new QueryBuilderException(String.format("Element %s is expected to be of type String or Boolean", obj.toString()));
        }
    }
}
