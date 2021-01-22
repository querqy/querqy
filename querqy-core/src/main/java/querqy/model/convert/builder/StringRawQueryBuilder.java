package querqy.model.convert.builder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.QuerqyQuery;
import querqy.model.StringRawQuery;
import querqy.model.convert.QueryBuilderException;
import querqy.model.convert.TypeCastingUtils;
import querqy.model.convert.model.QuerqyQueryBuilder;
import querqy.model.convert.converter.MapConverterConfig;
import querqy.model.convert.model.Occur;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static querqy.model.convert.converter.MapConverterConfig.DEFAULT_MV_CONVERTER;
import static querqy.model.convert.converter.MapConverterConfig.OCCUR_MV_CONVERTER;
import static querqy.model.convert.model.Occur.SHOULD;
import static querqy.model.convert.model.Occur.getOccurByClauseObject;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class StringRawQueryBuilder implements
        QuerqyQueryBuilder<StringRawQueryBuilder, StringRawQuery, DisjunctionMaxQuery> {

    public static final String NAME_OF_QUERY_TYPE = "string_raw_query";

    public static final String FIELD_NAME_RAW_QUERY = "raw_query";
    public static final String FIELD_NAME_OCCUR = "occur";
    public static final String FIELD_NAME_IS_GENERATED = "is_generated";

    private String rawQuery;
    private Occur occur = SHOULD;
    private Boolean isGenerated = false;

    public StringRawQueryBuilder(final StringRawQuery stringRawQuery) {
        this.setAttributesFromObject(stringRawQuery);
    }

    public StringRawQueryBuilder(final Map map) {
        this.fromMap(map);
    }

    public StringRawQueryBuilder(final String rawQuery) {
        this.setRawQuery(rawQuery);
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public StringRawQueryBuilder checkMandatoryFieldValues() {
        if (isNull(rawQuery)) {
            throw new QueryBuilderException(
                    String.format("Field %s is mandatory for convert %s", "rawQuery", this.getClass().getName()));
        }

        return this;
    }

    @Override
    public QuerqyQuery<?> buildQuerqyQuery() {
        return build(null);
    }

    @Override
    public StringRawQuery buildObject(final DisjunctionMaxQuery parent) {
        return new StringRawQuery(parent, this.rawQuery, this.occur.objectForClause, this.isGenerated);
    }

    @Override
    public StringRawQueryBuilder setAttributesFromObject(final StringRawQuery stringRawQuery) {
        this.setRawQuery(stringRawQuery.getQueryString());
        this.setOccur(getOccurByClauseObject(stringRawQuery.getOccur()));
        this.setIsGenerated(stringRawQuery.isGenerated());

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(final MapConverterConfig mapConverterConfig) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverterConfig.convertAndPut(map, FIELD_NAME_RAW_QUERY, this.rawQuery, DEFAULT_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_OCCUR, this.occur, OCCUR_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_MV_CONVERTER);

        return map;
    }

    @Override
    public StringRawQueryBuilder setAttributesFromMap(final Map map) {
        TypeCastingUtils.castString(map.get(FIELD_NAME_RAW_QUERY)).ifPresent(this::setRawQuery);
        TypeCastingUtils.castOccurByTypeName(map.get(FIELD_NAME_OCCUR)).ifPresent(this::setOccur);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);

        return this;
    }

    public static StringRawQueryBuilder raw(final String rawQueryString) {
        return new StringRawQueryBuilder(rawQueryString);
    }
}
