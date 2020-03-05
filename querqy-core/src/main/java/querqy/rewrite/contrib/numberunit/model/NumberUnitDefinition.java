package querqy.rewrite.contrib.numberunit.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NumberUnitDefinition {
    public final List<UnitDefinition> unitDefinitions;
    public final List<FieldDefinition> fields;
    public final BigDecimal maxScore;
    public final BigDecimal scoreUp;
    public final BigDecimal scoreDown;
    public final BigDecimal addScoreExactMatch;
    public final BigDecimal boostPercentageUp;
    public final BigDecimal boostPercentageDown;
    public final BigDecimal filterPercentageUp;
    public final BigDecimal filterPercentageDown;

    private NumberUnitDefinition(final List<UnitDefinition> unitDefinitions,
                                 final List<FieldDefinition> fields,
                                 final BigDecimal maxScore,
                                 final BigDecimal scoreUp,
                                 final BigDecimal scoreDown,
                                 final BigDecimal addScoreExactMatch,
                                 final BigDecimal boostPercentageUp,
                                 final BigDecimal boostPercentageDown,
                                 final BigDecimal filterPercentageUp,
                                 final BigDecimal filterPercentageDown) {

        this.unitDefinitions = unitDefinitions;
        this.fields = fields;
        this.maxScore = maxScore;
        this.scoreUp = scoreUp;
        this.scoreDown = scoreDown;
        this.addScoreExactMatch = addScoreExactMatch;
        this.boostPercentageUp = boostPercentageUp;
        this.boostPercentageDown = boostPercentageDown;
        this.filterPercentageUp = filterPercentageUp;
        this.filterPercentageDown = filterPercentageDown;
    }


    public static NumberUnitDefinition.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<UnitDefinition> unitDefinitions;
        private final List<FieldDefinition> fields;
        private BigDecimal maxScore;
        private BigDecimal scoreUp;
        private BigDecimal scoreDown;
        private BigDecimal addScoreExactMatch;
        private BigDecimal boostPercentageUp;
        private BigDecimal boostPercentageDown;
        private BigDecimal filterPercentageUp;
        private BigDecimal filterPercentageDown;

        private Builder() {
            this.unitDefinitions = new ArrayList<>();
            this.fields = new ArrayList<>();
        }

        public Builder addUnitDefinitions(final List<UnitDefinition> unitDefinitions) {
            this.unitDefinitions.addAll(unitDefinitions);
            return this;
        }

        public Builder addFields(List<FieldDefinition> fields) {
            this.fields.addAll(fields);
            return this;
        }

        public Builder setBoostMaxScore(BigDecimal maxScore) {
            this.maxScore = maxScore;
            return this;
        }

        public Builder setBoostScoreUp(BigDecimal scoreUp) {
            this.scoreUp = scoreUp;
            return this;
        }

        public Builder setBoostScoreDown(BigDecimal scoreDown) {
            this.scoreDown = scoreDown;
            return this;
        }

        public Builder setBoostAddScoreExactMatch(BigDecimal addScoreExactMatch) {
            this.addScoreExactMatch = addScoreExactMatch;
            return this;
        }

        public Builder setBoostPercentageUp(BigDecimal boostPercentageUp) {
            this.boostPercentageUp = boostPercentageUp;
            return this;
        }

        public Builder setBoostPercentageDown(BigDecimal boostPercentageDown) {
            this.boostPercentageDown = boostPercentageDown;
            return this;
        }

        public Builder setFilterPercentageUp(BigDecimal filterPercentageUp) {
            this.filterPercentageUp = filterPercentageUp;
            return this;
        }

        public Builder setFilterPercentageDown(BigDecimal filterPercentageDown) {
            this.filterPercentageDown = filterPercentageDown;
            return this;
        }

        public NumberUnitDefinition build() {
            return new NumberUnitDefinition(
                    Collections.unmodifiableList(this.unitDefinitions),
                    Collections.unmodifiableList(this.fields),
                    Objects.requireNonNull(maxScore),
                    Objects.requireNonNull(scoreUp),
                    Objects.requireNonNull(scoreDown),
                    Objects.requireNonNull(addScoreExactMatch),
                    Objects.requireNonNull(boostPercentageUp),
                    Objects.requireNonNull(boostPercentageDown),
                    Objects.requireNonNull(filterPercentageUp),
                    Objects.requireNonNull(filterPercentageDown)
            );
        }
    }
}
