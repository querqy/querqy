package querqy.solr.contrib;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.solr.contrib.numberunit.NumberUnitConfigObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberUnitRewriterParseConfigTest {

    private static String basePath = "/solr/collection1/conf/contrib/numberunit/";

    @Test
    public void testFullConfig() throws IOException {
        final NumberUnitConfigObject numberUnitConfigObject = createConfigObjectFromFileName("number-unit-full-config.json");
        assertThat(numberUnitConfigObject.getFloatingPointNumbersForLinearFunctions()).isEqualTo(1001);
        assertThat(numberUnitConfigObject.getNumberUnitDefinitions().get(0).getBoost()).isNotNull();
        assertThat(numberUnitConfigObject.getNumberUnitDefinitions().get(0).getFilter()).isNotNull();

        final List<NumberUnitDefinition> numberUnitDefinitions = new NumberUnitRewriterFactory().parseConfig(numberUnitConfigObject);
        assertThat(numberUnitDefinitions).isNotNull();
        assertThat(numberUnitDefinitions).isNotEmpty();

        NumberUnitDefinition numberUnitDefinition = numberUnitDefinitions.get(0);

        assertThat(numberUnitDefinition.unitDefinitions).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions).isNotEmpty();

        assertThat(numberUnitDefinition.unitDefinitions).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions).isNotEmpty();
        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isEqualTo("term");
        assertThat(numberUnitDefinition.unitDefinitions.get(0).multiplier.doubleValue()).isEqualTo(1002);

        assertThat(numberUnitDefinition.fields).isNotNull();
        assertThat(numberUnitDefinition.fields).isNotEmpty();
        assertThat(numberUnitDefinition.fields.get(0).fieldName).isNotNull();
        assertThat(numberUnitDefinition.fields.get(0).fieldName).isEqualTo("fieldName");
        assertThat(numberUnitDefinition.fields.get(0).scale).isEqualTo(1003);

        assertThat(numberUnitDefinition.maxScoreForExactMatch.doubleValue()).isEqualTo(1004);
        assertThat(numberUnitDefinition.minScoreAtUpperBoundary.doubleValue()).isEqualTo(1005);
        assertThat(numberUnitDefinition.minScoreAtLowerBoundary.doubleValue()).isEqualTo(1006);
        assertThat(numberUnitDefinition.additionalScoreForExactMatch.doubleValue()).isEqualTo(1007);

        assertThat(numberUnitDefinition.boostPercentageUpperBoundary.doubleValue()).isEqualTo(1008);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundary.doubleValue()).isEqualTo(1009);
        assertThat(numberUnitDefinition.boostPercentageUpperBoundaryExactMatch.doubleValue()).isEqualTo(1010);
        assertThat(numberUnitDefinition.boostPercentageLowerBoundaryExactMatch.doubleValue()).isEqualTo(1011);

        assertThat(numberUnitDefinition.filterPercentageUpperBoundary.doubleValue()).isEqualTo(1012);
        assertThat(numberUnitDefinition.filterPercentageLowerBoundary.doubleValue()).isEqualTo(1013);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfig() throws IOException {
        final NumberUnitConfigObject numberUnitConfigObject = createConfigObjectFromFileName("number-unit-invalid-config.json");
        new NumberUnitRewriterFactory().parseConfig(numberUnitConfigObject);
    }

    @Test
    public void testMinimalConfig() throws IOException {
        final NumberUnitConfigObject numberUnitConfigObject = createConfigObjectFromFileName("number-unit-minimal-config.json");

        assertThat(numberUnitConfigObject.getNumberUnitDefinitions().get(0).getBoost()).isNotNull();
        assertThat(numberUnitConfigObject.getNumberUnitDefinitions().get(0).getFilter()).isNotNull();

        final List<NumberUnitDefinition> numberUnitDefinitions = new NumberUnitRewriterFactory().parseConfig(numberUnitConfigObject);
        assertThat(numberUnitDefinitions).isNotNull();
        assertThat(numberUnitDefinitions).isNotEmpty();

        NumberUnitDefinition numberUnitDefinition = numberUnitDefinitions.get(0);

        assertThat(numberUnitDefinition.unitDefinitions).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions).isNotEmpty();

        assertThat(numberUnitDefinition.unitDefinitions).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions).isNotEmpty();
        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isNotNull();
        assertThat(numberUnitDefinition.unitDefinitions.get(0).term).isEqualTo("term");
        assertThat(numberUnitDefinition.unitDefinitions.get(0).multiplier).isNotNull();

        assertThat(numberUnitDefinition.fields).isNotNull();
        assertThat(numberUnitDefinition.fields).isNotEmpty();
        assertThat(numberUnitDefinition.fields.get(0).fieldName).isNotNull();
        assertThat(numberUnitDefinition.fields.get(0).fieldName).isEqualTo("fieldName");
        assertThat(numberUnitDefinition.fields.get(0).scale).isNotNull();

        assertThat(numberUnitDefinition.maxScoreForExactMatch.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.minScoreAtUpperBoundary.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.minScoreAtLowerBoundary.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.additionalScoreForExactMatch.doubleValue()).isNotNull();

        assertThat(numberUnitDefinition.boostPercentageUpperBoundary.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.boostPercentageLowerBoundary.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.boostPercentageUpperBoundaryExactMatch.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.boostPercentageLowerBoundaryExactMatch.doubleValue()).isNotNull();

        assertThat(numberUnitDefinition.filterPercentageUpperBoundary.doubleValue()).isNotNull();
        assertThat(numberUnitDefinition.filterPercentageLowerBoundary.doubleValue()).isNotNull();
    }

    private NumberUnitConfigObject createConfigObjectFromFileName(String fileName) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(basePath + fileName);
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputStream, NumberUnitConfigObject.class);
    }

}
