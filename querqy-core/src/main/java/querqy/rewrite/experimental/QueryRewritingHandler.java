package querqy.rewrite.experimental;

import querqy.model.ExpandedQuery;
import querqy.model.convert.builder.ExpandedQueryBuilder;
import querqy.parser.QuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.FieldAwareWhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.QuerqyParserFactory;
import querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.select.ExpressionCriteriaSelectionStrategyFactory;
import querqy.rewrite.contrib.ReplaceRewriterFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryRewritingHandler {

    private final RewriteChain rewriteChain;

    private static final QuerqyParserFactory QUERQY_PARSER_FACTORY = new FieldAwareWhiteSpaceQuerqyParserFactory();

    private QueryRewritingHandler(final RewriteChain rewriteChain) {
        this.rewriteChain = rewriteChain;
    }

    // TODO: Implement InfoLogging
    public RewrittenQuery rewriteQuery(final String queryString) {
        final Map<String, String[]> params = new HashMap<>();

        final QuerqyParser querqyParser = QUERQY_PARSER_FACTORY.createParser();
        final SearchEngineRequestAdapter adapter = new LocalSearchEngineRequestAdapter(this.rewriteChain, params);
        final ExpandedQuery inputQuery = new ExpandedQuery(querqyParser.parse(queryString));

        final ExpandedQueryBuilder expandedQueryBuilder = new ExpandedQueryBuilder(
                this.rewriteChain.rewrite(inputQuery, adapter));

        final RewrittenQuery rewrittenQuery = new RewrittenQuery(expandedQueryBuilder);
        final Map<String, Object> context = adapter.getContext();

        if (context != null) {
            @SuppressWarnings("unchecked")
            final Set<Object> decorations = (Set<Object>) context.get(DecorateInstruction.DECORATION_CONTEXT_KEY);
            rewrittenQuery.setDecorations(decorations);

            @SuppressWarnings("unchecked")
            final Map<String, Object> namedDecorations =
                    (Map<String, Object>) context.get(DecorateInstruction.DECORATION_CONTEXT_MAP_KEY);
            rewrittenQuery.setNamedDecorations(namedDecorations);
        }

        return rewrittenQuery;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<RewriterFactory> rewriterFactories = new LinkedList<>();
        private int rewriterIdCounter = 0;

        // TODO: Replace these methods by a method addRewriterFactoryBuilder(...) and by builders in the respective
        //  factories
        public QueryRewritingHandler.Builder addReplaceRewriter(final String config) throws IOException {
            final String rewriterId = "querqy_replace_" + this.rewriterIdCounter++;
            rewriterFactories.add(new ReplaceRewriterFactory(
                    rewriterId,
                    new InputStreamReader(new ByteArrayInputStream(config.getBytes())),
                    true,
                    "\t",
                    new WhiteSpaceQuerqyParserFactory().createParser()));

            return this;
        }

        public QueryRewritingHandler.Builder addCommonRulesRewriter(final String config) throws IOException {
            final String rewriterId = "querqy_commonrules_" + this.rewriterIdCounter++;
            rewriterFactories.add(new SimpleCommonRulesRewriterFactory(
                    rewriterId,
                    new StringReader(config),
                    new WhiteSpaceQuerqyParserFactory(),
                    true,
                    Collections.emptyMap(),
                    new ExpressionCriteriaSelectionStrategyFactory(), false));

            return this;
        }

        public QueryRewritingHandler build() {
            return new QueryRewritingHandler(new RewriteChain(Collections.unmodifiableList(this.rewriterFactories)));
        }
    }
}
