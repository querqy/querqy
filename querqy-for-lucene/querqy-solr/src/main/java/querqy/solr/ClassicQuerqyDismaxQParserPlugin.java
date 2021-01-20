package querqy.solr;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.QParser;
import querqy.infologging.InfoLogging;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.rewrite.RewriteChain;
import querqy.solr.rewriter.ClassicConfigurationParser;
import querqy.solr.utils.JsonUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.QuerqyRewriterRequestHandler.PARAM_ACTION;

/**
 * Querqy's default QParserPlugin. The produced QParser combines query rewriting with (e)dismax query QParserPlugin
 * features.
 */
public class ClassicQuerqyDismaxQParserPlugin extends QuerqyQParserPlugin {

    private final AtomicBoolean alreadyLoaded = new AtomicBoolean(false);

    public QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                final SolrQueryRequest req, final RewriteChain rewriteChain,
                                final InfoLogging infoLogging, final TermQueryCache termQueryCache) {
        if (alreadyLoaded.compareAndSet(false, true)) {
            loadRewriteChain(req.getCore());
        }

        return new QuerqyDismaxQParser(qstr, localParams, params, req,
                createQuerqyParser(qstr, localParams, params, req), rewriteChain, infoLogging, termQueryCache);
    }

    /**
     * TODO-1: Es dürfen keine rewriteChains definiert werden in dem anderen Parser.
     * TODO-2: Watcher für die ZK-Knoten deaktiveren. Am Request handler mitgeben.
     * TODO-3: Alles als Interface an die Factories auslagern
     */

    private void loadRewriteChain(SolrCore core) {

        final NamedList<?> chainConfig = (NamedList<?>) initArgs.get("rewriteChain");
        if (chainConfig != null) {

            SolrResourceLoader resourceLoader = core.getResourceLoader();
            QuerqyRewriterRequestHandler requestHandler = (QuerqyRewriterRequestHandler) core.getRequestHandler(super.rewriterRequestHandlerName);

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
                                .parseConfigurationToRequestHandlerBody(config, resourceLoader);

                        final SolrQueryRequestBase req = new SolrQueryRequestBase(core, params) {
                        };
                        req.setContentStreams(newArrayList(new ContentStreamBase.StringStream(JsonUtil.toJson(jsonBody), UTF_8.name())));

                        requestHandler.getSubHandler(id).handleRequest(req, new SolrQueryResponse());
                        //TODO Check the response?
                    } catch (RuntimeException e) {
                        logger.error("Could not parse rewrite entry: {}", config, e);
                        throw e;
                    }
                }
            }
        }

    }
}
