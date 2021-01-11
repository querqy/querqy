package querqy.model.builder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.BoostQuery;
import querqy.model.builder.BuilderFactory;
import querqy.model.builder.model.QuerqyQueryBuilder;
import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.model.QueryNodeBuilder;
import querqy.model.builder.converter.MapConverter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static querqy.model.builder.TypeCastingUtils.castFloatOrDoubleToFloat;
import static querqy.model.builder.TypeCastingUtils.castMap;
import static querqy.model.builder.converter.MapConverter.FLOAT_CONVERTER;
import static querqy.model.builder.converter.MapConverter.QUERY_NODE_CONVERTER;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BoostQueryBuilder implements QueryNodeBuilder<BoostQueryBuilder, BoostQuery, Object> {

    public static final String NAME_OF_QUERY_TYPE = "boost_query";

    public static final String FIELD_NAME_QUERY = "query";
    public static final String FIELD_NAME_BOOST = "boost";

    private QuerqyQueryBuilder querqyQueryBuilder;
    private Float boost = 1.0f;

    public BoostQueryBuilder(final BoostQuery boostQuery) {
        this.setAttributesFromObject(boostQuery);
    }

    public BoostQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    public BoostQueryBuilder(final QuerqyQueryBuilder querqyQueryBuilder) {
        this.querqyQueryBuilder = querqyQueryBuilder;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public BoostQueryBuilder checkMandatoryFieldValues() {
        if (isNull(querqyQueryBuilder)) {
            throw new QueryBuilderException(
                    String.format("Field %s is mandatory for builder %s", "querqyQueryBuilder", this.getClass().getName()));
        }

        return this;
    }

    @Override
    public BoostQuery buildObject(Object parent) {
        return new BoostQuery(querqyQueryBuilder.buildQuerqyQuery(), this.boost);
    }

    @Override
    public BoostQueryBuilder setAttributesFromObject(final BoostQuery boostQuery) {
        this.setQuerqyQueryBuilder(BuilderFactory.createQuerqyQueryBuilderFromObject(boostQuery.getQuery()));
        this.setBoost(boostQuery.getBoost());

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_QUERY, this.querqyQueryBuilder, QUERY_NODE_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_BOOST, this.boost, FLOAT_CONVERTER);

        return map;
    }

    @Override
    public BoostQueryBuilder setAttributesFromMap(final Map map) {
        final Optional<Map> optionalQuerqyQuery = castMap(map.get(FIELD_NAME_QUERY));

        if (optionalQuerqyQuery.isPresent()) {
            this.setQuerqyQueryBuilder(BuilderFactory.createQuerqyQueryBuilderFromMap(optionalQuerqyQuery.get()));
        } else {
            throw new QueryBuilderException("The query of a boost query must not be null");
        }

        castFloatOrDoubleToFloat(map.get(FIELD_NAME_BOOST)).ifPresent(this::setBoost);

        return this;
    }

    public static BoostQueryBuilder boost(final QuerqyQueryBuilder querqyQueryBuilder) {
        return new BoostQueryBuilder(querqyQueryBuilder);
    }

    public static BoostQueryBuilder boost(final QuerqyQueryBuilder querqyQueryBuilder, final Float boost) {
        return new BoostQueryBuilder(querqyQueryBuilder, boost);
    }
}
