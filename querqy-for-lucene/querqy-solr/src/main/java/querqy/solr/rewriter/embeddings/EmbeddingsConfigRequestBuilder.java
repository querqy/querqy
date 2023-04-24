package querqy.solr.rewriter.embeddings;

import querqy.embeddings.EmbeddingModel;
import querqy.solr.RewriterConfigRequestBuilder;

import java.util.HashMap;
import java.util.Map;

public class EmbeddingsConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private Map<String, Object> modelConfig;

    public EmbeddingsConfigRequestBuilder() {
        super(SolrEmbeddingsRewriterFactory.class);
    }

    @Override
    public Map<String, Object> buildConfig() {
        if (modelConfig == null) {
            throw new IllegalStateException("Missing: model");
        }
        return Map.of(SolrEmbeddingsRewriterFactory.CONF_MODEL, modelConfig);
    }

    public EmbeddingsConfigRequestBuilder model(final Class<? extends EmbeddingModel> modelClass,
                                                final Map<String, Object> config) {
        if (config != null && config.containsKey(SolrEmbeddingsRewriterFactory.CONF_CLASS)) {
            throw new IllegalArgumentException("Property " + SolrEmbeddingsRewriterFactory.CONF_CLASS +
                    " not allowed in config");
        }
        modelConfig = config != null ? new HashMap<>(config) : new HashMap<>();
        modelConfig.put(SolrEmbeddingsRewriterFactory.CONF_CLASS, modelClass.getName());
        return this;

    }
}
