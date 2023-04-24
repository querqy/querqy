package querqy.embeddings;

import java.util.Map;

public interface EmbeddingModel {

    default void configure(final Map<String, Object> config, final EmbeddingCache<String> embeddingCache) {
    }

    Embedding getEmbedding(String text);
}
