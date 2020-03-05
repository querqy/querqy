package querqy.solr.contrib;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.contrib.NumberUnitRewriter;
import querqy.rewrite.contrib.numberunit.model.FieldDefinition;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.UnitDefinition;
import querqy.solr.FactoryAdapter;
import querqy.solr.contrib.utils.NamedListWrapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NumberUnitRewriterFactory implements FactoryAdapter<RewriterFactory> {

    private static final String EXCEPTION_MESSAGE = "NumberUnitRewriter not properly configured. " +
            "At least one unit and one field need to be properly defined, e. g. \n" +
            "<lst name=\"rewriter\">" + "\n" +
            "  <str name=\"class\">" + NumberUnitRewriterFactory.class.getName() + "</str>" + "\n" +
            "  <lst name=\"numberUnitDefinitions\">" + "\n" +
            "    <lst name=\"numberUnitDefinition\">" + "\n" +
            "      <lst name=\"units\">" + "\n" +
            "        <lst name=\"unit\">" + "\n" +
            "          <str name=\"term\">cm</str>" + "\n" +
            "        </lst>" + "\n" +
            "      </lst>" + "\n" +
            "      <str name=\"fields\">size</str>" + "\n" +
            "    </lst>" + "\n" +
            "  </lst>" + "\n" +
            "</lst>";

    @Override
    public RewriterFactory createFactory(String id, NamedList<?> args, ResourceLoader resourceLoader) throws IOException {

        NamedListWrapper namedListWrapper = NamedListWrapper.create(args, EXCEPTION_MESSAGE);
        int scale = namedListWrapper.getOrDefaultInteger("globalFloatingPointNumbers", 5);

        List<NumberUnitDefinition> numberUnitDefinitions = namedListWrapper
                .getNamedListOrElseThrow("numberUnitDefinitions")
                .getListOfNamedListsAssertNotEmpty("numberUnitDefinition")
                .stream()
                .map(rawDefinition ->
                        NumberUnitDefinition.builder()
                                .addUnitDefinitions(rawDefinition
                                        .getNamedListOrElseThrow("units")
                                        .getListOfNamedListsAssertNotEmpty("unit")
                                        .stream()
                                        .map(rawUnit -> new UnitDefinition(
                                                rawUnit.getStringOrElseThrow("term"),
                                                rawUnit.getOrDefaultBigDecimal("multiplier", 1)))
                                        .collect(Collectors.toList()))
                                .addFields(
                                        rawDefinition
                                                .getNamedListOrElseThrow("fields")
                                                .getListOfNamedListsAssertNotEmpty("field")
                                                .stream()
                                                .map(field -> new FieldDefinition(
                                                        field.getStringOrElseThrow("fieldName"),
                                                        field.getOrDefaultInteger("floatingPointNumbers", 0)))
                                                .collect(Collectors.toList()))
                                .setBoostMaxScore(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostMaxScore", 200))
                                .setBoostScoreUp(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostScoreUp", 100))
                                .setBoostScoreDown(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostScoreDown", 100))
                                .setBoostAddScoreExactMatch(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostAddScoreExactMatch", 100))
                                .setBoostPercentageUp(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostPercentageUp", 20))
                                .setBoostPercentageDown(rawDefinition.getOrDefaultBigDecimalAssertNotNegative("boostPercentageDown", 20))
                                .setFilterPercentageUp(rawDefinition.getOrDefaultBigDecimal("filterPercentageUp", 20))
                                .setFilterPercentageDown(rawDefinition.getOrDefaultBigDecimal("filterPercentageDown", 20))
                                .build())
                .collect(Collectors.toList());

        numberUnitDefinitions.stream()
                .filter(this::numberUnitDefinitionHasDuplicateUnitDefinition)
                .findFirst()
                .ifPresent(numberUnitDefinition -> {
                    throw new IllegalArgumentException("Units must only defined once per NumberUnitDefinition");});

        return new querqy.rewrite.contrib.NumberUnitRewriterFactory(id, numberUnitDefinitions, scale);
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

    @Override
    public Class<?> getCreatedClass() {
        return NumberUnitRewriter.class;
    }
}
