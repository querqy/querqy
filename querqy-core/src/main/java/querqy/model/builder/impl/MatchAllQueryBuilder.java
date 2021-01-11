package querqy.model.builder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.MatchAllQuery;
import querqy.model.builder.TypeCastingUtils;
import querqy.model.builder.model.QuerqyQueryBuilder;
import querqy.model.builder.converter.MapConverter;
import querqy.model.builder.model.Occur;

import java.util.LinkedHashMap;
import java.util.Map;

import static querqy.model.builder.converter.MapConverter.DEFAULT_CONVERTER;
import static querqy.model.builder.converter.MapConverter.OCCUR_CONVERTER;
import static querqy.model.builder.model.Occur.SHOULD;
import static querqy.model.builder.model.Occur.getOccurByClauseObject;

@Accessors(chain = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MatchAllQueryBuilder implements QuerqyQueryBuilder<MatchAllQueryBuilder, MatchAllQuery, DisjunctionMaxQuery> {

    public static final String NAME_OF_QUERY_TYPE = "match_all_query";

    public static final String FIELD_NAME_OCCUR = "occur";
    public static final String FIELD_NAME_IS_GENERATED = "is_generated";

    private Occur occur = SHOULD;
    private Boolean isGenerated = false;

    public MatchAllQueryBuilder(final MatchAllQuery matchAllQuery) {
        this.setAttributesFromObject(matchAllQuery);
    }

    public MatchAllQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    @Override
    public MatchAllQuery buildObject(DisjunctionMaxQuery parent) {
        return new MatchAllQuery(parent, this.occur.objectForClause, isGenerated);
    }

    @Override
    public MatchAllQueryBuilder setAttributesFromObject(MatchAllQuery matchAllQuery) {
        this.setOccur(getOccurByClauseObject(matchAllQuery.getOccur()));
        this.setIsGenerated(matchAllQuery.isGenerated());
        return this;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public MatchAllQueryBuilder checkMandatoryFieldValues() {
        return this;
    }

    @Override
    public MatchAllQueryBuilder setAttributesFromMap(Map map) {
        TypeCastingUtils.castOccurByTypeName(map.get(FIELD_NAME_OCCUR)).ifPresent(this::setOccur);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);
        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_OCCUR, this.occur, OCCUR_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_CONVERTER);

        return map;
    }

    public static MatchAllQueryBuilder matchall(final Occur occur, final boolean isGenerated) {
        return new MatchAllQueryBuilder(occur, isGenerated);
    }

    public static MatchAllQueryBuilder matchall() {
        return new MatchAllQueryBuilder();
    }

}
