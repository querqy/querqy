package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.SelectionStrategy;

/**
 * @author René Kriegler, @renekrie
 */
public class SimpleCommonRulesRewriterFactory extends RewriterFactory {

    public static final String PARAM_SELECTION_STRATEGY = "rules.criteria.strategy";

    final RulesCollection rules;
    final Map<String, SelectionStrategyFactory> selectionStrategyFactories;


    /**
     *
     * @param rewriterId
     * @param reader
     * @param querqyParserFactory
     * @param ignoreCase
     * @param selectionStrategyFactories
     * @throws IOException
     */
    public SimpleCommonRulesRewriterFactory(final String rewriterId,
                                            final Reader reader, final QuerqyParserFactory querqyParserFactory,
                                            final boolean ignoreCase,
                                            final Map<String, SelectionStrategyFactory> selectionStrategyFactories)
            throws IOException {
        super(rewriterId);
        this.selectionStrategyFactories = new HashMap<>(selectionStrategyFactories);

        try {
            rules = new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase).parse();
        } catch (final RuleParseException e) {
            throw new IOException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO: log
            }
        }
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final SelectionStrategy selectionStrategy = searchEngineRequestAdapter
                .getRequestParam(PARAM_SELECTION_STRATEGY)
                .map(name -> {
                    final SelectionStrategyFactory factory = selectionStrategyFactories.get(name);
                    if (factory == null) {
                        throw new IllegalArgumentException("No selection strategy for name " + name);
                    }
                    return factory.createSelectionStrategy(searchEngineRequestAdapter);
                })
                .orElse(SelectionStrategyFactory.DEFAULT_SELECTION_STRATEGY); // strategy not specified in params

        return new CommonRulesRewriter(rules, selectionStrategy);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        // REVISIT: return Iterator? Limit number of results?
        final Set<Term> result = new HashSet<Term>();
        for (final Instruction instruction : rules.getInstructions()) {
            result.addAll(instruction.getGenerableTerms());
        }
        return result;
    }

}
