/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewrite.contrib.numberunit.model;

import querqy.model.DisjunctionMaxQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NumberUnitQueryInput {

    private final BigDecimal number;
    private final List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions;

    private final Set<DisjunctionMaxQuery> originDisjunctionMaxQueries = new HashSet<>();

    public NumberUnitQueryInput(final BigDecimal number) {
        this(number, new ArrayList<>());
    }

    public NumberUnitQueryInput(final BigDecimal number,
                                final List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        this.number = number;
        this.perUnitNumberUnitDefinitions = perUnitNumberUnitDefinitions;
    }

    public boolean hasUnit() {
        return !this.perUnitNumberUnitDefinitions.isEmpty();
    }

    public void addOriginDisjunctionMaxQuery(final DisjunctionMaxQuery dmq) {
        this.originDisjunctionMaxQueries.add(dmq);
    }

    public Set<DisjunctionMaxQuery> getOriginDisjunctionMaxQuery() {
        return Collections.unmodifiableSet(this.originDisjunctionMaxQueries);
    }

    public BigDecimal getNumber() {
        return this.number;
    }

    public List<PerUnitNumberUnitDefinition> getPerUnitNumberUnitDefinitions() {
        return perUnitNumberUnitDefinitions;
    }

    public void addPerUnitNumberUnitDefinitions(final List<PerUnitNumberUnitDefinition> perUnitNumberUnitDefinitions) {
        this.perUnitNumberUnitDefinitions.addAll(perUnitNumberUnitDefinitions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumberUnitQueryInput that = (NumberUnitQueryInput) o;
        return Objects.equals(number.doubleValue(), that.number.doubleValue()) &&
                Objects.equals(perUnitNumberUnitDefinitions, that.perUnitNumberUnitDefinitions) &&
                Objects.equals(originDisjunctionMaxQueries, that.originDisjunctionMaxQueries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, perUnitNumberUnitDefinitions, originDisjunctionMaxQueries);
    }

    @Override
    public String toString() {
        return "NumberUnitQueryInput{" +
                "number=" + number +
                ", perUnitNumberUnitDefinitions=" + perUnitNumberUnitDefinitions +
                '}';
    }
}
