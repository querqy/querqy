package querqy.solr;

import com.google.common.collect.Lists;
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
import querqy.solr.utils.ConfigUtils;
import querqy.solr.utils.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;
import static querqy.solr.QuerqyRewriterRequestHandler.PARAM_ACTION;
import static querqy.solr.RewriterConfigRequestBuilder.CONF_CLASS;

/**
 * Querqy's default QParserPlugin. The produced QParser combines query rewriting with (e)dismax query QParserPlugin
 * features.
 */
public class ClassicQuerqyDismaxQParserPlugin extends QuerqyQParserPlugin {

    public QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params,
                                final SolrQueryRequest req, final RewriteChain rewriteChain,
                                final InfoLogging infoLogging, final TermQueryCache termQueryCache) {
        QuerqyDismaxQParser querqyDismaxQParser = new QuerqyDismaxQParser(qstr, localParams, params, req,
                createQuerqyParser(qstr, localParams, params, req), rewriteChain, infoLogging, termQueryCache);

        // do the magic
        loadRewriteChain(req.getCore());

        return querqyDismaxQParser;
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

                    //TODO pre checks?

                    try {
                        final ModifiableSolrParams params = new ModifiableSolrParams();
                        params.set(PARAM_ACTION, SAVE.name());
                        final SolrQueryRequestBase req = new SolrQueryRequestBase(core, params) {
                        };
                        final Map<String, Object> jsonBody = new HashMap<>();
                        final String className = (String) config.remove("class");
                        jsonBody.put(CONF_CLASS, className);

                        ConfigUtils
                                .newInstance(className, ClassicConfigurationParser.class)
                                .parseConfiguration(config, resourceLoader);

                        for (Map.Entry<String, ?> e : config.asShallowMap().entrySet()) {
                            logger.warn("{}: Parameter that needs to be handled in the #parseConfiguration function:{}/{}", className, e.getKey(), e.getValue());
                        }

                        req.setContentStreams(newArrayList(new ContentStreamBase.StringStream(JsonUtil.toJson(jsonBody), StandardCharsets.UTF_8.name())));

                        final SolrQueryResponse rsp = new SolrQueryResponse();
                        requestHandler.getSubHandler(id).handleRequest(req, rsp);
                    } catch (Exception e) {
                        // TODO
                    }

                    //TODO Check the response

                    /**
                     http://localhost:8983/solr/CORE/querqy/rewriter/commonrules1?action=save' \
                     --data-raw '{
                     "class": "querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory",
                     "config": {
                     "rules" : "notebook =>\nSYNONYM: laptop"
                     }
                     }'
                     **/

                }
            }
        }

    }
}
