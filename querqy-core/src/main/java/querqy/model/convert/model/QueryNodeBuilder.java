package querqy.model.convert.model;

import querqy.model.Node;
import querqy.model.convert.QueryBuilderException;
import querqy.model.convert.converter.MapConverter;

import java.util.Collections;
import java.util.Map;

/**
 * Builds objects of type T with parent P. Defines methods to read
 * and apply attributes from a Map or objects of type T.
 *
 * P and T are normally {@link Node} implementation types.
 *
 * @param <B>The QueryNodeBuilder type
 * @param <T>The object type produced by this convert
 * @param <P>The parent type
 */
public interface QueryNodeBuilder<B extends QueryNodeBuilder, T, P> {

    String getNameOfQueryType();

    B checkMandatoryFieldValues();

    default T build() {
        return build(null);
    }

    default T build(final P parent) {
        checkMandatoryFieldValues();
        return buildObject(parent);
    }

    T buildObject(final P parent);

    B setAttributesFromObject(final T o);

    default Map<String, Object> toMap() {
        return toMap(MapConverter.DEFAULT_MAP_CONVERTER);
    }

    default Map<String, Object> toMap(final MapConverter mapConverter) {
        checkMandatoryFieldValues();

        return Collections.singletonMap(getNameOfQueryType(), attributesToMap(mapConverter));
    }

    Map<String, Object> attributesToMap(final MapConverter mapConverter);

    /**
     * Expects a map containing an entry for key {@link #getNameOfQueryType()}. The value of that
     * entry must be again be a map that contains the attributes for this convert, which should be
     * read and applied.
     *
     * @param map
     * @return
     */
    default B fromMap(final Map map) {
        final Object rawAttributes = map.get(getNameOfQueryType());

        if (rawAttributes instanceof Map) {
            return setAttributesFromMap((Map) rawAttributes);
        } else {
            throw new QueryBuilderException(String.format("Attributes are expected to be wrapped by %s",
                    getNameOfQueryType()));
        }
    }

    B setAttributesFromMap(final Map map);
}
