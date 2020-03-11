package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NumberUnitDefinition {
    public final List<UnitDefinition> unitDefinitions;
    public final List<FieldDefinition> fields;
    public final BigDecimal maxScoreForExactMatch;
    public final BigDecimal minScoreAtUpperBoundary;
    public final BigDecimal minScoreAtLowerBoundary;
    public final BigDecimal additionalScoreForExactMatch;

    public final BigDecimal boostPercentageUpperBoundary;
    public final BigDecimal boostPercentageLowerBoundary;
    public final BigDecimal boostPercentageUpperBoundaryExactMatch;
    public final BigDecimal boostPercentageLowerBoundaryExactMatch;

    public final BigDecimal filterPercentageUpperBoundary;
    public final BigDecimal filterPercentageLowerBoundary;

    private NumberUnitDefinition(final List<UnitDefinition> unitDefinitions,
                                 final List<FieldDefinition> fields,

                                 final BigDecimal maxScoreForExactMatch,
                                 final BigDecimal minScoreAtUpperBoundary,
                                 final BigDecimal minScoreAtLowerBoundary,
                                 final BigDecimal additionalScoreForExactMatch,

                                 final BigDecimal boostPercentageUpperBoundary,
                                 final BigDecimal boostPercentageLowerBoundary,
                                 final BigDecimal boostPercentageUpperBoundaryExactMatch,
                                 final BigDecimal boostPercentageLowerBoundaryExactMatch,

                                 final BigDecimal filterPercentageUpperBoundary,
                                 final BigDecimal filterPercentageLowerBoundary) {

        this.unitDefinitions = unitDefinitions;
        this.fields = fields;

        this.maxScoreForExactMatch = maxScoreForExactMatch;
        this.minScoreAtUpperBoundary = minScoreAtUpperBoundary;
        this.minScoreAtLowerBoundary = minScoreAtLowerBoundary;
        this.additionalScoreForExactMatch = additionalScoreForExactMatch;

        this.boostPercentageUpperBoundary = boostPercentageUpperBoundary;
        this.boostPercentageLowerBoundary = boostPercentageLowerBoundary;
        this.boostPercentageUpperBoundaryExactMatch = boostPercentageUpperBoundaryExactMatch;
        this.boostPercentageLowerBoundaryExactMatch = boostPercentageLowerBoundaryExactMatch;

        this.filterPercentageUpperBoundary = filterPercentageUpperBoundary;
        this.filterPercentageLowerBoundary = filterPercentageLowerBoundary;
    }


    public static NumberUnitDefinition.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<UnitDefinition> unitDefinitions;
        private final List<FieldDefinition> fields;
        private BigDecimal maxScoreForExactMatch;
        private BigDecimal minScoreAtUpperBoundary;
        private BigDecimal minScoreAtLowerBoundary;
        private BigDecimal additionalScoreForExactMatch;

        private BigDecimal boostPercentageUpperBoundary;
        private BigDecimal boostPercentageLowerBoundary;
        private BigDecimal boostPercentageUpperBoundaryExactMatch;
        private BigDecimal boostPercentageLowerBoundaryExactMatch;

        private BigDecimal filterPercentageUpperBoundary;
        private BigDecimal filterPercentageLowerBoundary;

        private Builder() {
            this.unitDefinitions = new ArrayList<>();
            this.fields = new ArrayList<>();
        }

        public Builder addUnits(List<UnitDefinition> unitDefinitions) {
            this.unitDefinitions.addAll(unitDefinitions);
            return this;
        }

        public Builder addFields(List<FieldDefinition> fieldDefinitions) {
            this.fields.addAll(fieldDefinitions);
            return this;
        }

        public Builder setMaxScoreForExactMatch(BigDecimal maxScoreForExactMatch) {
            this.maxScoreForExactMatch = maxScoreForExactMatch;
            return this;
        }

        public Builder setMinScoreAtUpperBoundary(BigDecimal minScoreAtUpperBoundary) {
            this.minScoreAtUpperBoundary = minScoreAtUpperBoundary;
            return this;
        }

        public Builder setMinScoreAtLowerBoundary(BigDecimal minScoreAtLowerBoundary) {
            this.minScoreAtLowerBoundary = minScoreAtLowerBoundary;
            return this;
        }

        public Builder setAdditionalScoreForExactMatch(BigDecimal additionalScoreForExactMatch) {
            this.additionalScoreForExactMatch = additionalScoreForExactMatch;
            return this;
        }

        public Builder setBoostPercentageUpperBoundary(BigDecimal boostPercentageUpperBoundary) {
            this.boostPercentageUpperBoundary = boostPercentageUpperBoundary;
            return this;
        }

        public Builder setBoostPercentageLowerBoundary(BigDecimal boostPercentageLowerBoundary) {
            this.boostPercentageLowerBoundary = boostPercentageLowerBoundary;
            return this;
        }

        public Builder setBoostPercentageUpperBoundaryExactMatch(BigDecimal boostPercentageUpperBoundaryExactMatch) {
            this.boostPercentageUpperBoundaryExactMatch = boostPercentageUpperBoundaryExactMatch;
            return this;
        }

        public Builder setBoostPercentageLowerBoundaryExactMatch(BigDecimal boostPercentageLowerBoundaryExactMatch) {
            this.boostPercentageLowerBoundaryExactMatch = boostPercentageLowerBoundaryExactMatch;
            return this;
        }

        public Builder setFilterPercentageUpperBoundary(BigDecimal filterPercentageUpperBoundary) {
            this.filterPercentageUpperBoundary = filterPercentageUpperBoundary;
            return this;
        }

        public Builder setFilterPercentageLowerBoundary(BigDecimal filterPercentageLowerBoundary) {
            this.filterPercentageLowerBoundary = filterPercentageLowerBoundary;
            return this;
        }

        public NumberUnitDefinition build() {
            return new NumberUnitDefinition(
                    Collections.unmodifiableList(this.unitDefinitions),
                    Collections.unmodifiableList(this.fields),
                    Objects.requireNonNull(maxScoreForExactMatch),
                    Objects.requireNonNull(minScoreAtUpperBoundary),
                    Objects.requireNonNull(minScoreAtLowerBoundary),
                    Objects.requireNonNull(additionalScoreForExactMatch),
                    Objects.requireNonNull(boostPercentageUpperBoundary),
                    Objects.requireNonNull(boostPercentageLowerBoundary),
                    Objects.requireNonNull(boostPercentageUpperBoundaryExactMatch),
                    Objects.requireNonNull(boostPercentageLowerBoundaryExactMatch),
                    Objects.requireNonNull(filterPercentageUpperBoundary),
                    Objects.requireNonNull(filterPercentageLowerBoundary)
            );
        }
    }
}
