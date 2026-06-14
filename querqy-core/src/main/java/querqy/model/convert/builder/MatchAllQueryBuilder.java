/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.model.convert.builder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.MatchAllQuery;
import querqy.model.convert.TypeCastingUtils;
import querqy.model.convert.model.QuerqyQueryBuilder;
import querqy.model.convert.converter.MapConverterConfig;
import querqy.model.convert.model.Occur;

import java.util.LinkedHashMap;
import java.util.Map;

import static querqy.model.convert.converter.MapConverterConfig.DEFAULT_MV_CONVERTER;
import static querqy.model.convert.converter.MapConverterConfig.OCCUR_MV_CONVERTER;
import static querqy.model.convert.model.Occur.SHOULD;
import static querqy.model.convert.model.Occur.getOccurByClauseObject;

@Accessors(chain = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MatchAllQueryBuilder implements
        QuerqyQueryBuilder<MatchAllQueryBuilder, MatchAllQuery, DisjunctionMaxQuery> {

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
    public MatchAllQuery buildObject(final DisjunctionMaxQuery parent) {
        return new MatchAllQuery(parent, this.occur.objectForClause, isGenerated);
    }

    @Override
    public MatchAllQueryBuilder setAttributesFromObject(final MatchAllQuery matchAllQuery) {
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
    public MatchAllQueryBuilder setAttributesFromMap(final Map map) {
        TypeCastingUtils.castOccurByTypeName(map.get(FIELD_NAME_OCCUR)).ifPresent(this::setOccur);
        TypeCastingUtils.castStringOrBooleanToBoolean(map.get(FIELD_NAME_IS_GENERATED)).ifPresent(this::setIsGenerated);
        return this;
    }

    @Override
    public Map<String, Object> attributesToMap(final MapConverterConfig mapConverterConfig) {
        final Map<String, Object> map = new LinkedHashMap<>(2);

        mapConverterConfig.convertAndPut(map, FIELD_NAME_OCCUR, this.occur, OCCUR_MV_CONVERTER);
        mapConverterConfig.convertAndPut(map, FIELD_NAME_IS_GENERATED, this.isGenerated, DEFAULT_MV_CONVERTER);

        return map;
    }

    public static MatchAllQueryBuilder matchall(final Occur occur, final boolean isGenerated) {
        return new MatchAllQueryBuilder(occur, isGenerated);
    }

    public static MatchAllQueryBuilder matchall() {
        return new MatchAllQueryBuilder();
    }

}
