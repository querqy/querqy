package querqy.solr;

import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.DELETE;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import querqy.solr.utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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

        final Map<String, Object> config = buildConfig();

        final List<String> errors = SolrRewriterFactoryAdapter
                .loadInstance("rewriterId1", rewriterFactoryAdapterClass.getName())
                .validateConfiguration(config);

        if ((errors != null) && !errors.isEmpty()) {
            throw new RuntimeException("Invalid configuration: " + String.join(", ", errors));
        }

        final Map<String, Object> request = new HashMap<>();
        request.put(CONF_CLASS, rewriterFactoryAdapterClass.getName());
        request.put(CONF_CONFIG, config);
        return JsonUtil.toJson(request);
    }

    public SaveRewriterConfigSolrRequest buildSaveRequest(final String rewriterId) {
        return buildSaveRequest(rewriterId, this);
    }

    public static SaveRewriterConfigSolrRequest buildSaveRequest(final String rewriterId,
                                                                 final RewriterConfigRequestBuilder requestBuilder) {
        return  new SaveRewriterConfigSolrRequest(rewriterId, requestBuilder.buildJson());
    }

    public static SaveRewriterConfigSolrRequest buildSaveRequest(final String requestHandlerName,
                                                                 final String rewriterId,
                                                                 final RewriterConfigRequestBuilder requestBuilder) {
        return new SaveRewriterConfigSolrRequest(requestHandlerName, rewriterId, requestBuilder.buildJson());
    }

    public static DeleteRewriterConfigSolrRequest buildDeleteRequest(final String rewriterId) {
        return new DeleteRewriterConfigSolrRequest(rewriterId);
    }

    public static DeleteRewriterConfigSolrRequest buildDeleteRequest(final String requestHandlerName,
                                                                     final String rewriterId) {
        return new DeleteRewriterConfigSolrRequest(requestHandlerName, rewriterId);
    }

    public static class DeleteRewriterConfigSolrRequest extends SolrRequest<DeleteRewriterConfigSolrSolrResponse> {

        public DeleteRewriterConfigSolrRequest(final String requestHandlerName, final String rewriterId) {
            super(SolrRequest.METHOD.POST, requestHandlerName + "/" + rewriterId);
        }

        public DeleteRewriterConfigSolrRequest(final String rewriterId) {
            this(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME, rewriterId);
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


    public static class DeleteRewriterConfigSolrSolrResponse extends SolrResponseBase { }

    public static class SaveRewriterConfigSolrRequest extends SolrRequest<SaveRewriterConfigSolrResponse> {

        private final String payload;

        public SaveRewriterConfigSolrRequest(final String requestHandlerName, final String rewriterId,
                                             final String payload) {
            super(SolrRequest.METHOD.POST, requestHandlerName + "/" + rewriterId);
            this.payload = payload;
        }

        public SaveRewriterConfigSolrRequest(final String rewriterId, final String payload) {
            this(QuerqyRewriterRequestHandler.DEFAULT_HANDLER_NAME, rewriterId, payload);
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

    public static class SaveRewriterConfigSolrResponse extends SolrResponseBase { }


}
