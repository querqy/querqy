package querqy.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import querqy.ComparableCharSequence;
import querqy.model.BooleanClause;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Term;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * <p>Facilitate boosting of (sub)phrases created from query terms, similar to Solr's (e)dismax pf, pf2, pf3.</p>
 */
public class PhraseBoosting {

    public enum NGramType {

        PHRASE(0), BI_GRAM(2), TRI_GRAM(3);

        public final int nGramSize;

        NGramType(final int nGramSize) {
            this.nGramSize = nGramSize;
        }

    }

    /**
     * Modified copy of org.apache.solr.search.FieldParams
     */
    public static class PhraseBoostFieldParams {

        private final NGramType nGramType;
        private final int slop;
        private final float boost;
        private final String field;

        public PhraseBoostFieldParams(final String field, final NGramType nGramType, final int slop, final float boost) {
            this.nGramType = nGramType;
            this.slop      = slop;
            this.boost     = boost;
            this.field     = field;
        }

        public NGramType getNGramType() {
            return nGramType;
        }
        public int getSlop() {
            return slop;
        }
        public float getBoost() {
            return boost;
        }
        public String getField() {
            return field;
        }

    }

    /**
     * <p>Make a phrase boost query.</p>
     * <p>Unlike in Solr, the scores of the queries from the (sub-)phrase levels, such as bi-grams, tri-grams and
     * complete phrase, are not simply adds up but combined using a
     * {@link org.apache.lucene.search.DisjunctionMaxQuery}. The phraseBoostTiebreaker parameter defines how
     * the scores for these levels are aggregated: a value of 1.0 will add up the scores, a value of 0.0 will pick the
     * highest score.</p>
     *
     * @param userQuery The main query from which to generate phrases
     * @param phraseBoostFieldParams A list of phrase boost parameters
     * @param phraseBoostTiebreaker The tie breaker for aggregating scores from the different phrase length levels
     * @param queryAnalyzer The Lucene query analyzer
     * @return An optional query that contains the phrase boost queries if any such query could be created
     */
    public static Optional<Query> makePhraseFieldsBoostQuery(final QuerqyQuery<?> userQuery,
                                                             final List<PhraseBoostFieldParams> phraseBoostFieldParams,
                                                             final float phraseBoostTiebreaker,
                                                             final Analyzer queryAnalyzer) {

        if (userQuery instanceof querqy.model.Query) {

            final List<BooleanClause> clauses = ((querqy.model.Query) userQuery).getClauses();

            if (clauses.size() > 1) {

                if (!phraseBoostFieldParams.isEmpty()) {

                    final List<String> sequence = new LinkedList<>();

                    for (final querqy.model.BooleanClause clause : clauses) {

                        if (clause instanceof DisjunctionMaxQuery) {

                            final DisjunctionMaxQuery dmq = (DisjunctionMaxQuery) clause;

                            if (dmq.occur != querqy.model.SubQuery.Occur.MUST_NOT) {

                                for (final DisjunctionMaxClause dmqClause : dmq.getClauses()) {
                                    if (dmqClause instanceof Term) {

                                        final ComparableCharSequence value = ((Term) dmqClause).getValue();
                                        final int length = value.length();
                                        final StringBuilder sb = new StringBuilder(length);
                                        for (int i = 0; i < length; i++) {
                                            sb.append(value.charAt(i));
                                        }
                                        sequence.add(sb.toString());
                                        break;
                                    }
                                }

                            }
                        }

                    }

                    if (sequence.size() > 1) {

                        final List<Query> disjuncts = new LinkedList<>();

                        final QueryBuilder queryBuilder = new QueryBuilder(queryAnalyzer);

                        final List<String>[] shingles = new List[4];
                        String pf = null;


                        for (final PhraseBoosting.PhraseBoostFieldParams fieldParams : phraseBoostFieldParams) {

                            final PhraseBoosting.NGramType nGramType = fieldParams.getNGramType();
                            final int slop = fieldParams.getSlop();
                            final String fieldname = fieldParams.getField();

                            if (nGramType == PhraseBoosting.NGramType.PHRASE) {

                                if (pf == null) {
                                    final StringBuilder sb = new StringBuilder(sequence.size() * 7);
                                    for (final String term : sequence) {
                                        if (sb.length() > 0) {
                                            sb.append(' ');
                                        }
                                        sb.append(term);
                                    }
                                    pf = sb.toString();
                                }
                                final Query pq = queryBuilder.createPhraseQuery(fieldname, pf, slop);
                                if (pq != null) {
                                    disjuncts.add(LuceneQueryUtil.boost(pq, fieldParams.getBoost()));
                                }

                            } else if (nGramType.nGramSize <= sequence.size()) {

                                if (shingles[nGramType.nGramSize] == null) {
                                    shingles[nGramType.nGramSize] = new LinkedList<>();
                                    for (int i = 0, lenI = sequence.size() - nGramType.nGramSize + 1; i < lenI; i++) {
                                        final StringBuilder sb = new StringBuilder();

                                        for (int j = i, lenJ = j + nGramType.nGramSize; j < lenJ; j++) {
                                            if (sb.length() > 0) {
                                                sb.append(' ');
                                            }
                                            sb.append(sequence.get(j));
                                        }
                                        shingles[nGramType.nGramSize].add(sb.toString());
                                    }
                                }


                                final List<Query> nGramQueries = new ArrayList<>(shingles[nGramType.nGramSize].size());

                                for (final String nGram : shingles[nGramType.nGramSize]) {
                                    final Query pq = queryBuilder.createPhraseQuery(fieldname, nGram, slop);
                                    if (pq != null) {
                                        nGramQueries.add(pq);
                                    }
                                }

                                switch (nGramQueries.size()) {
                                    case 0: break;
                                    case 1: {

                                        final Query nGramQuery = nGramQueries.get(0);
                                        disjuncts.add(LuceneQueryUtil.boost(nGramQuery, fieldParams.getBoost()));
                                        break;

                                    }
                                    default:

                                        final BooleanQuery.Builder builder = new BooleanQuery.Builder();

                                        for (final Query nGramQuery : nGramQueries) {
                                            builder.add(nGramQuery, org.apache.lucene.search.BooleanClause.Occur.SHOULD);
                                        }

                                        final BooleanQuery bq = builder.build();
                                        disjuncts.add(LuceneQueryUtil.boost(bq, fieldParams.getBoost()));
                                }
                            }
                        }

                        switch (disjuncts.size()) {
                            case 0: break;
                            case 1: return Optional.of(disjuncts.get(0));
                            default :
                                return Optional.of(new org.apache.lucene.search.DisjunctionMaxQuery(disjuncts,
                                        phraseBoostTiebreaker));
                        }

                    }


                }
            }
        }

        return Optional.empty();

    }
}
