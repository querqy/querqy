package querqy.explain;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.BoostQuery;
import querqy.model.BoostedTerm;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.RawQuery;
import querqy.model.Term;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnapshotRewriter implements ContextAwareQueryRewriter {

    // Query types from Querqy's query object model
    public static final String TYPE_QUERY = "QUERY";
    public static final String TYPE_BOOLEAN_QUERY = "BOOL";
    public static final String TYPE_DISMAX = "DISMAX";
    public static final String TYPE_MATCH_ALL = "MATCH_ALL";
    public static final String TYPE_RAW_QUERY = "RAW_QUERY";
    public static final String TYPE_TERM = "TERM";

    // Top-level structure of the ExpandedQuery
    public static final String MATCHING_QUERY = "MATCHING_QUERY";
    public static final String FILTER_QUERIES = "FILTER_QUERIES";
    public static final String BOOST_QUERIES = "BOOST_QUERIES";
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    // properties further down the tree
    public static final String PROP_BOOST = "boost";
    public static final String PROP_QUERY = "query";
    public static final String PROP_FACTOR = "factor";
    public static final String PROP_OCCUR = "occur";
    public static final String PROP_CLAUSES = "clauses";
    public static final String PROP_FIELD = "field";
    public static final String PROP_GENERATED = "generated";
    public static final String PROP_VALUE = "value";

    private Map<String, Object> snapshot;

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query,
                                 final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        snapshot = new LinkedHashMap<>();

        final QuerySnapshotVisitor snapshooter = new QuerySnapshotVisitor();

        snapshot.put(MATCHING_QUERY, snapshooter.takeSnapshot(query.getUserQuery()));

        final Collection<BoostQuery> boostUpQueries = query.getBoostUpQueries();
        final Collection<BoostQuery> boostDownQueries = query.getBoostDownQueries();
        final List<Map<String, Object>> up;
        final List<Map<String, Object>> down;

        if (boostUpQueries != null) {
            up = boostUpQueries.stream().map(bq -> {
                final LinkedHashMap<String, Object> data = new LinkedHashMap<>(2);
                data.put(PROP_QUERY, snapshooter.takeSnapshot(bq.getQuery()));
                data.put(PROP_FACTOR, bq.getBoost());
                return data;
            }).collect(Collectors.toList());
        } else {
            up = null;
        }

        if (boostDownQueries != null) {
            down = boostDownQueries.stream().map(bq -> {
                final LinkedHashMap<String, Object> data = new LinkedHashMap<>(2);
                data.put(PROP_QUERY, snapshooter.takeSnapshot(bq.getQuery()));
                data.put(PROP_FACTOR, bq.getBoost());
                return data;
            }).collect(Collectors.toList());
        } else {
            down = null;
        }

        if (up != null || down !=null) {
            final Map<String, List<?>> boosts = new HashMap<>();
            if (up != null) {
                boosts.put(UP, up);
            }
            if (down != null) {
                boosts.put(DOWN, down);
            }
            snapshot.put(BOOST_QUERIES, boosts);
        }

        final Collection<QuerqyQuery<?>> filterQueries = query.getFilterQueries();
        if (filterQueries != null) {
            snapshot.put(FILTER_QUERIES, filterQueries.stream()
                    .map(snapshooter::takeSnapshot)
                    .collect(Collectors.toList()));
        }


        //final Collection<BoostQuery> boostUpQueries = query.getBoostUpQueries();
        return query;
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query) {
        throw new UnsupportedOperationException("This rewriter needs a query context");
    }

    public Map<String, Object> getSnapshot() {
        return snapshot == null ? Collections.emptyMap() : snapshot;
    }


    public static class QuerySnapshotVisitor extends AbstractNodeVisitor<Void> {

        Map<String, Object> snapshot = new HashMap<>();
        List<Map<String,?>> clauses = new LinkedList<>();

        public Map<String, Object> takeSnapshot(final QuerqyQuery<?> querqyQuery) {
            if (querqyQuery instanceof Query) {
                reset();
                visit((Query) querqyQuery);
            } else if (querqyQuery instanceof BooleanQuery) {
                visit((BooleanQuery) querqyQuery);
            } else if (querqyQuery instanceof MatchAllQuery) {
                visit((MatchAllQuery) querqyQuery);
            } else if (querqyQuery instanceof RawQuery) {
                visit((RawQuery) querqyQuery);
            } else {
                throw new IllegalArgumentException("Cannot handle query type " + querqyQuery.getClass());
            }

            return snapshot;

        }

        @Override
        public Void visit(final Query query) {
            snapshot.put(TYPE_QUERY, clauses);
            super.visit(query);
            return null;
        }

        @Override
        public Void visit(final BooleanQuery booleanQuery) {

            final Map<String, Object> bq = new HashMap<>();
            clauses.add(bq);
            final Map<String, Object> data = new LinkedHashMap<>(); // preserve order
            bq.put(TYPE_BOOLEAN_QUERY, data);
            data.put(PROP_OCCUR, booleanQuery.occur.name());

            // save reference
            final List<Map<String,?>> origClauses = clauses;
            clauses = new LinkedList<>();
            data.put(PROP_CLAUSES, clauses);

            super.visit(booleanQuery);

            snapshot.put(TYPE_BOOLEAN_QUERY, clauses);
            // restore
            clauses = origClauses;

            return null;
        }

        @Override
        public Void visit(final DisjunctionMaxQuery disjunctionMaxQuery) {
            final Map<String, Object> dmq = new HashMap<>();
            clauses.add(dmq);
            final Map<String, Object> data = new LinkedHashMap<>(); // preserve order
            dmq.put(TYPE_DISMAX, data);
            data.put(PROP_OCCUR, disjunctionMaxQuery.occur.name());

            // save reference
            final List<Map<String,?>> origClauses = clauses;
            clauses = new LinkedList<>();
            data.put(PROP_CLAUSES, clauses);

            super.visit(disjunctionMaxQuery);
            clauses = origClauses;

            return null;

        }

        @Override
        public Void visit(final Term term) {
            final Map<String, Object> tq = new HashMap<>();
            final HashMap props = new HashMap<>();

            if (term instanceof BoostedTerm) {
                props.put(PROP_BOOST, ((BoostedTerm) term).getBoost());
            }

            if (term.getField() != null) {
                props.put(PROP_FIELD, term.getField());
            }

            props.put(PROP_VALUE, term.getValue());
            props.put(PROP_GENERATED, term.isGenerated());
            tq.put(TYPE_TERM, props);
            clauses.add(tq);
            return null;
        }

        @Override
        public Void visit(final RawQuery rawQuery) {
            final Map<String, Object> rq = new HashMap<>();
            rq.put(TYPE_RAW_QUERY, rawQuery.toString());
            clauses.add(rq);
            snapshot.put(TYPE_RAW_QUERY, clauses);
            super.visit(rawQuery);
            return null;
        }

        @Override
        public Void visit(final MatchAllQuery query) {
            final Map<String, Object> maq = new HashMap<>();
            maq.put(TYPE_MATCH_ALL, Collections.EMPTY_MAP);
            clauses.add(maq);
            snapshot.put(TYPE_MATCH_ALL, clauses);
            super.visit(query);
            return null;
        }

        private void reset() {
            snapshot = new HashMap<>();
            clauses = new LinkedList<>();
        }
    }

}
