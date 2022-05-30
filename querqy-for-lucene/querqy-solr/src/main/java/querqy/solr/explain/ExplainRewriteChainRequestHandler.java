package querqy.solr.explain;

import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.metrics.SolrMetricsContext;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import querqy.explain.SnapshotRewriterFactory;
import querqy.lucene.QueryParsingController;
import querqy.parser.WhiteSpaceQuerqyParser;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.RewriterFactory;
import querqy.solr.DismaxSearchEngineRequestAdapter;
import querqy.solr.QuerqyQParserPlugin;
import querqy.solr.QuerqyRewriterRequestHandler;
import querqy.solr.SimpleQuerqyQParserFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExplainRewriteChainRequestHandler implements SolrRequestHandler {

    public static final String PARAM_REWRITERS = QuerqyQParserPlugin.PARAM_REWRITERS;

    private final QuerqyRewriterRequestHandler rewriterRequestHandler;

    public ExplainRewriteChainRequestHandler(final QuerqyRewriterRequestHandler rewriterRequestHandler) {
        this.rewriterRequestHandler = rewriterRequestHandler;
    }

    @Override
    public void init(final NamedList args) {

    }

    @Override
    public void handleRequest(final SolrQueryRequest req, final SolrQueryResponse rsp) {

        final SolrParams params = req.getParams();

        final List<RewriterFactory> factories = new LinkedList<>();
        factories.add(new SnapshotRewriterFactory("_parser"));

        String rewritersParam = params.get(PARAM_REWRITERS);

        final RewriteChain rewriteChain;
        if (rewritersParam != null) {

            final String[] rewriterIds = rewritersParam.split(",");

            for (final String rewriterId: rewriterIds) {

                final Optional<RewriterFactory> factoryOpt = rewriterRequestHandler
                        .getRewriterFactory(rewriterId.trim());

                if (factoryOpt.isPresent()) {
                    factories.add(factoryOpt.get());
                    factories.add(new SnapshotRewriterFactory(rewriterId));
                } else {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No such rewriter: " + rewriterId);
                }

            }


        }
        rewriteChain = new RewriteChain(factories);

        final String q = params.get(CommonParams.Q, "").trim();

        if (q.isEmpty()) {
            throw new SolrException(BAD_REQUEST, "Query string ' " + CommonParams.Q + " ' must not be empty");
        }

        final SimpleQuerqyQParserFactory factory = new SimpleQuerqyQParserFactory();
        factory.setQuerqyParserClass(WhiteSpaceQuerqyParser.class);


        final DismaxSearchEngineRequestAdapter requestAdapter = new DismaxSearchEngineRequestAdapter(
                null, req, q,
                params, factory.createParser(q, params, params, req), rewriteChain, null, null) {
            @Override
            protected Map<String, Float> parseQueryFields(final String fieldParamName, final Float defaultBoost,
                                                          final boolean useDefaultFieldAsFallback) {
                // the super class wants either qf or df params set, which we don't need for query rewriting
                return Collections.emptyMap();
            }
        };

        final QueryParsingController controller = new QueryParsingController(requestAdapter);

        rewriteChain.rewrite(controller.createExpandedQuery(), requestAdapter);

        final Map<String, Object> explain = new LinkedHashMap<>();
        explain.put("query_string", q);

        final Map<String, Object> parser = new LinkedHashMap<>();
        final Map<String, Object> parserOutput = new LinkedHashMap<>();
        parserOutput.put("query", ((SnapshotRewriterFactory) factories.get(0)).getSnapshot());
        parser.put("output", parserOutput);
        explain.put("parser", parser);

        final List<Map<String, Object>> chain = new LinkedList<>();

        for (int i = 1, len = factories.size(); i < len; i++) {
            if (i % 2 == 0) {
                final SnapshotRewriterFactory rewriterFactory = (SnapshotRewriterFactory) factories.get(i);
                final Map<String, Object> rewriter = new LinkedHashMap<>();
                rewriter.put("name", rewriterFactory.getPreviousRewriterId());
                final Map<String, Object> output = new LinkedHashMap<>();
                rewriter.put("output", output);
                output.put("query", rewriterFactory.getSnapshot());
                chain.add(rewriter);

            }
        }
        if (!chain.isEmpty()) {
            explain.put("chain", chain);
        }


        rsp.add("explain", explain);

    }

    @Override
    public String getName() {
        return ExplainRewriteChainRequestHandler.class.getSimpleName();
    }

    @Override
    public String getDescription() {
        return "Request handler to explain a Querqy rewrite chain";
    }

    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    @Override
    public void initializeMetrics(final SolrMetricsContext parentContext, final String scope) {
    }

    @Override
    public SolrMetricsContext getSolrMetricsContext() {
        return null;
    }
}
