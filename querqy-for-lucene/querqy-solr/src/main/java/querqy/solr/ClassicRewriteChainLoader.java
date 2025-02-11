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
import querqy.solr.utils.ConfigUtils;
import querqy.solr.utils.JsonUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static querqy.solr.QuerqyQParserPlugin.CONF_REWRITER_REQUEST_HANDLER;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME;
import static querqy.solr.QuerqyRewriterRequestHandler.PARAM_ACTION;

@Deprecated
public class ClassicRewriteChainLoader extends AbstractSolrEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClassicRewriteChainLoader.class);

    private String rewriterRequestHandlerName;

    public ClassicRewriteChainLoader(final SolrCore core) {
        super(core);
        LOG.warn("You are using a temporary and deprecated solution to load rewriters. Please migrate to the rewriter " +
                "API soon");
    }

    @Override
    public void init(final NamedList args) {
        super.init(args);
        final String name = ConfigUtils.get(getArgs(), CONF_REWRITER_REQUEST_HANDLER, DEFAULT_HANDLER_NAME).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(CONF_REWRITER_REQUEST_HANDLER + " cannot be empty");
        }
        rewriterRequestHandlerName = name;
    }

    @Override
    public void newSearcher(final SolrIndexSearcher newSearcher, final SolrIndexSearcher currentSearcher) {
        if (currentSearcher == null) {
            loadRewriteChain(newSearcher.getCore());
        }
    }

    String getRewriterRequestHandler() {
        return this.rewriterRequestHandlerName;
    }

    private void loadRewriteChain(final SolrCore core) {

        final NamedList<?> chainConfig = (NamedList<?>) getArgs().get("rewriteChain");
        if (chainConfig != null) {

            final GZIPAwareResourceLoader resourceLoader = new GZIPAwareResourceLoader(core.getResourceLoader());
            final QuerqyRewriterRequestHandler requestHandler = (QuerqyRewriterRequestHandler) core
                    .getRequestHandler(rewriterRequestHandlerName);

            @SuppressWarnings("unchecked") final List<NamedList<?>> rewriterConfigs = (List<NamedList<?>>) chainConfig.getAll("rewriter");
            if (rewriterConfigs != null && !rewriterConfigs.isEmpty()) {

                if (requestHandler.isPersistingRewriters()) {
                    LOG.error("Must not configure rewriters in solrconfig.xml if QuerqyRewriterRequestHandler persists rewriters. \n" +
                            "Did you forget to set rewriterStorage=inMemory " +
                            "[https://docs.querqy.org/querqy/querqy5-solr-migration.html?highlight=inmemory#configuring-and-using-rewriters]");
                    throw new RuntimeException("Cannot load rewriter from solrconfig.xml " +
                            "if QuerqyRewriterRequestHandler rewriterStorage is not 'inMemory'\n" +
                            "[https://docs.querqy.org/querqy/querqy5-solr-migration.html?highlight=inmemory#configuring-and-using-rewriters]");
                }

                final Set<String> seenRewriterIds = new HashSet<>(rewriterConfigs.size());

                for (NamedList<?> config : rewriterConfigs) {

                    final String id = (String) config.get("id");
                    if (id == null) {
                        throw new IllegalArgumentException("Rewriter missing id field!");
                    }

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
                        req.setContentStreams(singleton(new ContentStreamBase.StringStream(JsonUtil.toJson(jsonBody),
                                UTF_8.name())));

                        final SolrQueryResponse response = new SolrQueryResponse();
                        requestHandler.getSubHandler(id).handleRequest(req, response);
                        if (response.getException() != null) {
                            throw new IllegalStateException("Could not upload rewriter " + id, response.getException());
                        }
                    } catch (final RuntimeException e) {
                        LOG.error("Could not parse rewrite entry: {}", config, e);
                        throw e;
                    }
                }
            }
        }
    }
}
