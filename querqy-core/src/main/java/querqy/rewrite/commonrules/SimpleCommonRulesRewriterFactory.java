package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QuerqyTemplateEngine;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.TemplateParseException;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.TrieMapRulesCollectionBuilder;
import querqy.rewrite.commonrules.select.SelectionStrategy;
import querqy.rewrite.commonrules.select.RuleSelectionParams;
import querqy.rewrite.commonrules.select.SelectionStrategyFactory;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.RulesParser;
import querqy.rewrite.rules.factory.RulesParserFactory;
import querqy.rewrite.rules.factory.config.RuleParserConfig;
import querqy.rewrite.rules.factory.config.RulesParserConfig;
import querqy.rewrite.rules.factory.config.TextParserConfig;
import querqy.rewrite.rules.instruction.InstructionType;

import static querqy.rewrite.rules.instruction.InstructionType.DECORATE;
import static querqy.rewrite.rules.instruction.InstructionType.DELETE;
import static querqy.rewrite.rules.instruction.InstructionType.DOWN;
import static querqy.rewrite.rules.instruction.InstructionType.FILTER;
import static querqy.rewrite.rules.instruction.InstructionType.SYNONYM;
import static querqy.rewrite.rules.instruction.InstructionType.UP;

/**
 * @author René Kriegler, @renekrie
 */
public class SimpleCommonRulesRewriterFactory extends RewriterFactory {

    private static final Set<InstructionType> ALLOWED_TYPES = Stream.of(
            SYNONYM, UP, DOWN, FILTER, DELETE, DECORATE
    ).collect(Collectors.toSet());

    private final RulesCollection rules;
    private final Map<String, SelectionStrategyFactory> selectionStrategyFactories;
    private final String strategyParam;
    private final SelectionStrategyFactory defaultSelectionStrategyFactory;
    private final boolean buildTermCache;


    /**
     *
     * @param rewriterId The id of this rewriter
     * @param reader The reader to access the rewriter configuration
     * @param allowBooleanInput Iff true, rule input can have boolean expressions
     * @param generateMultiplicativeBoosts Iff true, create {@link querqy.rewrite.commonrules.model.BoostInstruction}s
     *                                     with multiplicative boosts
     * @param querqyParserFactory A parser for the right-hand side of rules
     * @param ignoreCase Iff true, rule input matching is case insensitive.
     * @param selectionStrategyFactories A mapping between names of rule selection strategies and their factories.
     * @param defaultSelectionStrategyFactory The default {@link SelectionStrategyFactory} to be used if no strategy is
     *                                       specified as a request parameter
     * @param buildTermCache If true, build the term cache for terms from the rhs of rules
     * @throws IOException if rules cannot be read or parsed
     */
    public SimpleCommonRulesRewriterFactory(final String rewriterId,
                                            final Reader reader,
                                            final boolean allowBooleanInput,
                                            final boolean generateMultiplicativeBoosts,
                                            final QuerqyParserFactory querqyParserFactory,
                                            final boolean ignoreCase,
                                            final Map<String, SelectionStrategyFactory> selectionStrategyFactories,
                                            final SelectionStrategyFactory defaultSelectionStrategyFactory,
                                            final boolean buildTermCache)
            throws IOException {

        super(rewriterId);

        this.strategyParam = RuleSelectionParams.getStrategyParamName(rewriterId);

        this.selectionStrategyFactories = new HashMap<>(selectionStrategyFactories);

        this.defaultSelectionStrategyFactory = Objects.requireNonNull(defaultSelectionStrategyFactory);

        this.buildTermCache = buildTermCache;

        // TODO: using List<String> to process lines instead of Reader should be better:
        //  (1) Lines can be reused across different processing stages (should reduce resource consumption)
        //  (2) Allows removing various try & catch blocks
        try {
            final QuerqyTemplateEngine querqyTemplateEngine = new QuerqyTemplateEngine(reader);

            final RulesParserConfig config = RulesParserConfig.builder()
                    .textParserConfig(TextParserConfig.builder()
                            .rulesContentReader(querqyTemplateEngine.renderedRules.reader)
                            .isMultiLineRulesConfig(true)
                            .lineNumberMappings(querqyTemplateEngine.renderedRules.lineNumberMapping)
                            .build())
                    .ruleParserConfig(RuleParserConfig.builder()
                            .isAllowedToParseBooleanInput(allowBooleanInput)
                            .generateMultiplicativeBoosts(generateMultiplicativeBoosts)
                            .querqyParserFactory(querqyParserFactory)
                            .allowedInstructionTypes(ALLOWED_TYPES)
                            .build())
                    .rulesCollectionBuilder(new TrieMapRulesCollectionBuilder(ignoreCase))
                    .build();

            final RulesParser rulesParser = RulesParserFactory.textParser(config);
            rules = rulesParser.parse();

            // should be closed already in RulesParser - passing Readers as arguments should be avoided
            // and refactored as suggested above
            querqyTemplateEngine.renderedRules.reader.close();

        } catch (final RuleParseException | TemplateParseException e) {
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
    public Set<Term> getCacheableGenerableTerms() {
        if (buildTermCache) {
            return rules.getGenerableTerms();
        }

        return Collections.emptySet();
    }

    RulesCollection getRules() {
        return rules;
    }

}
