package querqy.embeddings;

public interface EmbeddingCache<K> {

    Embedding getEmbedding(K key);

    void putEmbedding(K key, Embedding embedding);

}
