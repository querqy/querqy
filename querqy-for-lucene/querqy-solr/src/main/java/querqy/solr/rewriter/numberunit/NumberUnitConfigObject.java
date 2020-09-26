package querqy.solr.rewriter.numberunit;

import java.util.List;

public class NumberUnitConfigObject {
    private Integer scaleForLinearFunctions;
    private List<NumberUnitDefinitionObject> numberUnitDefinitions;

    public Integer getScaleForLinearFunctions() {
        return scaleForLinearFunctions;
    }

    public void setScaleForLinearFunctions(Integer scaleForLinearFunctions) {
        this.scaleForLinearFunctions = scaleForLinearFunctions;
    }

    public List<NumberUnitDefinitionObject> getNumberUnitDefinitions() {
        return numberUnitDefinitions;
    }

    public void setNumberUnitDefinitions(List<NumberUnitDefinitionObject> numberUnitDefinitions) {
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

        public void setUnits(List<UnitObject> units) {
            this.units = units;
        }

        public List<FieldObject> getFields() {
            return fields;
        }

        public void setFields(List<FieldObject> fields) {
            this.fields = fields;
        }

        public BoostObject getBoost() {
            return boost;
        }

        public void setBoost(BoostObject boost) {
            this.boost = boost;
        }

        public FilterObject getFilter() {
            return filter;
        }

        public void setFilter(FilterObject filter) {
            this.filter = filter;
        }
    }

    public static class UnitObject {
        private String term;
        private Float multiplier;

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Float getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Float multiplier) {
            this.multiplier = multiplier;
        }
    }

    public static class FieldObject {
        private String fieldName;
        private Integer scale;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(Integer scale) {
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

        public void setPercentageLowerBoundary(Float percentageLowerBoundary) {
            this.percentageLowerBoundary = percentageLowerBoundary;
        }

        public Float getPercentageUpperBoundary() {
            return percentageUpperBoundary;
        }

        public void setPercentageUpperBoundary(Float percentageUpperBoundary) {
            this.percentageUpperBoundary = percentageUpperBoundary;
        }

        public Float getPercentageLowerBoundaryExactMatch() {
            return percentageLowerBoundaryExactMatch;
        }

        public void setPercentageLowerBoundaryExactMatch(Float percentageLowerBoundaryExactMatch) {
            this.percentageLowerBoundaryExactMatch = percentageLowerBoundaryExactMatch;
        }

        public Float getPercentageUpperBoundaryExactMatch() {
            return percentageUpperBoundaryExactMatch;
        }

        public void setPercentageUpperBoundaryExactMatch(Float percentageUpperBoundaryExactMatch) {
            this.percentageUpperBoundaryExactMatch = percentageUpperBoundaryExactMatch;
        }

        public Float getMinScoreAtLowerBoundary() {
            return minScoreAtLowerBoundary;
        }

        public void setMinScoreAtLowerBoundary(Float minScoreAtLowerBoundary) {
            this.minScoreAtLowerBoundary = minScoreAtLowerBoundary;
        }

        public Float getMinScoreAtUpperBoundary() {
            return minScoreAtUpperBoundary;
        }

        public void setMinScoreAtUpperBoundary(Float minScoreAtUpperBoundary) {
            this.minScoreAtUpperBoundary = minScoreAtUpperBoundary;
        }

        public Float getMaxScoreForExactMatch() {
            return maxScoreForExactMatch;
        }

        public void setMaxScoreForExactMatch(Float maxScoreForExactMatch) {
            this.maxScoreForExactMatch = maxScoreForExactMatch;
        }

        public Float getAdditionalScoreForExactMatch() {
            return additionalScoreForExactMatch;
        }

        public void setAdditionalScoreForExactMatch(Float additionalScoreForExactMatch) {
            this.additionalScoreForExactMatch = additionalScoreForExactMatch;
        }
    }

    public static class FilterObject {
        private Float percentageLowerBoundary;
        private Float percentageUpperBoundary;

        public Float getPercentageLowerBoundary() {
            return percentageLowerBoundary;
        }

        public void setPercentageLowerBoundary(Float percentageLowerBoundary) {
            this.percentageLowerBoundary = percentageLowerBoundary;
        }

        public Float getPercentageUpperBoundary() {
            return percentageUpperBoundary;
        }

        public void setPercentageUpperBoundary(Float percentageUpperBoundary) {
            this.percentageUpperBoundary = percentageUpperBoundary;
        }
    }
}
