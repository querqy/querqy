package querqy.solr.rewriter.embeddings;

import querqy.embeddings.Embedding;
import querqy.embeddings.EmbeddingModel;

import java.util.Map;
import java.util.Random;

public class DummyEmbeddingModel implements EmbeddingModel {

    private static final float[] VECTOR_W1 = new float[] { 0.100f, -0.006f, -0.900f,  0.250f};
    private static final float[] VECTOR_W2 = new float[] {-0.100f,  0.006f,  0.900f, -0.250f};
    private static final float[] VECTOR_W3 = new float[] { 0.750f,  0.006f, -0.030f, -0.250f};
    private static final float[] VECTOR_W4 = new float[] {-0.070f,  0.010f,  0.800f, -0.180f};

    static final Map<String, float[]> EMBEDDINGS = Map.of(
            "w1", VECTOR_W1,
            "w2", VECTOR_W2,
            "w3", VECTOR_W3,
            "w4", VECTOR_W4
    );

    @Override
    public Embedding getEmbedding(final String text) {
        float[] emb = EMBEDDINGS.get(text);
        if (emb == null) {
            // use a stable (but meaningless) random vector if we don't have a pre-defined embedding
            final Random random = new Random(text.hashCode());
            emb = new float[4];
            for (int i = 0; i < emb.length; i++) {
                emb[i] = random.nextFloat();
            }
        }
        return Embedding.of(emb);
    }

}
