package querqy.solr.contrib.numberunit;

import org.junit.Test;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.rewrite.contrib.numberunit.model.FieldDefinition;
import querqy.rewrite.contrib.numberunit.model.LinearFunction;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberUnitQueryCreatorSolrTest {

    private NumberUnitQueryCreatorSolr numberUnitQueryCreator = new NumberUnitQueryCreatorSolr(3);

    @Test
    public void testCreateBoostQueryWithExactMatchRange() {
        StringRawQuery rawBoostQuery;

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 2)),
                                20, 20, 5, 5,
                                20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40.00 u=50.00 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!frange l=47.50 u=52.50 v='screen_size'}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 0)),
                                20, 20, 5, 5,
                                20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40 u=50 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!frange l=48 u=53 v='screen_size'}),30," +
                        "if(query({!frange l=50 u=60 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");
    }

    @Test
    public void testCreateBoostQuery() {
        StringRawQuery rawBoostQuery;

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 2)),
                                20, 20, 0, 0,
                                20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40.00 u=50.00 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!frange l=50.00 u=50.00 v='screen_size'}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 0)),
                                20, 20, 0, 0,
                                20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40 u=50 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!frange l=50 u=50 v='screen_size'}),30," +
                        "if(query({!frange l=50 u=60 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Arrays.asList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Arrays.asList(new FieldDefinition("f1", 2), new FieldDefinition("f2", 2)),
                                20, 20, 0, 0,
                                20, 10, 1.0),
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("f3", 2)),
                                20, 20, 0, 0,
                                20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}max(" +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f1'})," +
                        "rint(linear(f1,1.000,-30.000))," +
                        "if(query({!frange l=50.00 u=50.00 v='f1'}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f1'})," +
                        "rint(linear(f1,-1.000,70.000)),0)))," +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f2'})," +
                        "rint(linear(f2,1.000,-30.000))," +
                        "if(query({!frange l=50.00 u=50.00 v='f2'}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f2'})," +
                        "rint(linear(f2,-1.000,70.000)),0)))," +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f3'})," +
                        "rint(linear(f3,1.000,-30.000))," +
                        "if(query({!frange l=50.00 u=50.00 v='f3'}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f3'})," +
                        "rint(linear(f3,-1.000,70.000)),0))))");
    }

    @Test
    public void testCreateFilterQuery() {
        StringRawQuery filterQuery;

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Arrays.asList(
                        createPerUnitNumberUnitDefinitionForFilters(
                                Arrays.asList(new FieldDefinition("f1", 2), new FieldDefinition("f2", 2)),
                                20, 20, 0.5),
                        createPerUnitNumberUnitDefinitionForFilters(Collections.singletonList(new FieldDefinition("f3", 2)),
                                30, 30, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[40.00 TO 60.00] OR f2:[40.00 TO 60.00] OR f3:[70.00 TO 130.00]");

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Arrays.asList(
                        createPerUnitNumberUnitDefinitionForFilters(
                                Arrays.asList(new FieldDefinition("f1", 0), new FieldDefinition("f2", 0)),
                                20, 20, 0.5),
                        createPerUnitNumberUnitDefinitionForFilters(Collections.singletonList(new FieldDefinition("f3", 2)),
                                30, 30, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[40 TO 60] OR f2:[40 TO 60] OR f3:[70.00 TO 130.00]");

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Arrays.asList(
                        createPerUnitNumberUnitDefinitionForFilters(
                                Arrays.asList(new FieldDefinition("f1", 2), new FieldDefinition("f2", 2)),
                                20, 20, 0.5),
                        createPerUnitNumberUnitDefinitionForFilters(
                                Arrays.asList(new FieldDefinition("f3", 2), new FieldDefinition("f4", 2)),
                                30, 30, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[40.00 TO 60.00] OR f2:[40.00 TO 60.00] OR f3:[70.00 TO 130.00] OR f4:[70.00 TO 130.00]");

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Collections.singletonList(createPerUnitNumberUnitDefinitionForFilters(
                        Collections.singletonList(new FieldDefinition("f1", 2)), -1, 30, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[70.00 TO *]");

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Collections.singletonList(createPerUnitNumberUnitDefinitionForFilters(
                        Collections.singletonList(new FieldDefinition("f1", 2)), 30, -1, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[* TO 130.00]");

        filterQuery = numberUnitQueryCreator.createFilterQuery(
                BigDecimal.valueOf(100),
                Collections.singletonList(createPerUnitNumberUnitDefinitionForFilters(
                        Collections.singletonList(new FieldDefinition("f1", 2)), -1, -1, 1.0)));

        assertThat(filterQuery.getQueryString()).isEqualTo("f1:[* TO *]");

    }

    private PerUnitNumberUnitDefinition createPerUnitNumberUnitDefinitionForFilters(
            List<FieldDefinition> fields, double percentageUp, double percentageDown, double multiplier) {
        return new PerUnitNumberUnitDefinition(
                NumberUnitDefinition.builder()
                        .addUnits(Collections.emptyList())
                        .addFields(fields)
                        .setFilterPercentageUpperBoundary(BigDecimal.valueOf(percentageUp))
                        .setFilterPercentageLowerBoundary(BigDecimal.valueOf(percentageDown))
                        .setMaxScoreForExactMatch(BigDecimal.ONE)
                        .setAdditionalScoreForExactMatch(BigDecimal.ONE)
                        .setMinScoreAtUpperBoundary(BigDecimal.ONE)
                        .setMinScoreAtLowerBoundary(BigDecimal.ONE)
                        .setBoostPercentageUpperBoundary(BigDecimal.ONE)
                        .setBoostPercentageLowerBoundary(BigDecimal.ONE)
                        .setBoostPercentageUpperBoundaryExactMatch(BigDecimal.ONE)
                        .setBoostPercentageLowerBoundaryExactMatch(BigDecimal.ONE)
                        .build(),
                BigDecimal.valueOf(multiplier));
    }

    private PerUnitNumberUnitDefinition createPerUnitNumberUnitDefinitionForBoosts(
            List<FieldDefinition> fields, double percentageUp, double percentageDown, double percentageUpExact, double percentageDownExact,
            double maxScore, double minScore, double multiplier) {
        return new PerUnitNumberUnitDefinition(
                NumberUnitDefinition.builder()
                        .addUnits(Collections.emptyList())
                        .addFields(fields)
                        .setMaxScoreForExactMatch(BigDecimal.valueOf(maxScore))
                        .setAdditionalScoreForExactMatch(BigDecimal.valueOf(10))
                        .setMinScoreAtUpperBoundary(BigDecimal.valueOf(minScore))
                        .setMinScoreAtLowerBoundary(BigDecimal.valueOf(minScore))
                        .setBoostPercentageUpperBoundary(BigDecimal.valueOf(percentageUp))
                        .setBoostPercentageLowerBoundary(BigDecimal.valueOf(percentageDown))
                        .setBoostPercentageUpperBoundaryExactMatch(BigDecimal.valueOf(percentageUpExact))
                        .setBoostPercentageLowerBoundaryExactMatch(BigDecimal.valueOf(percentageDownExact))
                        .setFilterPercentageUpperBoundary(BigDecimal.ONE)
                        .setFilterPercentageLowerBoundary(BigDecimal.ONE)
                        .build(),
                BigDecimal.valueOf(multiplier));
    }

    @Test
    public void testCreateLinearFunctionParameters() {
        LinearFunction result;
        result = numberUnitQueryCreator.createLinearFunctionParameters(
                BigDecimal.valueOf(55), BigDecimal.valueOf(15),
                BigDecimal.valueOf(50), BigDecimal.valueOf(10));
        assertThat(result.m.doubleValue()).isEqualTo(1);
        assertThat(result.b.doubleValue()).isEqualTo(-40);

        result = numberUnitQueryCreator.createLinearFunctionParameters(
                BigDecimal.valueOf(50), BigDecimal.valueOf(10),
                BigDecimal.valueOf(55), BigDecimal.valueOf(15));
        assertThat(result.m.doubleValue()).isEqualTo(1);
        assertThat(result.b.doubleValue()).isEqualTo(-40);

        result = numberUnitQueryCreator.createLinearFunctionParameters(
                BigDecimal.valueOf(55), BigDecimal.valueOf(15),
                BigDecimal.valueOf(60), BigDecimal.valueOf(10));
        assertThat(result.m.doubleValue()).isEqualTo(-1);
        assertThat(result.b.doubleValue()).isEqualTo(70);

        result = numberUnitQueryCreator.createLinearFunctionParameters(
                BigDecimal.valueOf(55.5), BigDecimal.valueOf(15.3),
                BigDecimal.valueOf(60.4), BigDecimal.valueOf(10.3));
        assertThat(result.m.doubleValue()).isEqualTo(-1.02);
        assertThat(result.b.doubleValue()).isEqualTo(71.91);

        result = numberUnitQueryCreator.createLinearFunctionParameters(
                BigDecimal.valueOf(55), BigDecimal.valueOf(15),
                BigDecimal.valueOf(55), BigDecimal.valueOf(15));
        assertThat(result.m.doubleValue()).isEqualTo(0);
        assertThat(result.b.doubleValue()).isEqualTo(15);
    }

    @Test
    public void testCalculatePercentageChange() {
        BigDecimal result;
        result = numberUnitQueryCreator.calculatePercentageChange(new BigDecimal(200), new BigDecimal(20));
        assertThat(result.doubleValue()).isEqualTo(40);

        result = numberUnitQueryCreator.calculatePercentageChange(new BigDecimal(220.2), new BigDecimal(20));
        assertThat(result.doubleValue()).isEqualTo(44.04);
    }
}
