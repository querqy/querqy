package querqy.solr;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import querqy.lucene.GZIPAwareResourceLoader;
import querqy.solr.rewriter.ClassicConfigurationParser;
import querqy.solr.utils.JsonUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.QuerqyQParserPlugin.CONF_REWRITER_REQUEST_HANDLER;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;
import static querqy.solr.QuerqyRewriterRequestHandler.PARAM_ACTION;

public class ClassicRewriteChainLoader extends AbstractSolrEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(TermQueryCachePreloader.class);

    private String rewriterRequestHandlerName;

    public ClassicRewriteChainLoader(SolrCore core) {
        super(core);
    }

    @Override
    public void init(NamedList args) {
        super.init(args);

        final String rewriteHandlerName = (String) getArgs().get(CONF_REWRITER_REQUEST_HANDLER);
        rewriterRequestHandlerName = !isNullOrEmpty(rewriteHandlerName) ? rewriteHandlerName : DEFAULT_HANDLER_NAME;
    }

    @Override
    public void newSearcher(SolrIndexSearcher newSearcher, SolrIndexSearcher currentSearcher) {
        loadRewriteChain(newSearcher.getCore());
    }

    private void loadRewriteChain(SolrCore core) {

        final NamedList<?> chainConfig = (NamedList<?>) getArgs().get("rewriteChain");
        if (chainConfig != null) {

            GZIPAwareResourceLoader resourceLoader = new GZIPAwareResourceLoader(core.getResourceLoader());
            QuerqyRewriterRequestHandler requestHandler = (QuerqyRewriterRequestHandler) core.getRequestHandler(rewriterRequestHandlerName);

            @SuppressWarnings("unchecked") final List<NamedList<?>> rewriterConfigs = (List<NamedList<?>>) chainConfig.getAll("rewriter");
            if (rewriterConfigs != null) {

                final Set<String> seenRewriterIds = new HashSet<>(rewriterConfigs.size());

                for (NamedList<?> config : rewriterConfigs) {

                    final String id = (String) config.get("id");

                    if (!seenRewriterIds.add(id)) {
                        throw new RuntimeException("Rewriter id: " + id + "already defined.");
                    }

                    try {
                        final ModifiableSolrParams params = new ModifiableSolrParams();
                        params.set(PARAM_ACTION, SAVE.name());

                        final String className = (String) config.get("class");

                        final Map<String, Object> jsonBody = ((ClassicConfigurationParser) SolrRewriterFactoryAdapter
                                .loadInstance(id, className))
                                .parseConfigurationToRequestHandlerBody((NamedList<Object>) config, resourceLoader);

                        final SolrQueryRequestBase req = new SolrQueryRequestBase(core, params) {
                        };
                        req.setContentStreams(newArrayList(new ContentStreamBase.StringStream(JsonUtil.toJson(jsonBody), UTF_8.name())));

                        requestHandler.getSubHandler(id).handleRequest(req, new SolrQueryResponse());
                        //TODO Check the response?
                    } catch (RuntimeException e) {
                        LOG.error("Could not parse rewrite entry: {}", config, e);
                        throw e;
                    }
                }
            }
        }

    }
}
