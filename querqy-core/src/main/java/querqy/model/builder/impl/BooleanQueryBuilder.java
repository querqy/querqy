package querqy.model.builder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.builder.TypeCastingUtils;
import querqy.model.builder.model.DisjunctionMaxClauseBuilder;
import querqy.model.builder.model.QuerqyQueryBuilder;
import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.converter.MapConverter;
import querqy.model.builder.model.Occur;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static querqy.model.builder.converter.MapConverter.DEFAULT_CONVERTER;
import static querqy.model.builder.converter.MapConverter.LIST_OF_QUERY_NODE_CONVERTER;
import static querqy.model.builder.converter.MapConverter.OCCUR_CONVERTER;
import static querqy.model.builder.model.Occur.SHOULD;
import static querqy.model.builder.model.Occur.getOccurByClauseObject;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BooleanQueryBuilder implements DisjunctionMaxClauseBuilder<BooleanQueryBuilder, BooleanQuery>,
        QuerqyQueryBuilder<BooleanQueryBuilder, BooleanQuery, DisjunctionMaxQuery> {

    public static final String NAME_OF_QUERY_TYPE = "boolean_query";

    public static final String FIELD_NAME_CLAUSES = "clauses";
    public static final String FIELD_NAME_OCCUR = "occur";
    public static final String FIELD_NAME_IS_GENERATED = "is_generated";

    private List<DisjunctionMaxQueryBuilder> clauses = Collections.emptyList();
    private Occur occur = SHOULD;
    private Boolean isGenerated = false;

    public BooleanQueryBuilder(final BooleanQuery bq) {
        this.setAttributesFromObject(bq);
    }

    public BooleanQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    public BooleanQueryBuilder(final List<DisjunctionMaxQueryBuilder> clauses) {
        this.clauses = clauses;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public BooleanQueryBuilder checkMandatoryFieldValues() {
        return this;
    }

    @Override
    public BooleanQuery buildObject(final DisjunctionMaxQuery parent) {
        final BooleanQuery bq = new BooleanQuery(parent, this.occur.objectForClause, this.isGenerated);
        clauses.stream().map(dmq -> dmq.build(bq)).forEach(bq::addClause);
        return bq;
    }

    @Override
    public QuerqyQuery<?> buildQuerqyQuery() {
        checkMandatoryFieldValues();

        final Query query = new Query(isGenerated);
        clauses.stream().map(clause -> clause.build(query)).forEach(query::addClause);
        return query;
    }

    @Override
    public BooleanQueryBuilder setAttributesFromObject(final BooleanQuery bq) {

        final List<DisjunctionMaxQueryBuilder> clausesFromObject = bq.getClauses().stream()
                .map(clause -> {

                    if (clause instanceof DisjunctionMaxQuery) {
                        return new DisjunctionMaxQueryBuilder((DisjunctionMaxQuery) clause);

                    } else {
                        throw new QueryBuilderException("The structure of this query is currently not supported by builders");
                    }})

                .collect(Collectors.toList());

        this.setClauses(clausesFromObject);
        this.setOccur(getOccurByClauseObject(bq.getOccur()));
        this.setIsGenerated(bq.isGenerated());

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_CLAUSES, this.clauses, LIST_OF_QUERY_NODE_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_OCCUR, this.occur, OCCUR_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_CONVERTER);

        return map;
    }

    @Override
    public BooleanQueryBuilder setAttributesFromMap(final Map map) {
        this.setClauses(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_CLAUSES), DisjunctionMaxQueryBuilder::new));

        TypeCastingUtils.castOccurByTypeName(map.get(FIELD_NAME_OCCUR)).ifPresent(this::setOccur);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);

        return this;
    }

    public static BooleanQueryBuilder bq(final List<DisjunctionMaxQueryBuilder> dmqs, final Occur occur, boolean isGenerated) {
        return new BooleanQueryBuilder(dmqs, occur, isGenerated);
    }

    public static BooleanQueryBuilder bq(final List<DisjunctionMaxQueryBuilder> dmqs) {
        return new BooleanQueryBuilder(dmqs);
    }

    public static BooleanQueryBuilder bq(final DisjunctionMaxQueryBuilder... dmqs) {
        return bq(Arrays.stream(dmqs).collect(Collectors.toList()));
    }

    public static BooleanQueryBuilder bq(final String... dmqs) {
        return bq(Arrays.stream(dmqs).map(DisjunctionMaxQueryBuilder::dmq).collect(Collectors.toList()));
    }
}
