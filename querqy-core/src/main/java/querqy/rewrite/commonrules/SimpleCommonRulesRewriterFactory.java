package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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

    private final RulesCollection rules;
    private final Map<String, SelectionStrategyFactory> selectionStrategyFactories;
    private final String strategyParam;
    private final SelectionStrategyFactory defaultSelectionStrategyFactory;


    /**
     *
     * @param rewriterId
     * @param reader
     * @param querqyParserFactory
     * @param ignoreCase
     * @param selectionStrategyFactories
     * @param defaultSelectionStrategyFactory The default {@link SelectionStrategyFactory} to be used if no strategy is
     *                                       specified as a request parameter
     * @throws IOException
     */
    public SimpleCommonRulesRewriterFactory(final String rewriterId,
                                            final Reader reader, final QuerqyParserFactory querqyParserFactory,
                                            final boolean ignoreCase,
                                            final Map<String, SelectionStrategyFactory> selectionStrategyFactories,
                                            final SelectionStrategyFactory defaultSelectionStrategyFactory)
            throws IOException {

        super(rewriterId);

        this.strategyParam = RuleSelectionParams.getStrategyParamName(rewriterId);

        this.selectionStrategyFactories = new HashMap<>(selectionStrategyFactories);

        this.defaultSelectionStrategyFactory = Objects.requireNonNull(defaultSelectionStrategyFactory);

        try {
            rules = new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase).parse();
        } catch (final RuleParseException e) {
            throw new IOException(e);
        } finally {
            try {
                reader.close();
            } catch (final IOException e) {
                // TODO: log
            }
        }
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final SelectionStrategy selectionStrategy = searchEngineRequestAdapter
                .getRequestParam(strategyParam)
                .map(name -> {
                    final SelectionStrategyFactory factory = selectionStrategyFactories.get(name);
                    if (factory == null) {
                        throw new IllegalArgumentException("No selection strategy for name " + name);
                    }
                    return factory;
                }).orElse(defaultSelectionStrategyFactory) // strategy not specified in params
                .createSelectionStrategy(getRewriterId(), searchEngineRequestAdapter);

        return new CommonRulesRewriter(rules, selectionStrategy);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        return rules.getGenerableTerms();
    }

    RulesCollection getRules() {
        return rules;
    }

}
