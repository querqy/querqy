package querqy.solr;

import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import querqy.lucene.rewrite.infologging.Sink;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResponseSink implements Sink {

    private static final String CONTEXT_KEY = ResponseSink.class.getName() + ".MESSAGES";
    public static final String QUERQY_INFO_LOG = "querqy_rewriteLogging";

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void log(final Object message, final String rewriterId,
                    final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final List<Object> messages = (List<Object>) searchEngineRequestAdapter
                .getContext().computeIfAbsent(CONTEXT_KEY, key -> new LinkedList<>());

        if (message instanceof List && !((List) message).isEmpty()) {
            messages.add(
                    Map.of("rewriterId", rewriterId, "actions", message)
            );

        } else {
            messages.add(Map.of("rewriterId", rewriterId));
        }
    }

    @Override
    public void endOfRequest(final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        @SuppressWarnings("unchecked")
        final List<Object> messages = (List<Object>) searchEngineRequestAdapter.getContext()
                .get(CONTEXT_KEY);

        if (messages != null && !messages.isEmpty()) {
            final SolrQueryResponse rsp = SolrRequestInfo.getRequestInfo().getRsp();
            rsp.add(QUERQY_INFO_LOG, Map.of("rewriteChainLogging", messages));
        }

    }

}
