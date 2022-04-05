package querqy.rewrite.commonrules.model;

import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.SHOULD;
import static querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory.CONTEXT_KEY_FILTER_COMPLEMENT_QUERY;
import static querqy.rewrite.commonrules.SimpleCommonRulesRewriterFactory.CONTEXT_KEY_FILTER_COMPLEMENT_STRING;

import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.Query;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Term;
import querqy.model.Clause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.convert.builder.StringRawQueryBuilder;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author René Kriegler, @renekrie
 *
 * Created by rene on 07/07/2017.
 */
public class InstructionHelper {

    public static final String MATCH_ALL_FILTER_QUERY_STRING = "*:*";

    /**
     * Currently applies mm=100%
     * @param query The query on which mm should be applied
     * @return The query with mm applied
     */
    public static BooleanQuery applyMinShouldMatchAndGeneratedToBooleanQuery(final BooleanQuery query) {

        // TODO: avoid creating new query for single clause BQ

        final List<BooleanClause> clauses = query.getClauses();

        boolean applicable = true;
        for (final BooleanClause clause : clauses) {
            if (clause instanceof BooleanQuery || clause instanceof RawQuery) {
                applicable = false;
                break;
            }
        }

        final BooleanQuery newQuery = query instanceof Query
                ? new Query(true)
                : new BooleanQuery(query.getParent(), query.getOccur(), true);

        for (final BooleanClause clause : clauses) {
            if (applicable && clause.getOccur() == SHOULD) {
                newQuery.addClause(clause.clone(newQuery, MUST, true));
            } else {
                newQuery.addClause(clause.clone(newQuery, true));
            }
        }

        return newQuery;

    }

    public static QuerqyQuery<?> wrapWithOptionalFilterComplement(
            final SearchEngineRequestAdapter searchEngineRequestAdapter,
            final QuerqyQuery<?> originalFlterQuery
    ) {

        final Optional<String> filterComplementQueryString =
                Optional.ofNullable(
                        (String) searchEngineRequestAdapter.getContext().get(CONTEXT_KEY_FILTER_COMPLEMENT_STRING)
                );

        boolean isFilterComplementRequested =
                filterComplementQueryString.isPresent()
                        && (originalFlterQuery instanceof BooleanQuery
                                || originalFlterQuery instanceof StringRawQuery);

        if (!isFilterComplementRequested) {
            return originalFlterQuery.clone(null, true);
        }

        if (originalFlterQuery instanceof BooleanQuery) {
            return wrapBooleanQueryWithOptionalFilterComplement(
                    (BooleanQuery) originalFlterQuery,
                    (BooleanQuery) searchEngineRequestAdapter.getContext().get(CONTEXT_KEY_FILTER_COMPLEMENT_QUERY)
            );
        }

        return wrapRawQueryWithOptionalFilterComplement((StringRawQuery) originalFlterQuery, filterComplementQueryString.get());
    }

    public static QuerqyQuery<?> wrapBooleanQueryWithOptionalFilterComplement(final BooleanQuery filterQuery, final BooleanQuery filterComplementQuery) {

        BooleanQuery newQuery = filterQuery instanceof Query
                ? new Query(true)
                : new BooleanQuery(null, SHOULD, true);

        BooleanQuery bq1 = new BooleanQuery(newQuery, SHOULD, filterQuery.isGenerated());
        for (final BooleanClause clause : filterQuery.getClauses()) {
            bq1.addClause(clause.clone(bq1, clause.isGenerated()));
        }
        // a single negative BooleanClause needs combination with MatchAllDocsQuery
        if (bq1.getClauses().size() == 1
                    && Clause.Occur.MUST_NOT.equals(bq1.getClauses().get(0).getOccur())) {
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq1, SHOULD, true);
            bq1.addClause(dmq);
            dmq.addClause(new Term(dmq, MATCH_ALL_FILTER_QUERY_STRING));
        }
        newQuery.addClause(bq1);

        BooleanQuery bq2 = new BooleanQuery(newQuery, SHOULD, true);
        for (final BooleanClause clause : filterComplementQuery.getClauses()) {
            final DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq2, SHOULD, true);
            bq2.addClause(dmq);
            for (Term t : parseClause(dmq, clause)) {
                dmq.addClause(t);
            }
        }
        newQuery.addClause(bq2);
        return newQuery;

    }

    private static List<Term> parseClause(DisjunctionMaxQuery parent, BooleanClause originalClause) {

        return originalClause instanceof DisjunctionMaxQuery
                ? ((DisjunctionMaxQuery) originalClause).getClauses().stream().map(c -> parseTerm(parent, c.toString())).collect(Collectors.toList())
                : Collections.singletonList(parseTerm(parent, originalClause.toString()));

    }

    private static Term parseTerm(DisjunctionMaxQuery parent, String term) {

        String[] s = term.split(":");
        if (s.length == 2) {
            return new Term(parent, s[0], s[1], true);
        }
        return new Term(parent, term, true);

    }

    public static QuerqyQuery<?> wrapRawQueryWithOptionalFilterComplement(final StringRawQuery originalRawQuery, final String filterComplementQueryString) {

        String qs = originalRawQuery.getQueryString();
        // a single negative BooleanClause needs combination with MatchAllDocsQuery
        if (qs.startsWith("-") && !qs.contains(" ")) {
            qs = MATCH_ALL_FILTER_QUERY_STRING + " " + qs;
        }
        return StringRawQueryBuilder.raw(String.format("(%s) %s", qs, filterComplementQueryString)).build();

    }

}
