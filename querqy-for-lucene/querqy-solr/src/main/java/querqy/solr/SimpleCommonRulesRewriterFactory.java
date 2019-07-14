/**
 *
 */
package querqy.solr;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;

import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.ExpressionCriteriaSelectionStrategyFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.SelectionStrategyFactory;

/**
 * @author René Kriegler, @renekrie
 */
public class SimpleCommonRulesRewriterFactory implements FactoryAdapter<RewriterFactory> {

    private static final SelectionStrategyFactory DEFAULT_SELECTION_STRATEGY_FACTORY =
            new ExpressionCriteriaSelectionStrategyFactory();

    /*
     * (non-Javadoc)
     *
     * @see
     * querqy.solr.FactoryAdapter#createRewriterFactory(org.apache.solr
     * .common.util.NamedList, org.apache.lucene.analysis.util.ResourceLoader)
     */
    @Override
    public RewriterFactory createFactory(final String id, final NamedList<?> args,
                                         final ResourceLoader resourceLoader) throws IOException {

        final String rulesResourceName = (String) args.get("rules");
        if (rulesResourceName == null) {
            throw new IllegalArgumentException("Property 'rules' not configured");
        }

        final Map<String, SelectionStrategyFactory> selectionStrategyFactories = new HashMap<>();

        final NamedList<?> selectionStrategyConfiguration = (NamedList<?>) args.get("rules.selectionStrategy");

        if (selectionStrategyConfiguration != null) {

            @SuppressWarnings("unchecked")
            final List<NamedList<?>> strategyConfigs = (List<NamedList<?>>) selectionStrategyConfiguration
                    .getAll("strategy");

            if (strategyConfigs != null) {
                for (NamedList<?> config : strategyConfigs) {
                    @SuppressWarnings("unchecked")
                    final FactoryAdapter<SelectionStrategyFactory> factory = resourceLoader
                            .newInstance((String) config.get("class"), FactoryAdapter.class);
                    final String strategyId = (String) config.get("id");
                    if (selectionStrategyFactories.put(strategyId,
                            factory.createFactory(strategyId, config, resourceLoader)) != null) {
                        throw new IOException("Duplicate id in rules.selectionStrategy: " + id);
                    }
                }
            }
        }


        final Boolean ignoreCase = args.getBooleanArg("ignoreCase");

        // querqy parser for queries that are part of the instructions in the
        // rules
        String rulesQuerqyParser = (String) args.get("querqyParser");
        QuerqyParserFactory querqyParser = null;
        if (rulesQuerqyParser != null) {
            rulesQuerqyParser = rulesQuerqyParser.trim();
            if (rulesQuerqyParser.length() > 0) {
                querqyParser = resourceLoader.newInstance(rulesQuerqyParser, QuerqyParserFactory.class);
            }
        }

        if (querqyParser == null) {
            querqyParser = new WhiteSpaceQuerqyParserFactory();
        }

        return new querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory(id,
                new InputStreamReader(resourceLoader.openResource(rulesResourceName), "UTF-8"), querqyParser,
                ignoreCase == null || ignoreCase, selectionStrategyFactories, DEFAULT_SELECTION_STRATEGY_FACTORY);
    }

    @Override
    public Class<?> getCreatedClass() {
        return querqy.rewrite.commonrules.CommonRulesRewriter.class;
    }
}
