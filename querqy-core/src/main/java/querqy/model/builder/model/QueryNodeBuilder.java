package querqy.model.builder.model;

import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.converter.MapConverter;

import java.util.Collections;
import java.util.Map;

public interface QueryNodeBuilder<B extends QueryNodeBuilder, O, P> {
    String getNameOfQueryType();

    B checkMandatoryFieldValues();

    default O build() {
        return build(null);
    }

    default O build(final P parent) {
        checkMandatoryFieldValues();
        return buildObject(parent);
    }

    O buildObject(final P parent);

    B setAttributesFromObject(final O o);

    default Map<String, Object> toMap() {
        return Collections.singletonMap(getNameOfQueryType(), attributesToMap(new MapConverter()));
    }

    default Map<String, Object> toMap(final MapConverter mapConverter) {
        checkMandatoryFieldValues();

        return Collections.singletonMap(getNameOfQueryType(), attributesToMap(mapConverter));
    }

    Map<String, Object> attributesToMap(final MapConverter mapConverter);

    default B fromMap(final Map map) {
        final Object rawAttributes = map.get(getNameOfQueryType());

        if (rawAttributes instanceof Map) {
            return setAttributesFromMap((Map) rawAttributes);
        } else {
            throw new QueryBuilderException(String.format("Attributes are expected to be wrapped by %s", getNameOfQueryType()));
        }
    }

    B setAttributesFromMap(final Map map);
}
