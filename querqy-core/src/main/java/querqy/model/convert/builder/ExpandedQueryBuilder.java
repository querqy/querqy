package querqy.model.convert.builder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.BoostQuery;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.convert.BuilderFactory;
import querqy.model.convert.TypeCastingUtils;
import querqy.model.convert.model.QuerqyQueryBuilder;
import querqy.model.convert.QueryBuilderException;
import querqy.model.convert.model.QueryNodeBuilder;
import querqy.model.convert.converter.MapConverterConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static querqy.model.convert.converter.MapConverterConfig.LIST_OF_QUERY_NODE_MV_CONVERTER;
import static querqy.model.convert.converter.MapConverterConfig.QUERY_NODE_MV_CONVERTER;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ExpandedQueryBuilder implements QueryNodeBuilder<ExpandedQueryBuilder, ExpandedQuery, Void> {

    public static final String NAME_OF_QUERY_TYPE = "expanded_query";

    public static final String FIELD_NAME_USER_QUERY = "user_query";
    public static final String FIELD_NAME_FILTER_QUERIES = "filter_queries";
    public static final String FIELD_NAME_BOOST_UP_QUERIES = "boost_up_queries";
    public static final String FIELD_NAME_BOOST_DOWN_QUERIES = "boost_down_queries";
    public static final String FIELD_NAME_BOOST_MULT_QUERIES = "boost_mult_queries";

    private QuerqyQueryBuilder userQuery;
    private List<QuerqyQueryBuilder> filterQueries = emptyList();
    private List<BoostQueryBuilder> boostUpQueries = emptyList();
    private List<BoostQueryBuilder> boostDownQueries = emptyList();
    private List<BoostQueryBuilder> boostMultQueries = emptyList();

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
                    String.format("Field %s is mandatory for convert %s", "userQuery", this.getClass().getName()));
        }

        return this;
    }

    @Override
    public ExpandedQuery buildObject(final Void parent) {
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

        for (final BoostQueryBuilder boostMultQuery : boostMultQueries) {
            expandedQuery.addMultiplicativeBoostQuery(boostMultQuery.build());
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
                    .collect(toList()));
        }

        // TODO: Change expanded query to return empty list instead of null
        final Collection<BoostQuery> boostUpQueries = expanded.getBoostUpQueries();
        if (nonNull(boostUpQueries)) {
            this.setBoostUpQueries(boostUpQueries.stream().map(BoostQueryBuilder::new).collect(toList()));
        }

        // TODO: Change expanded query to return empty list instead of null
        final Collection<BoostQuery> boostDownQueries = expanded.getBoostDownQueries();
        if (nonNull(boostDownQueries)) {
            this.setBoostDownQueries(boostDownQueries.stream().map(BoostQueryBuilder::new).collect(toList()));
        }

        // TODO: Change expanded query to return empty list instead of null
        final Collection<BoostQuery> boostMultQueries = expanded.getMultiplicativeBoostQueries();
        if (nonNull(boostMultQueries)) {
            this.setBoostMultQueries(boostMultQueries.stream().map(BoostQueryBuilder::new).collect(toList()));
        }

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(final MapConverterConfig mapConverterConfig) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverterConfig.convertAndPut(map, FIELD_NAME_USER_QUERY, this.userQuery, QUERY_NODE_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_FILTER_QUERIES, this.filterQueries,
                LIST_OF_QUERY_NODE_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_BOOST_UP_QUERIES, this.boostUpQueries,
                LIST_OF_QUERY_NODE_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_BOOST_DOWN_QUERIES, this.boostDownQueries,
                LIST_OF_QUERY_NODE_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_BOOST_MULT_QUERIES, this.boostMultQueries,
                LIST_OF_QUERY_NODE_MV_CONVERTER);

        return map;
    }

    @Override
    public ExpandedQueryBuilder setAttributesFromMap(final Map map) {

        final Optional<Map> optionalUserQuery = TypeCastingUtils.castMap(map.get(FIELD_NAME_USER_QUERY));
        if (optionalUserQuery.isPresent()) {
            setUserQuery(BuilderFactory.createQuerqyQueryBuilderFromMap(optionalUserQuery.get()));

        } else {
            throw new QueryBuilderException(String.format("Creating %s requires an entry %s", NAME_OF_QUERY_TYPE,
                    FIELD_NAME_USER_QUERY));
        }

        this.setFilterQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_FILTER_QUERIES),
                BuilderFactory::createQuerqyQueryBuilderFromMap));

        this.setBoostUpQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_BOOST_UP_QUERIES),
                BoostQueryBuilder::new));
        this.setBoostDownQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_BOOST_DOWN_QUERIES),
                BoostQueryBuilder::new));
        this.setBoostMultQueries(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_BOOST_MULT_QUERIES),
                BoostQueryBuilder::new));


        return this;
    }

    public static ExpandedQueryBuilder expanded(final Map map) {
        return new ExpandedQueryBuilder(map);
    }

    public static ExpandedQueryBuilder expanded(final ExpandedQuery expanded) {
        return new ExpandedQueryBuilder(expanded);
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery,
                                                final QuerqyQueryBuilder... filters) {
        return new ExpandedQueryBuilder(userQuery, asList(filters), emptyList(), emptyList(), emptyList());
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery,
                                                final BoostQueryBuilder... boostUps) {
        return new ExpandedQueryBuilder(userQuery, emptyList(), asList(boostUps), emptyList(), emptyList());
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery,
                                                final List<QuerqyQueryBuilder> filters,
                                                final List<BoostQueryBuilder> boostUps,
                                                final List<BoostQueryBuilder> boostDowns,
                                                final List<BoostQueryBuilder> multiplicativeBoosts) {
        return new ExpandedQueryBuilder(userQuery, filters, boostUps, boostDowns, multiplicativeBoosts);
    }

    public static ExpandedQueryBuilder expanded(final QuerqyQueryBuilder userQuery) {
        return new ExpandedQueryBuilder(userQuery);
    }

}
