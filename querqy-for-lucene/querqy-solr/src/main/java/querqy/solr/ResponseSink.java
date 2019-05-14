package querqy.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import querqy.infologging.Sink;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ResponseSink implements Sink {

    private static final String CONTEXT_KEY = ResponseSink.class.getName() + ".MESSAGES";


    @Override
    public void log(final Object message, final String rewriterId,
                    final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        @SuppressWarnings("unchecked")
        final Map<String, List<Object>> messages = (Map<String, List<Object>>) searchEngineRequestAdapter
                .getContext().computeIfAbsent(CONTEXT_KEY, key -> new TreeMap<>());

        messages.computeIfAbsent(rewriterId, key -> new ArrayList<>()).add(message);

    }

    @Override
    public void endOfRequest(final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        @SuppressWarnings("unchecked")
        final Map<String, List<Object>> messages = (Map<String, List<Object>>) searchEngineRequestAdapter.getContext()
                .get(CONTEXT_KEY);

        if (messages != null && !messages.isEmpty()) {
            final SolrQueryResponse rsp = SolrRequestInfo.getRequestInfo().getRsp();
            rsp.add("querqy.infoLog", messages);
        }

    }
}
