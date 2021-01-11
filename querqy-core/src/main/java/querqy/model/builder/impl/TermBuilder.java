package querqy.model.builder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.ComparableCharSequence;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.model.builder.QueryBuilderException;
import querqy.model.builder.TypeCastingUtils;
import querqy.model.builder.model.DisjunctionMaxClauseBuilder;
import querqy.model.builder.converter.MapConverter;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static querqy.model.builder.converter.MapConverter.DEFAULT_CONVERTER;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TermBuilder implements DisjunctionMaxClauseBuilder<TermBuilder, Term> {

    public static final String NAME_OF_QUERY_TYPE = "term";

    public static final String FIELD_NAME_VALUE = "value";
    public static final String FIELD_NAME_SEARCH_FIELD = "field";
    public static final String FIELD_NAME_IS_GENERATED = "is_generated";

    private String value;
    private String field;
    private Boolean isGenerated = false;

    public TermBuilder(final Term term) {
        this.setAttributesFromObject(term);
    }

    public TermBuilder(final Map map) {
        this.fromMap(map);
    }

    public TermBuilder(final String value) {
        this.value = value;
    }

    @Override
    public String getNameOfQueryType() {
        return NAME_OF_QUERY_TYPE;
    }

    @Override
    public TermBuilder checkMandatoryFieldValues() {
        if (isNull(value)) {
            throw new QueryBuilderException(
                    String.format("Field %s is mandatory for builder %s", "rawQuery", this.getClass().getName()));
        }

        return this;
    }

    @Override
    public Term buildObject(DisjunctionMaxQuery parent) {
        return new Term(parent, field, value, isGenerated);
    }

    @Override
    public DisjunctionMaxClause buildDisjunctionMaxClause(DisjunctionMaxQuery parent) {
        return build(parent);
    }

    @Override
    public TermBuilder setAttributesFromMap(final Map map) {
        TypeCastingUtils.castString(map.get(FIELD_NAME_VALUE)).ifPresent(this::setValue);
        TypeCastingUtils.castString(map.get(FIELD_NAME_SEARCH_FIELD)).ifPresent(this::setField);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);

        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(final MapConverter mapConverter) {
        final Map<String, Object> map = new LinkedHashMap<>();

        mapConverter.convertAndPut(map, FIELD_NAME_VALUE, this.value, DEFAULT_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_SEARCH_FIELD, this.field, DEFAULT_CONVERTER);
        mapConverter.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_CONVERTER);

        return map;
    }

    @Override
    public TermBuilder setAttributesFromObject(final Term term) {
        this.setValue(term.getComparableCharSequence().toString());
        this.setField(term.getField());
        this.setIsGenerated(term.isGenerated());

        return this;
    }

    public static TermBuilder term(final String value, final String field, final Boolean isGenerated) {
        return new TermBuilder(value, field, isGenerated);
    }

    public static TermBuilder term(final String value, final boolean isGenerated) {
        return term(value, null, isGenerated);
    }

    public static TermBuilder term(final ComparableCharSequence value) {
        return term(value.toString());
    }

    public static TermBuilder term(final String value) {
        return new TermBuilder(value);
    }
}
