package querqy.model.builder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.builder.BuilderFactory;
import querqy.model.builder.TypeCastingUtils;
import querqy.model.builder.model.QuerqyQueryBuilder;
import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.model.QueryNodeBuilder;
import querqy.model.builder.converter.MapConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static querqy.model.builder.converter.MapConverter.LIST_OF_QUERY_NODE_CONVERTER;
import static querqy.model.builder.converter.MapConverter.QUERY_NODE_CONVERTER;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ExpandedQueryBuilder implements QueryNodeBuilder<ExpandedQueryBuilder, ExpandedQuery, Object> {

    public static final String NAME_OF_QUERY_TYPE = "expanded_query";

    public static final String FIELD_NAME_USER_QUERY = "user_query";
    public static final String FIELD_NAME_FILTER_QUERIES = "filter_queries";
    public static final String FIELD_NAME_BOOST_UP_QUERIES = "boost_up_queries";
    public static final String FIELD_NAME_BOOST_DOWN_QUERIES = "boost_down_queries";

    private QuerqyQueryBuilder userQuery;
    private List<QuerqyQueryBuilder> filterQueries = Collections.emptyList();
    private List<BoostQueryBuilder> boostUpQueries = Collections.emptyList();
    private List<BoostQueryBuilder> boostDownQueries = Collections.emptyList();

    public ExpandedQueryBuilder(final ExpandedQuery expanded) {
        this.setAttributesFromObject(expanded);
    }

    public ExpandedQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    public ExpandedQueryBuilder(final QuerqyQueryBuilder userQuery) {
        this.userQuery = userQuery;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public ExpandedQueryBuilder checkMandatoryFieldValues() {
        if (isNull(userQuery)) {
            throw new QueryBuilderException(
                    String.format("Field %s is mandatory for builder %s", "userQuery", this.getClass().getName()));
        }

        return this;
    }

    @Override
    public ExpandedQuery buildObject(Object parent) {
        final ExpandedQuery expandedQuery = new ExpandedQuery(userQuery.buildQuerqyQuery());

        for (final QuerqyQueryBuilder filterQuery : filterQueries) {
            expandedQuery.addFilterQuery(filterQuery.buildQuerqyQuery());
        }

        for (final BoostQueryBuilder boostUpQuery : boostUpQueries) {
            expandedQuery.addBoostUpQuery(boostUpQuery.build());
        }

        for (final BoostQueryBuilder boostDownQuery : boostDownQueries) {
            expandedQuery.addBoostDownQuery(boostDownQuery.build());
        }

        return expandedQuery;

    }

    @Override
    public ExpandedQueryBuilder setAttributesFromObject(final ExpandedQuery expanded) {
        this.setUserQuery(BuilderFactory.createQuerqyQueryBuilderFromObject(expanded.getUserQuery()));

        // TODO: Change expanded query to return empty list instead of null
        final Collection<QuerqyQuery<?>> filterQueries = expanded.getFilterQueries();
        if (nonNull(filterQueries)) {
            this.setFilterQueries(filterQueries.stream()
                    .map(BuilderFactory::createQuerqyQueryBuilderFromObject)
                    .collect(Collectors.toList()));
        }

        // TODO: Change expanded query to return empty list instead of null
        final Collection<BoostQuery> boostUpQueries = expanded.getBoostUpQueries();
        if (nonNull(boostUpQueries)) {
            this.setBoostUpQueries(boostUpQueries.stream().map(BoostQueryBuilder::new).collect(Collectors.toList()));
        }

        // TODO: Change expanded query to return empty list instead of null
        final Collection<BoostQuery> boostDownQueries = expanded.getBoostDownQueries();
        if (nonNull(boostDownQueries)) {
            this.setBoostDownQueries(boostDownQueries.stream().map(BoostQueryBuilder::new).collect(Collectors.toList()));
        }

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_USER_QUERY, this.userQuery, QUERY_NODE_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_FILTER_QUERIES, this.filterQueries, LIST_OF_QUERY_NODE_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_BOOST_UP_QUERIES, this.boostUpQueries, LIST_OF_QUERY_NODE_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_BOOST_DOWN_QUERIES, this.boostDownQueries, LIST_OF_QUERY_NODE_CONVERTER);

        return map;
    }

    @Override
    public ExpandedQueryBuilder setAttributesFromMap(final Map map) {

        final Optional<Map> optionalUserQuery = TypeCastingUtils.castMap(map.get(FIELD_NAME_USER_QUERY));
        if (optionalUserQuery.isPresent()) {
            setUserQuery(BuilderFactory.createQuerqyQueryBuilderFromMap(optionalUserQuery.get()));

        } else {
            throw new QueryBuilderException(String.format("Creating %s requires an entry %s", NAME_OF_QUERY_TYPE, FIELD_NAME_USER_QUERY));
        }

        this.setFilterQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_FILTER_QUERIES),
                BuilderFactory::createQuerqyQueryBuilderFromMap));

        this.setBoostUpQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_BOOST_UP_QUERIES), BoostQueryBuilder::new));
        this.setBoostDownQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_BOOST_DOWN_QUERIES), BoostQueryBuilder::new));

        return this;
    }

    public static ExpandedQueryBuilder expanded(final Map map) {
        return new ExpandedQueryBuilder(map);
    }

    public static ExpandedQueryBuilder expanded(final ExpandedQuery expanded) {
        return new ExpandedQueryBuilder(expanded);
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery, final QuerqyQueryBuilder... filters) {
        return new ExpandedQueryBuilder(userQuery, Arrays.asList(filters), Collections.emptyList(), Collections.emptyList());
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery, final BoostQueryBuilder... boostUps) {
        return new ExpandedQueryBuilder(userQuery, Collections.emptyList(), Arrays.asList(boostUps), Collections.emptyList());
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery,
                                                final List<QuerqyQueryBuilder> filters,
                                                final List<BoostQueryBuilder> boostUps,
                                                final List<BoostQueryBuilder> boostDowns) {
        return new ExpandedQueryBuilder(userQuery, filters, boostUps, boostDowns);
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery) {
        return new ExpandedQueryBuilder(userQuery);
    }

}
