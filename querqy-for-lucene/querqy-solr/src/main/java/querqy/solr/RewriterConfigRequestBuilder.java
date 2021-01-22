package querqy.solr;

import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.DELETE;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.GET;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import querqy.solr.utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RewriterConfigRequestBuilder {

    public static final String CONF_CLASS = "class";
    public static final String CONF_CONFIG = "config";

    private final Class<? extends SolrRewriterFactoryAdapter> rewriterFactoryAdapterClass;

    public RewriterConfigRequestBuilder(final Class<? extends SolrRewriterFactoryAdapter> rewriterFactoryAdapterClass) {
        this.rewriterFactoryAdapterClass = rewriterFactoryAdapterClass;
    }

    public abstract Map<String, Object> buildConfig();

    public String buildJson() {
        return JsonUtil.toJson(buildDefinition());
    }

    public Map<String, Object> buildDefinition() {
        final Map<String, Object> config = buildConfig();

        final List<String> errors = SolrRewriterFactoryAdapter
                .loadInstance("rewriterId1", rewriterFactoryAdapterClass.getName())
                .validateConfiguration(config);

        if ((errors != null) && !errors.isEmpty()) {
            throw new RuntimeException("Invalid configuration: " + String.join(", ", errors));
        }

        final Map<String, Object> description = new HashMap<>();
        description.put(CONF_CLASS, rewriterFactoryAdapterClass.getName());
        description.put(CONF_CONFIG, config);
        return description;

    }


    public SaveRewriterConfigSolrRequest buildSaveRequest(final String rewriterId) {
        return buildSaveRequest(rewriterId, this);
    }

    public static SaveRewriterConfigSolrRequest buildSaveRequest(final String rewriterId,
                                                                 final RewriterConfigRequestBuilder requestBuilder) {
        return  new SaveRewriterConfigSolrRequest(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME, rewriterId,
                requestBuilder.buildJson());
    }

    public static SaveRewriterConfigSolrRequest buildSaveRequest(final String requestHandlerName,
                                                                 final String rewriterId,
                                                                 final RewriterConfigRequestBuilder requestBuilder) {
        return new SaveRewriterConfigSolrRequest(requestHandlerName, rewriterId, requestBuilder.buildJson());
    }

    public static DeleteRewriterConfigSolrRequest buildDeleteRequest(final String rewriterId) {
        return new DeleteRewriterConfigSolrRequest(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME, rewriterId);
    }

    public static DeleteRewriterConfigSolrRequest buildDeleteRequest(final String requestHandlerName,
                                                                     final String rewriterId) {
        return new DeleteRewriterConfigSolrRequest(requestHandlerName, rewriterId);
    }

    public static GetRewriterConfigSolrRequest buildGetRequest(final String rewriterId) {
        return new GetRewriterConfigSolrRequest(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME, rewriterId);
    }

    public static GetRewriterConfigSolrRequest buildGetRequest(final String requestHandlerName,
                                                               final String rewriterId) {
        return new GetRewriterConfigSolrRequest(requestHandlerName, rewriterId);
    }

    public static ListRewriterConfigsSolrRequest buildListRequest() {
        return buildListRequest(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME);
    }

    public static ListRewriterConfigsSolrRequest buildListRequest(final String requestHandlerName) {
        return new ListRewriterConfigsSolrRequest(requestHandlerName);
    }

    public static class SaveRewriterConfigSolrRequest extends SolrRequest<SaveRewriterConfigSolrResponse> {

        private final String payload;

        public SaveRewriterConfigSolrRequest(final String requestHandlerName, final String rewriterId,
                                             final String payload) {
            super(SolrRequest.METHOD.POST, requestHandlerName + "/" + rewriterId);
            this.payload = payload;
        }

        @Override
        public SolrParams getParams() {
            return SAVE.params();
        }


        @Override
        protected SaveRewriterConfigSolrResponse createResponse(final SolrClient client) {
            return new SaveRewriterConfigSolrResponse();
        }

        @Override
        public RequestWriter.ContentWriter getContentWriter(final String expectedType) {
            return new RequestWriter.ContentWriter() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                    writer.write(payload);
                    writer.flush();
                }

                @Override
                public String getContentType() {
                    return ClientUtils.TEXT_JSON;
                }
            };
        }
    }

    public static class DeleteRewriterConfigSolrRequest extends SolrRequest<DeleteRewriterConfigSolrSolrResponse> {

        public DeleteRewriterConfigSolrRequest(final String requestHandlerName, final String rewriterId) {
            super(SolrRequest.METHOD.POST, requestHandlerName + "/" + rewriterId);
        }

        @Override
        public SolrParams getParams() {
            return DELETE.params();
        }

        @Override
        protected DeleteRewriterConfigSolrSolrResponse createResponse(final SolrClient client) {
            return new DeleteRewriterConfigSolrSolrResponse();
        }

    }

    public static class GetRewriterConfigSolrRequest extends SolrRequest<GetRewriterConfigSolrResponse> {

        public GetRewriterConfigSolrRequest(final String requestHandlerName, final String rewriterId) {
            super(SolrRequest.METHOD.GET, requestHandlerName + (rewriterId != null ? ("/" + rewriterId) : ""));
        }

        @Override
        public SolrParams getParams() {
            return GET.params();
        }

        @Override
        protected GetRewriterConfigSolrResponse createResponse(final SolrClient client) {
            return new GetRewriterConfigSolrResponse();
        }

    }

    public static class ListRewriterConfigsSolrRequest extends SolrRequest<ListRewriterConfigsSolrResponse> {

        public ListRewriterConfigsSolrRequest(final String requestHandlerName) {
            super(SolrRequest.METHOD.GET, requestHandlerName);
        }

        @Override
        public SolrParams getParams() {
            return new MapSolrParams(Collections.emptyMap());
        }

        @Override
        protected ListRewriterConfigsSolrResponse createResponse(final SolrClient client) {
            return new ListRewriterConfigsSolrResponse();
        }
    }


    public static class SaveRewriterConfigSolrResponse extends SolrResponseBase { }

    public static class DeleteRewriterConfigSolrSolrResponse extends SolrResponseBase { }

    public static class GetRewriterConfigSolrResponse extends SolrResponseBase { }

    public static class ListRewriterConfigsSolrResponse extends SolrResponseBase {}


}
