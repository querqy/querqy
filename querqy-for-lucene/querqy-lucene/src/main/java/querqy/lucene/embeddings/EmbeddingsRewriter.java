package querqy.lucene.embeddings;

import org.apache.lucene.search.KnnVectorQuery;
import querqy.CompoundCharSequence;
import querqy.embeddings.Embedding;
import querqy.embeddings.EmbeddingModel;
import querqy.lucene.LuceneRawQuery;
import querqy.model.*;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterOutput;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmbeddingsRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {


    public enum EmbeddingQueryMode {
        BOOST, MAIN_QUERY;

        public static EmbeddingQueryMode fromString(final String str) {
            for (final EmbeddingQueryMode mode : values()) {
                if (mode.name().equalsIgnoreCase(str)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("No such EmbeddingQueryMode " + str);
        }
    }

    private final List<Term> terms = new ArrayList<>();

    private final EmbeddingQueryMode queryMode;

    private final EmbeddingModel embeddingModel;

    private final int topK;

    private final String vectorField;

    private final float boost;

    public EmbeddingsRewriter(final EmbeddingModel embeddingModel, final EmbeddingQueryMode queryMode, final int topK,
                              final String vectorField) {
        // FIXME: it would probably be better to have 2 different rewriters, one for boosting and one for selection
        this(embeddingModel, queryMode, topK, vectorField, 1f);
    }

    public EmbeddingsRewriter(final EmbeddingModel embeddingModel, final EmbeddingQueryMode queryMode, final int topK,
                              final String vectorField, final float boost) {
        this.queryMode = queryMode;
        this.embeddingModel = embeddingModel;
        this.topK = topK;
        this.vectorField = vectorField;
        this.boost = boost;
    }


    @Override
    public RewriterOutput rewrite(final ExpandedQuery query,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        return RewriterOutput.builder().expandedQuery(
                        collectQueryString(query)
                                .map(embeddingModel::getEmbedding)
                                .map(embedding -> applyEmbedding(embedding, query))
                                .orElse(query))
                .build();

    }

    protected ExpandedQuery applyEmbedding(final Embedding embedding, final ExpandedQuery inputQuery) {
        KnnVectorQuery knnVectorQuery = new KnnVectorQuery(vectorField, embedding.asVector(), topK);
        LuceneRawQuery luceneRawQuery = new LuceneRawQuery(null, Clause.Occur.MUST,true, knnVectorQuery);

        switch (queryMode) {
            case BOOST:
                inputQuery.addBoostUpQuery(new BoostQuery(luceneRawQuery, boost));
                break;
            case MAIN_QUERY:
                inputQuery.setUserQuery(luceneRawQuery);
                break;
            default:
                throw new IllegalStateException("Unknown query mode: " + queryMode);
        }

        return inputQuery;
    }

    /**
     * Traverse the query graph, collect all the terms and join them into a string
     */
    protected Optional<String> collectQueryString(final ExpandedQuery query) {
        final QuerqyQuery<?> userQuery = query.getUserQuery();
        // make sure we get a query type that we can handle and not a match all query etc.
        if (userQuery instanceof Query) {
            terms.clear();
            this.visit((Query) userQuery);
            if (!terms.isEmpty()) {
                return Optional.of(new CompoundCharSequence(" ", terms).toString());
            }
        }
        return Optional.empty();
    }

    @Override
    public Node visit(final Term term) {
        terms.add(term);
        return null;
    }

}
