package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.ComparableCharSequenceWrapper;
import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NumberUnitRewriterFactory extends RewriterFactory {

    private final Map<ComparableCharSequence, List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    public NumberUnitRewriterFactory(final String id,
                                     final List<NumberUnitDefinition> numberUnitDefinitions,
                                     final NumberUnitQueryCreator numberUnitQueryCreator) {
        super(id);
        this.numberUnitMap = createNumberUnitMap(numberUnitDefinitions);
        this.numberUnitQueryCreator = numberUnitQueryCreator;
    }

    private Map<ComparableCharSequence, List<PerUnitNumberUnitDefinition>> createNumberUnitMap(
            List<NumberUnitDefinition> numberUnitDefinitions) {

        final Map<ComparableCharSequence, List<PerUnitNumberUnitDefinition>> map = new HashMap<>();

        numberUnitDefinitions.forEach(
                numberUnitDefinition -> numberUnitDefinition.unitDefinitions.forEach(
                        unitDefinition -> map.computeIfAbsent(
                                new ComparableCharSequenceWrapper(unitDefinition.term),
                                key -> new ArrayList<>()).add(
                                        new PerUnitNumberUnitDefinition(numberUnitDefinition, unitDefinition.multiplier))));

        return map;
    }

    @Override
    public QueryRewriter createRewriter(ExpandedQuery input, SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
