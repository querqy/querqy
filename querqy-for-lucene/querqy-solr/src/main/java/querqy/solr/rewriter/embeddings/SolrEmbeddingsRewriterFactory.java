package querqy.solr.rewriter.embeddings;

import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.search.SolrCache;
import querqy.embeddings.Embedding;
import querqy.embeddings.EmbeddingCache;
import querqy.embeddings.EmbeddingModel;
import querqy.lucene.embeddings.EmbeddingsRewriterFactory;
import querqy.rewrite.RewriterFactory;
import querqy.solr.SolrRewriterFactoryAdapter;
import querqy.solr.utils.ConfigUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SolrEmbeddingsRewriterFactory extends SolrRewriterFactoryAdapter {

    static EmbeddingCache<String> NULL_CACHE = new EmbeddingCache<>() {
        @Override
        public Embedding getEmbedding(final String key) {
            return null;
        }

        @Override
        public void putEmbedding(final String key, final Embedding embedding) {
        }
    };

    public static final String CONF_MODEL = "model";
    public static final String CONF_CLASS = "class";
    public static final String CONF_CACHE_NAME = "cache";

    private EmbeddingModel model;

    public SolrEmbeddingsRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) {
        final Map<String, Object> modelConfig = (Map<String, Object>) config.get(CONF_MODEL);
        if (modelConfig == null) {
            throw new IllegalArgumentException("Missing config property" + CONF_MODEL);
        }
        final EmbeddingModel embeddingModel = getInstanceFromArg(modelConfig, CONF_CLASS, null);
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Missing " + CONF_MODEL + "/" + CONF_CLASS + "  property");
        }

        final EmbeddingCache<String> cache = ConfigUtils.getStringArg(modelConfig, CONF_CACHE_NAME)
                .map(name ->
                    (EmbeddingCache<String>) new SolrCacheAdapter(() -> SolrRequestInfo.getRequestInfo().getReq().getSearcher()
                                .getCache(name))).orElse(NULL_CACHE);

        embeddingModel.configure(modelConfig, cache);

        this.model = embeddingModel;


    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        try {
            // TODO: provide some more meaningful error details (for now we just try to configure ourselves and
            //  see what happens)
            configure(config);
        } catch (final Exception e) {
            return Collections.singletonList("Cannot configure this EmbeddingsRewriterFactory because: " +
                    e.getMessage());

        }
        return Collections.emptyList(); // it worked, no error message
    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return new EmbeddingsRewriterFactory(getRewriterId(), model);
    }

    // TODO: copied from query-solr ConfigUtils. Make it public there!
    static <V> V getInstanceFromArg(final Map<String, Object> config, final String propertyName, final V defaultValue) {

        final String classField = (String) config.get(propertyName);
        if (classField == null) {
            return defaultValue;
        }

        final String className = classField.trim();
        if (className.isEmpty()) {
            return defaultValue;
        }

        try {
            return (V) Class.forName(className).newInstance();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    static class SolrCacheAdapter implements EmbeddingCache<String> {

        final Supplier<SolrCache<String, Embedding>> cacheSupplier;

        SolrCacheAdapter(final Supplier<SolrCache<String, Embedding>> cacheSupplier) {
            this.cacheSupplier = cacheSupplier;
        }

        @Override
        public Embedding getEmbedding(final String key) {
            return cacheSupplier.get().get(key);
        }

        @Override
        public void putEmbedding(final String key, final Embedding embedding) {
            final SolrCache<String, Embedding> cache = cacheSupplier.get();
            cache.put(key, embedding);
        }
    }
}


