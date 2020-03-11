package querqy.solr.contrib;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.contrib.NumberUnitRewriter;
import querqy.rewrite.contrib.numberunit.model.FieldDefinition;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.UnitDefinition;
import querqy.solr.FactoryAdapter;
import querqy.solr.contrib.numberunit.NumberUnitConfigObject;
import querqy.solr.contrib.numberunit.NumberUnitQueryCreatorSolr;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NumberUnitRewriterFactory implements FactoryAdapter<RewriterFactory> {

    private static final String EXCEPTION_MESSAGE = "NumberUnitRewriter not properly configured. " +
            "At least one unit and one field need to be properly defined, e. g. \n" +
            "{\n" +
            "  \"numberUnitDefinitions\": [\n" +
            "    {\n" +
            "      \"units\": [ { \"term\": \"cm\" } ],\n" +
            "      \"fields\": [ { \"fieldName\": \"weight\" } ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static int defaultUnitMultiplier = 1;

    private static int defaultFloatingPointNumbersForLinearFunctions = 5;
    private static int defaultFieldFloatingPointNumbers = 0;

    private static float defaultBoostMaxScoreForExactMatch = 200;
    private static float defaultBoostMinScoreAtUpperBoundary = 100;
    private static float defaultBoostMinScoreAtLowerBoundary = 100;
    private static float defaultBoostAdditionalScoreForExactMatch = 100;

    private static float defaultBoostPercentageUpperBoundary = 20;
    private static float defaultBoostPercentageLowerBoundary = 20;
    private static float defaultBoostPercentageUpperBoundaryExactMatch = 5;
    private static float defaultBoostPercentageLowerBoundaryExactMatch = 5;

    private static float defaultFilterPercentageLowerBoundary = 20;
    private static float defaultFilterPercentageUpperBoundary = 20;

    private static String keyForConfigFile = "config";

    @Override
    public RewriterFactory createFactory(String id, NamedList<?> args, ResourceLoader resourceLoader) throws IOException {
        final Object obj = args.get(keyForConfigFile);
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException("Property 'config' not or not properly configured");
        }

        final String rulesResourceName = (String) obj;
        final InputStream is = resourceLoader.openResource(rulesResourceName);

        final ObjectMapper objectMapper = new ObjectMapper();
        final NumberUnitConfigObject numberUnitConfigObject = objectMapper.readValue(is, NumberUnitConfigObject.class);

        final int scale = getOrDefaultInt(numberUnitConfigObject::getFloatingPointNumbersForLinearFunctions, defaultFloatingPointNumbersForLinearFunctions);
        final List<NumberUnitDefinition> numberUnitDefinitions = parseConfig(numberUnitConfigObject);

        numberUnitDefinitions.stream()
                .filter(this::numberUnitDefinitionHasDuplicateUnitDefinition)
                .findFirst()
                .ifPresent(numberUnitDefinition -> {
                    throw new IllegalArgumentException("Units must only defined once per NumberUnitDefinition");});

        return new querqy.rewrite.contrib.NumberUnitRewriterFactory(id, numberUnitDefinitions, new NumberUnitQueryCreatorSolr(scale));
    }

    protected boolean numberUnitDefinitionHasDuplicateUnitDefinition(NumberUnitDefinition numberUnitDefinition) {
        Set<String> observedUnits = new HashSet<>();
        for (UnitDefinition unitDefinition : numberUnitDefinition.unitDefinitions) {
            if (observedUnits.contains(unitDefinition.term)) {
                return true;
            }
            observedUnits.add(unitDefinition.term);
        }
        return false;
    }

    protected List<NumberUnitDefinition> parseConfig(final NumberUnitConfigObject numberUnitConfigObject) {
        List<NumberUnitConfigObject.NumberUnitDefinitionObject> numberUnitDefinitionObjects = numberUnitConfigObject.getNumberUnitDefinitions();
        if (numberUnitDefinitionObjects == null || numberUnitDefinitionObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        List<NumberUnitDefinition> numberUnitDefinitions = new ArrayList<>();
        for (NumberUnitConfigObject.NumberUnitDefinitionObject defObj : numberUnitDefinitionObjects) {
            NumberUnitDefinition.Builder builder = NumberUnitDefinition.builder()
                    .addUnits(this.parseUnitDefinitions(defObj))
                    .addFields(this.parseFieldDefinitions(defObj));

            final NumberUnitConfigObject.BoostObject boost = defObj.getBoost() != null ? defObj.getBoost() : new NumberUnitConfigObject.BoostObject();

            builder
                    .setMaxScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                            boost::getMaxScoreForExactMatch, defaultBoostMaxScoreForExactMatch))
                    .setMinScoreAtUpperBoundary(getOrDefaultBigDecimalForFloat(
                            boost::getMinScoreAtUpperBoundary, defaultBoostMinScoreAtUpperBoundary))
                    .setMinScoreAtLowerBoundary(getOrDefaultBigDecimalForFloat(
                            boost::getMinScoreAtLowerBoundary, defaultBoostMinScoreAtLowerBoundary))
                    .setAdditionalScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                            boost::getAdditionalScoreForExactMatch, defaultBoostAdditionalScoreForExactMatch))
                    .setBoostPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                            boost::getPercentageUpperBoundary, defaultBoostPercentageUpperBoundary))
                    .setBoostPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                            boost::getPercentageLowerBoundary, defaultBoostPercentageLowerBoundary))
                    .setBoostPercentageUpperBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                            boost::getPercentageUpperBoundaryExactMatch, defaultBoostPercentageUpperBoundaryExactMatch))
                    .setBoostPercentageLowerBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                            boost::getPercentageLowerBoundaryExactMatch, defaultBoostPercentageLowerBoundaryExactMatch));

            final NumberUnitConfigObject.FilterObject filter = defObj.getFilter() != null ? defObj.getFilter() : new NumberUnitConfigObject.FilterObject();

            builder
                    .setFilterPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                            filter::getPercentageUpperBoundary, defaultFilterPercentageUpperBoundary))
                    .setFilterPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                            filter::getPercentageLowerBoundary, defaultFilterPercentageLowerBoundary));

            numberUnitDefinitions.add(builder.build());
        }
        return numberUnitDefinitions;

    }


    private List<UnitDefinition> parseUnitDefinitions(NumberUnitConfigObject.NumberUnitDefinitionObject numberUnitDefinitionObject) {
        List<NumberUnitConfigObject.UnitObject> unitObjects = numberUnitDefinitionObject.getUnits();
        if (unitObjects == null || unitObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return unitObjects.stream()
                .peek(unitObject -> {
                    if (unitObject.getTerm() == null || StringUtils.isBlank(unitObject.getTerm())) {
                        throw new IllegalArgumentException("Unit definition requires a term to be defined");
                    }})
                .map(unitObject -> new UnitDefinition(
                        unitObject.getTerm(),
                        getOrDefaultBigDecimalForFloat(unitObject::getMultiplier, defaultUnitMultiplier)))
                .collect(Collectors.toList());
    }

    private List<FieldDefinition> parseFieldDefinitions(NumberUnitConfigObject.NumberUnitDefinitionObject numberUnitDefinitionObject) {
        List<NumberUnitConfigObject.FieldObject> fieldObjects = numberUnitDefinitionObject.getFields();
        if (fieldObjects == null || fieldObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return fieldObjects.stream()
                .peek(fieldObject -> {
                    if (fieldObject.getFieldName() == null || StringUtils.isBlank(fieldObject.getFieldName())) {
                        throw new IllegalArgumentException("Unit definition requires a term to be defined");
                    }})
                .map(fieldObject -> new FieldDefinition(
                        fieldObject.getFieldName(),
                        getOrDefaultInt(fieldObject::getFloatingPointNumbers, defaultFieldFloatingPointNumbers)))
                .collect(Collectors.toList());
    }

    private BigDecimal getOrDefaultBigDecimalForFloat(Supplier<Float> supplier, float defaultValue) {
        Float value = supplier.get();
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.valueOf(defaultValue);
    }

    private int getOrDefaultInt(Supplier<Integer> supplier, int defaultValue) {
        Integer value = supplier.get();
        return value != null ? value : defaultValue;
    }

    @Override
    public Class<?> getCreatedClass() {
        return NumberUnitRewriter.class;
    }
}
