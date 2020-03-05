package querqy.rewrite.contrib.numberunit;

import org.junit.Test;
import querqy.model.RawQuery;
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
    public void testCreateBoostQuery() {
        RawQuery rawBoostQuery;

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 2)),
                                20, 20, 20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40.00 u=50.00 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!term f=screen_size v=50.00}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Collections.singletonList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("screen_size", 0)),
                                20, 20, 20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}if(query({!frange l=40 u=50 incu='false' v='screen_size'})," +
                        "rint(linear(screen_size,1.000,-30.000))," +
                        "if(query({!term f=screen_size v=50}),30," +
                        "if(query({!frange l=50 u=60 incl='false' v='screen_size'})," +
                        "rint(linear(screen_size,-1.000,70.000)),0)))");

        rawBoostQuery = numberUnitQueryCreator.createRawBoostQuery(
                BigDecimal.valueOf(50),
                Arrays.asList(
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Arrays.asList(new FieldDefinition("f1", 2), new FieldDefinition("f2", 2)),
                                        20, 20, 20, 10, 1.0),
                        createPerUnitNumberUnitDefinitionForBoosts(
                                Collections.singletonList(new FieldDefinition("f3", 2)),
                                20, 20, 20, 10, 1.0)));

        assertThat(rawBoostQuery.getQueryString()).isEqualTo(
                "{!func}max(" +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f1'})," +
                        "rint(linear(f1,1.000,-30.000))," +
                        "if(query({!term f=f1 v=50.00}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f1'})," +
                        "rint(linear(f1,-1.000,70.000)),0)))," +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f2'})," +
                        "rint(linear(f2,1.000,-30.000))," +
                        "if(query({!term f=f2 v=50.00}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f2'})," +
                        "rint(linear(f2,-1.000,70.000)),0)))," +
                        "if(query({!frange l=40.00 u=50.00 incu='false' v='f3'})," +
                        "rint(linear(f3,1.000,-30.000))," +
                        "if(query({!term f=f3 v=50.00}),30," +
                        "if(query({!frange l=50.00 u=60.00 incl='false' v='f3'})," +
                        "rint(linear(f3,-1.000,70.000)),0))))");
    }

    @Test
    public void testCreateFilterQuery() {
        RawQuery filterQuery;

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
                        .addUnitDefinitions(Collections.emptyList())
                        .addFields(fields)
                        .setFilterPercentageUp(BigDecimal.valueOf(percentageUp))
                        .setFilterPercentageDown(BigDecimal.valueOf(percentageDown))
                        .setBoostMaxScore(BigDecimal.ONE)
                        .setBoostAddScoreExactMatch(BigDecimal.ONE)
                        .setBoostScoreUp(BigDecimal.ONE)
                        .setBoostScoreDown(BigDecimal.ONE)
                        .setBoostPercentageUp(BigDecimal.ONE)
                        .setBoostPercentageDown(BigDecimal.ONE)
                        .build(),
                BigDecimal.valueOf(multiplier));
    }

    private PerUnitNumberUnitDefinition createPerUnitNumberUnitDefinitionForBoosts(
            List<FieldDefinition> fields, double percentageUp, double percentageDown,
            double maxScore, double minScore, double multiplier) {
        return new PerUnitNumberUnitDefinition(
                NumberUnitDefinition.builder()
                        .addUnitDefinitions(Collections.emptyList())
                        .addFields(fields)
                        .setBoostMaxScore(BigDecimal.valueOf(maxScore))
                        .setBoostAddScoreExactMatch(BigDecimal.valueOf(10))
                        .setBoostScoreUp(BigDecimal.valueOf(minScore))
                        .setBoostScoreDown(BigDecimal.valueOf(minScore))
                        .setBoostPercentageUp(BigDecimal.valueOf(percentageUp))
                        .setBoostPercentageDown(BigDecimal.valueOf(percentageDown))
                        .setFilterPercentageUp(BigDecimal.ONE)
                        .setFilterPercentageDown(BigDecimal.ONE)
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
