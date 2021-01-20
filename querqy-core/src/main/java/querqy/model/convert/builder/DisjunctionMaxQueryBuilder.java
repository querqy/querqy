package querqy.model.convert.builder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.ComparableCharSequence;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.convert.BuilderFactory;
import querqy.model.convert.TypeCastingUtils;
import querqy.model.convert.model.DisjunctionMaxClauseBuilder;
import querqy.model.convert.model.QueryNodeBuilder;
import querqy.model.convert.converter.MapConverter;
import querqy.model.convert.model.Occur;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static querqy.model.convert.converter.MapConverter.DEFAULT_MV_CONVERTER;
import static querqy.model.convert.converter.MapConverter.LIST_OF_QUERY_NODE_MV_CONVERTER;
import static querqy.model.convert.converter.MapConverter.OCCUR_MV_CONVERTER;
import static querqy.model.convert.model.Occur.SHOULD;
import static querqy.model.convert.model.Occur.getOccurByClauseObject;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DisjunctionMaxQueryBuilder implements
        QueryNodeBuilder<DisjunctionMaxQueryBuilder, DisjunctionMaxQuery, BooleanQuery> {

    public static final String NAME_OF_QUERY_TYPE = "disjunction_max_query";

    public static final String FIELD_NAME_CLAUSES = "clauses";
    public static final String FIELD_NAME_OCCUR = "occur";
    public static final String FIELD_NAME_IS_GENERATED = "is_generated";

    private List<DisjunctionMaxClauseBuilder> clauses = Collections.emptyList();
    private Occur occur = SHOULD;
    private Boolean isGenerated = false;

    public DisjunctionMaxQueryBuilder(final DisjunctionMaxQuery dmq) {
        this.setAttributesFromObject(dmq);
    }

    public DisjunctionMaxQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    public DisjunctionMaxQueryBuilder(final List<DisjunctionMaxClauseBuilder> clauses) {
        this.clauses = clauses;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public DisjunctionMaxQueryBuilder checkMandatoryFieldValues() {
        return this;
    }

    @Override
    public DisjunctionMaxQuery buildObject(BooleanQuery parent) {
        final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(parent, this.occur.objectForClause, this.isGenerated);
        clauses.stream().map(clause -> clause.buildDisjunctionMaxClause(dmq)).forEach(dmq::addClause);

        return dmq;
    }

    @Override
    public DisjunctionMaxQueryBuilder setAttributesFromObject(final DisjunctionMaxQuery dmq) {
        final List<DisjunctionMaxClauseBuilder> clausesFromObject = dmq.getClauses().stream()
                .map(BuilderFactory::createDisjunctionMaxClauseBuilderFromObject)
                .collect(toList());

        this.setClauses(clausesFromObject);
        this.setOccur(getOccurByClauseObject(dmq.getOccur()));
        this.setIsGenerated(dmq.isGenerated());

        return this;
    }

    @Override
    public DisjunctionMaxQueryBuilder setAttributesFromMap(final Map map) {
        this.setClauses(TypeCastingUtils.castAndParseListOfMaps(map.get(FIELD_NAME_CLAUSES),
                BuilderFactory::createDisjunctionMaxClauseBuilderFromMap));
        TypeCastingUtils.castOccurByTypeName(map.get(FIELD_NAME_OCCUR)).ifPresent(this::setOccur);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(final MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_CLAUSES, this.clauses, LIST_OF_QUERY_NODE_MV_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_OCCUR, this.occur, OCCUR_MV_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_MV_CONVERTER);

        return map;
    }


    public static DisjunctionMaxQueryBuilder dmq(
            final List<DisjunctionMaxClauseBuilder> clauses, final Occur occur, boolean isGenerated) {
        return new DisjunctionMaxQueryBuilder(clauses, occur, isGenerated);
    }

    public static DisjunctionMaxQueryBuilder dmq(final List<DisjunctionMaxClauseBuilder> clauses) {
        return new DisjunctionMaxQueryBuilder(clauses);
    }

    public static DisjunctionMaxQueryBuilder dmq(final DisjunctionMaxClauseBuilder... clauses) {
        return new DisjunctionMaxQueryBuilder(Arrays.asList(clauses));
    }

    public static DisjunctionMaxQueryBuilder dmq(final ComparableCharSequence... terms) {
        return new DisjunctionMaxQueryBuilder(Arrays.stream(terms).map(TermBuilder::term).collect(toList()));
    }

    public static DisjunctionMaxQueryBuilder dmq(final String... terms) {
        return new DisjunctionMaxQueryBuilder(Arrays.stream(terms).map(TermBuilder::term).collect(toList()));
    }
}
