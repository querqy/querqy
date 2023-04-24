package querqy.lucene.embeddings;

import querqy.embeddings.EmbeddingModel;
import querqy.lucene.embeddings.EmbeddingsRewriter.EmbeddingQueryMode;
import querqy.model.ExpandedQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;


public class EmbeddingsRewriterFactory extends RewriterFactory {

    public  static final String PARAM_QUERQY_PREFIX = "querqy.";
    public static final String PARAM_TOP_K = ".topK";
    public static final String PARAM_VECTOR_FIELD = ".f";
    public static final String PARAM_MODE = ".mode";
    public static final String PARAM_BOOST = ".boost";


    private final EmbeddingModel embeddingModel;

    public EmbeddingsRewriterFactory(final String rewriterId, final EmbeddingModel embeddingModel) {
        super(rewriterId);
        this.embeddingModel = embeddingModel;
    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final int topK = searchEngineRequestAdapter.getIntegerRequestParam(getParamName(PARAM_TOP_K)).orElse(10);
        final String vectorField = searchEngineRequestAdapter.getRequestParam(getParamName(PARAM_VECTOR_FIELD))
                .orElseThrow(() -> new IllegalArgumentException("Missing param " + getParamName(PARAM_VECTOR_FIELD)));

        final EmbeddingQueryMode embeddingQueryMode = searchEngineRequestAdapter.getRequestParam(getParamName(PARAM_MODE))
                .map(EmbeddingQueryMode::fromString)
                .orElse(EmbeddingQueryMode.MAIN_QUERY);

        // FIXME: it would probably be better to have 2 different rewriters, one for boosting and one for selection

        if (embeddingQueryMode == EmbeddingQueryMode.BOOST) {
            final float boost = searchEngineRequestAdapter.getFloatRequestParam(getParamName(PARAM_BOOST))
                    .orElseThrow(() -> new IllegalArgumentException("Missing param " + getParamName(PARAM_BOOST)));
            return new EmbeddingsRewriter(embeddingModel, embeddingQueryMode, topK, vectorField, boost);

        } else {
            return new EmbeddingsRewriter(embeddingModel, embeddingQueryMode, topK, vectorField);
        }
    }

    protected String getParamName(final String suffix) {
        return PARAM_QUERQY_PREFIX + getRewriterId() + suffix;
    }


}
