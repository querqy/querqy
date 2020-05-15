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
import querqy.solr.contrib.numberunit.NumberUnitConfigObject.NumberUnitDefinitionObject;
import querqy.solr.contrib.numberunit.NumberUnitQueryCreatorSolr;

import java.io.IOException;
import java.math.BigDecimal;
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

    private static final int DEFAULT_UNIT_MULTIPLIER = 1;

    private static final int DEFAULT_SCALE_FOR_LINEAR_FUNCTIONS = 5;
    private static final int DEFAULT_FIELD_SCALE = 0;

    private static final float DEFAULT_BOOST_MAX_SCORE_FOR_EXACT_MATCH = 200;
    private static final float DEFAULT_BOOST_MIN_SCORE_AT_UPPER_BOUNDARY = 100;
    private static final float DEFAULT_BOOST_MIN_SCORE_AT_LOWER_BOUNDARY = 100;
    private static final float DEFAULT_BOOST_ADDITIONAL_SCORE_FOR_EXACT_MATCH = 100;

    private static final float DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY = 20;
    private static final float DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY = 20;
    private static final float DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY_EXACT_MATCH = 0;
    private static final float DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY_EXACT_MATCH = 0;

    private static final float DEFAULT_FILTER_PERCENTAGE_LOWER_BOUNDARY = 20;
    private static final float DEFAULT_FILTER_PERCENTAGE_UPPER_BOUNDARY = 20;

    private static final String KEY_CONFIG_FILE = "config";

    private static final ObjectMapper JSON_DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    @Override
    public RewriterFactory createFactory(final String id, final NamedList<?> args, final ResourceLoader resourceLoader)
            throws IOException {

        final Object obj = args.get(KEY_CONFIG_FILE);
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException("Property 'config' not or not properly configured");
        }

        final String rulesResourceName = (String) obj;

        // resource InputStream will be closed by Jackson Json Parser
        final NumberUnitConfigObject numberUnitConfigObject = JSON_DEFAULT_OBJECT_MAPPER.readValue(
                resourceLoader.openResource(rulesResourceName), NumberUnitConfigObject.class);

        final int scale = getOrDefaultInt(numberUnitConfigObject::getScaleForLinearFunctions,
                DEFAULT_SCALE_FOR_LINEAR_FUNCTIONS);
        final List<NumberUnitDefinition> numberUnitDefinitions = parseConfig(numberUnitConfigObject);

        numberUnitDefinitions.stream()
                .filter(this::numberUnitDefinitionHasDuplicateUnitDefinition)
                .findFirst()
                .ifPresent(numberUnitDefinition -> {
                    throw new IllegalArgumentException("Units must only defined once per NumberUnitDefinition");});

        return new querqy.rewrite.contrib.NumberUnitRewriterFactory(id, numberUnitDefinitions,
                new NumberUnitQueryCreatorSolr(scale));
    }

    protected boolean numberUnitDefinitionHasDuplicateUnitDefinition(final NumberUnitDefinition numberUnitDefinition) {
        final Set<String> observedUnits = new HashSet<>();
        for (final UnitDefinition unitDefinition : numberUnitDefinition.unitDefinitions) {
            if (!observedUnits.add(unitDefinition.term)) {
                return true;
            }
        }
        return false;
    }

    protected List<NumberUnitDefinition> parseConfig(final NumberUnitConfigObject numberUnitConfigObject) {

        final List<NumberUnitDefinitionObject> numberUnitDefinitionObjects =
                numberUnitConfigObject.getNumberUnitDefinitions();

        if (numberUnitDefinitionObjects == null || numberUnitDefinitionObjects.isEmpty()) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE);
        }

        return numberUnitDefinitionObjects.stream().map(this::parseNumberUnitDefinition).collect(Collectors.toList());

    }

    private NumberUnitDefinition parseNumberUnitDefinition(final NumberUnitDefinitionObject defObj) {

        final NumberUnitDefinition.Builder builder = NumberUnitDefinition.builder()
                .addUnits(this.parseUnitDefinitions(defObj))
                .addFields(this.parseFieldDefinitions(defObj));

        final NumberUnitConfigObject.BoostObject boost = defObj.getBoost() != null
                ? defObj.getBoost()
                : new NumberUnitConfigObject.BoostObject();

        builder
                .setMaxScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getMaxScoreForExactMatch, DEFAULT_BOOST_MAX_SCORE_FOR_EXACT_MATCH))
                .setMinScoreAtUpperBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getMinScoreAtUpperBoundary, DEFAULT_BOOST_MIN_SCORE_AT_UPPER_BOUNDARY))
                .setMinScoreAtLowerBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getMinScoreAtLowerBoundary, DEFAULT_BOOST_MIN_SCORE_AT_LOWER_BOUNDARY))
                .setAdditionalScoreForExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getAdditionalScoreForExactMatch, DEFAULT_BOOST_ADDITIONAL_SCORE_FOR_EXACT_MATCH))
                .setBoostPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageUpperBoundary, DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY))
                .setBoostPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageLowerBoundary, DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY))
                .setBoostPercentageUpperBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageUpperBoundaryExactMatch, DEFAULT_BOOST_PERCENTAGE_UPPER_BOUNDARY_EXACT_MATCH))
                .setBoostPercentageLowerBoundaryExactMatch(getOrDefaultBigDecimalForFloat(
                        boost::getPercentageLowerBoundaryExactMatch, DEFAULT_BOOST_PERCENTAGE_LOWER_BOUNDARY_EXACT_MATCH));

        final NumberUnitConfigObject.FilterObject filter = defObj.getFilter() != null
                ? defObj.getFilter()
                : new NumberUnitConfigObject.FilterObject();

        builder
                .setFilterPercentageUpperBoundary(getOrDefaultBigDecimalForFloat(
                        filter::getPercentageUpperBoundary, DEFAULT_FILTER_PERCENTAGE_UPPER_BOUNDARY))
                .setFilterPercentageLowerBoundary(getOrDefaultBigDecimalForFloat(
                        filter::getPercentageLowerBoundary, DEFAULT_FILTER_PERCENTAGE_LOWER_BOUNDARY));

        return builder.build();
    }


    private List<UnitDefinition> parseUnitDefinitions(final NumberUnitDefinitionObject numberUnitDefinitionObject) {
        final List<NumberUnitConfigObject.UnitObject> unitObjects = numberUnitDefinitionObject.getUnits();
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
                        getOrDefaultBigDecimalForFloat(unitObject::getMultiplier, DEFAULT_UNIT_MULTIPLIER)))
                .collect(Collectors.toList());
    }

    private List<FieldDefinition> parseFieldDefinitions(final NumberUnitDefinitionObject numberUnitDefinitionObject) {
        final List<NumberUnitConfigObject.FieldObject> fieldObjects = numberUnitDefinitionObject.getFields();
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
                        getOrDefaultInt(fieldObject::getScale, DEFAULT_FIELD_SCALE)))
                .collect(Collectors.toList());
    }

    private BigDecimal getOrDefaultBigDecimalForFloat(final Supplier<Float> supplier, final float defaultValue) {
        final Float value = supplier.get();
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.valueOf(defaultValue);
    }

    private int getOrDefaultInt(final Supplier<Integer> supplier, final int defaultValue) {
        final Integer value = supplier.get();
        return value != null ? value : defaultValue;
    }

    @Override
    public Class<?> getCreatedClass() {
        return NumberUnitRewriter.class;
    }
}
