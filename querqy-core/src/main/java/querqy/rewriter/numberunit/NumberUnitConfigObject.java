/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.numberunit;

import java.util.List;

/**
 * Jackson binding target for the NumberUnitRewriter's JSON configuration.
 */
class NumberUnitConfigObject {

    private Integer scaleForLinearFunctions;
    private List<NumberUnitDefinitionObject> numberUnitDefinitions;

    public Integer getScaleForLinearFunctions() {
        return scaleForLinearFunctions;
    }

    public void setScaleForLinearFunctions(final Integer scaleForLinearFunctions) {
        this.scaleForLinearFunctions = scaleForLinearFunctions;
    }

    public List<NumberUnitDefinitionObject> getNumberUnitDefinitions() {
        return numberUnitDefinitions;
    }

    public void setNumberUnitDefinitions(final List<NumberUnitDefinitionObject> numberUnitDefinitions) {
        this.numberUnitDefinitions = numberUnitDefinitions;
    }

    public static class NumberUnitDefinitionObject {
        private List<UnitObject> units;
        private List<FieldObject> fields;
        private BoostObject boost = new BoostObject();
        private FilterObject filter = new FilterObject();

        public List<UnitObject> getUnits() {
            return units;
        }

        public void setUnits(final List<UnitObject> units) {
            this.units = units;
        }

        public List<FieldObject> getFields() {
            return fields;
        }

        public void setFields(final List<FieldObject> fields) {
            this.fields = fields;
        }

        public BoostObject getBoost() {
            return boost;
        }

        public void setBoost(final BoostObject boost) {
            this.boost = boost;
        }

        public FilterObject getFilter() {
            return filter;
        }

        public void setFilter(final FilterObject filter) {
            this.filter = filter;
        }
    }

    public static class UnitObject {
        private String term;
        private Float multiplier;

        public String getTerm() {
            return term;
        }

        public void setTerm(final String term) {
            this.term = term;
        }

        public Float getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(final Float multiplier) {
            this.multiplier = multiplier;
        }
    }

    public static class FieldObject {
        private String fieldName;
        private Integer scale;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(final String fieldName) {
            this.fieldName = fieldName;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(final Integer scale) {
            this.scale = scale;
        }
    }

    public static class BoostObject {
        private Float percentageLowerBoundary;
        private Float percentageUpperBoundary;
        private Float percentageLowerBoundaryExactMatch;
        private Float percentageUpperBoundaryExactMatch;

        private Float minScoreAtLowerBoundary;
        private Float minScoreAtUpperBoundary;
        private Float maxScoreForExactMatch;
        private Float additionalScoreForExactMatch;

        public Float getPercentageLowerBoundary() {
            return percentageLowerBoundary;
        }

        public void setPercentageLowerBoundary(final Float percentageLowerBoundary) {
            this.percentageLowerBoundary = percentageLowerBoundary;
        }

        public Float getPercentageUpperBoundary() {
            return percentageUpperBoundary;
        }

        public void setPercentageUpperBoundary(final Float percentageUpperBoundary) {
            this.percentageUpperBoundary = percentageUpperBoundary;
        }

        public Float getPercentageLowerBoundaryExactMatch() {
            return percentageLowerBoundaryExactMatch;
        }

        public void setPercentageLowerBoundaryExactMatch(final Float percentageLowerBoundaryExactMatch) {
            this.percentageLowerBoundaryExactMatch = percentageLowerBoundaryExactMatch;
        }

        public Float getPercentageUpperBoundaryExactMatch() {
            return percentageUpperBoundaryExactMatch;
        }

        public void setPercentageUpperBoundaryExactMatch(final Float percentageUpperBoundaryExactMatch) {
            this.percentageUpperBoundaryExactMatch = percentageUpperBoundaryExactMatch;
        }

        public Float getMinScoreAtLowerBoundary() {
            return minScoreAtLowerBoundary;
        }

        public void setMinScoreAtLowerBoundary(final Float minScoreAtLowerBoundary) {
            this.minScoreAtLowerBoundary = minScoreAtLowerBoundary;
        }

        public Float getMinScoreAtUpperBoundary() {
            return minScoreAtUpperBoundary;
        }

        public void setMinScoreAtUpperBoundary(final Float minScoreAtUpperBoundary) {
            this.minScoreAtUpperBoundary = minScoreAtUpperBoundary;
        }

        public Float getMaxScoreForExactMatch() {
            return maxScoreForExactMatch;
        }

        public void setMaxScoreForExactMatch(final Float maxScoreForExactMatch) {
            this.maxScoreForExactMatch = maxScoreForExactMatch;
        }

        public Float getAdditionalScoreForExactMatch() {
            return additionalScoreForExactMatch;
        }

        public void setAdditionalScoreForExactMatch(final Float additionalScoreForExactMatch) {
            this.additionalScoreForExactMatch = additionalScoreForExactMatch;
        }
    }

    public static class FilterObject {
        private Float percentageLowerBoundary;
        private Float percentageUpperBoundary;

        public Float getPercentageLowerBoundary() {
            return percentageLowerBoundary;
        }

        public void setPercentageLowerBoundary(final Float percentageLowerBoundary) {
            this.percentageLowerBoundary = percentageLowerBoundary;
        }

        public Float getPercentageUpperBoundary() {
            return percentageUpperBoundary;
        }

        public void setPercentageUpperBoundary(final Float percentageUpperBoundary) {
            this.percentageUpperBoundary = percentageUpperBoundary;
        }
    }
}
