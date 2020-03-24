package querqy.rewrite.contrib;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.contrib.numberunit.NumberUnitQueryCreator;
import querqy.rewrite.contrib.numberunit.model.NumberUnitDefinition;
import querqy.rewrite.contrib.numberunit.model.PerUnitNumberUnitDefinition;
import querqy.trie.State;
import querqy.trie.TrieMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NumberUnitRewriterFactory extends RewriterFactory {

    private final TrieMap<List<PerUnitNumberUnitDefinition>> numberUnitMap;
    private final NumberUnitQueryCreator numberUnitQueryCreator;

    public NumberUnitRewriterFactory(final String id,
                                     final List<NumberUnitDefinition> numberUnitDefinitions,
                                     final NumberUnitQueryCreator numberUnitQueryCreator) {
        super(id);
        this.numberUnitMap = createNumberUnitMap(numberUnitDefinitions);
        this.numberUnitQueryCreator = numberUnitQueryCreator;
    }

    private TrieMap<List<PerUnitNumberUnitDefinition>> createNumberUnitMap(
            List<NumberUnitDefinition> numberUnitDefinitions) {

        final TrieMap<List<PerUnitNumberUnitDefinition>> map = new TrieMap<>();

        numberUnitDefinitions.forEach(numberUnitDefinition ->
                numberUnitDefinition.unitDefinitions.forEach(unitDefinition -> {

                    final State<List<PerUnitNumberUnitDefinition>> state = map.get(unitDefinition.term)
                            .getStateForCompleteSequence();

                    final PerUnitNumberUnitDefinition def = new PerUnitNumberUnitDefinition(numberUnitDefinition,
                            unitDefinition.multiplier);

                    if (state.isFinal()) {
                        state.value.add(def);

                    } else {
                        final List<PerUnitNumberUnitDefinition> newList = new ArrayList<>();
                        newList.add(def);
                        map.put(unitDefinition.term, newList);
                    }
                }));

        return map;
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return new NumberUnitRewriter(numberUnitMap, numberUnitQueryCreator);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return QueryRewriter.EMPTY_GENERABLE_TERMS;
    }
}
