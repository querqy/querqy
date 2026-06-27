/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.explain;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.BoostQuery;
import querqy.model.BoostedPhraseQuery;
import querqy.model.BoostedTerm;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.PhraseQuery;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.RawQuery;
import querqy.rewrite.RewriterOutput;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnapshotRewriter implements QueryRewriter {

    // Query types from Querqy's query object model
    public static final String TYPE_QUERY = "QUERY";
    public static final String TYPE_BOOLEAN_QUERY = "BOOL";
    public static final String TYPE_DISMAX = "DISMAX";
    public static final String TYPE_MATCH_ALL = "MATCH_ALL";
    public static final String TYPE_PHRASE_QUERY = "PHRASE";
    public static final String TYPE_RAW_QUERY = "RAW_QUERY";
    public static final String TYPE_TERM = "TERM";

    // Top-level structure of the ExpandedQuery
    public static final String MATCHING_QUERY = "MATCHING_QUERY";
    public static final String FILTER_QUERIES = "FILTER_QUERIES";
    public static final String BOOST_QUERIES = "BOOST_QUERIES";
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String MULT = "MULT";

    // properties further down the tree
    public static final String PROP_BOOST = "boost";
    public static final String PROP_QUERY = "query";
    public static final String PROP_FACTOR = "factor";
    public static final String PROP_OCCUR = "occur";
    public static final String PROP_CLAUSES = "clauses";
    public static final String PROP_FIELD = "field";
    public static final String PROP_GENERATED = "generated";
    public static final String PROP_VALUE = "value";
    public static final String PROP_TERMS = "terms";
    public static final String PROP_SLOP = "slop";

    private Map<String, Object> snapshot;

    @Override
    public RewriterOutput rewrite(final ExpandedQuery query,
                                  final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        snapshot = new LinkedHashMap<>();

        final QuerySnapshotVisitor snapshooter = new QuerySnapshotVisitor();

        snapshot.put(MATCHING_QUERY, snapshooter.takeSnapshot(query.getUserQuery()));

        final Collection<BoostQuery> boostUpQueries = query.getBoostUpQueries();
        final Collection<BoostQuery> boostDownQueries = query.getBoostDownQueries();
        final Collection<BoostQuery> multiplicativeBoostQueries = query.getMultiplicativeBoostQueries();
        final List<Map<String, Object>> up;
        final List<Map<String, Object>> down;
        final List<Map<String, Object>> mult;

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

        if (multiplicativeBoostQueries != null) {
            mult = multiplicativeBoostQueries.stream().map(bq -> {
                final LinkedHashMap<String, Object> data = new LinkedHashMap<>(2);
                data.put(PROP_QUERY, snapshooter.takeSnapshot(bq.getQuery()));
                data.put(PROP_FACTOR, bq.getBoost());
                return data;
            }).collect(Collectors.toList());
        } else {
            mult = null;
        }

        if (up != null || down !=null || mult != null) {
            final Map<String, List<?>> boosts = new HashMap<>();
            if (up != null) {
                boosts.put(UP, up);
            }
            if (down != null) {
                boosts.put(DOWN, down);
            }
            if (mult != null) {
                boosts.put(MULT, mult);
            }
            snapshot.put(BOOST_QUERIES, boosts);
        }

        final Collection<QuerqyQuery<?>> filterQueries = query.getFilterQueries();
        if (filterQueries != null) {
            snapshot.put(FILTER_QUERIES, filterQueries.stream()
                    .map(snapshooter::takeSnapshot)
                    .collect(Collectors.toList()));
        }

        return RewriterOutput.builder().expandedQuery(query).build();
    }

    public Map<String, Object> getSnapshot() {
        return snapshot == null ? Collections.emptyMap() : snapshot;
    }


    public static class QuerySnapshotVisitor extends AbstractNodeVisitor<Void> {

        Map<String, Object> snapshot = new HashMap<>();
        List<Map<String,?>> clauses = null;

        public Map<String, Object> takeSnapshot(final QuerqyQuery<?> querqyQuery) {
            reset();
            if (querqyQuery instanceof Query) {
                visit((Query) querqyQuery);
            } else if (querqyQuery instanceof BooleanQuery) {
                visit((BooleanQuery) querqyQuery);
            } else if (querqyQuery instanceof MatchAllQuery) {
                visit((MatchAllQuery) querqyQuery);
            } else if (querqyQuery instanceof PhraseQuery) {
                visit((PhraseQuery) querqyQuery);
            } else if (querqyQuery instanceof RawQuery) {
                visit((RawQuery) querqyQuery);
            } else {
                throw new IllegalArgumentException("Cannot handle query type " + querqyQuery.getClass());
            }
            return snapshot;

        }

        @Override
        public Void visit(final Query query) {

            clauses = new LinkedList<>();
            super.visit(query);
            snapshot.put(TYPE_QUERY, clauses);
            return null;
        }

        @Override
        public Void visit(final BooleanQuery booleanQuery) {

            final Map<String, Object> data = new LinkedHashMap<>(); // preserve order
            data.put(PROP_OCCUR, booleanQuery.occur.name());

            add(TYPE_BOOLEAN_QUERY, data);

            // save reference
            final List<Map<String,?>> origClauses = clauses;
            clauses = new LinkedList<>();
            data.put(PROP_CLAUSES, clauses);

            super.visit(booleanQuery);

            // restore
            clauses = origClauses;

            return null;
        }

        @Override
        public Void visit(final DisjunctionMaxQuery disjunctionMaxQuery) {
            final Map<String, Object> data = new LinkedHashMap<>(); // preserve order
            data.put(PROP_OCCUR, disjunctionMaxQuery.occur.name());
            add(TYPE_DISMAX, data);

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
            final HashMap props = new HashMap<>();

            if (term instanceof BoostedTerm) {
                props.put(PROP_BOOST, ((BoostedTerm) term).getBoost());
            }

            if (term.getField() != null) {
                props.put(PROP_FIELD, term.getField());
            }

            props.put(PROP_VALUE, term.getValue());
            props.put(PROP_GENERATED, term.isGenerated());
            add(TYPE_TERM, props);
            return null;
        }

        @Override
        public Void visit(final PhraseQuery phraseQuery) {
            final Map<String, Object> props = new LinkedHashMap<>();
            props.put(PROP_OCCUR, phraseQuery.occur.name());
            props.put(PROP_GENERATED, phraseQuery.isGenerated());
            if (phraseQuery.getField() != null) {
                props.put(PROP_FIELD, phraseQuery.getField());
            }
            props.put(PROP_TERMS, phraseQuery.getTerms());
            props.put(PROP_SLOP, phraseQuery.getSlop());
            if (phraseQuery instanceof BoostedPhraseQuery) {
                props.put(PROP_BOOST, ((BoostedPhraseQuery) phraseQuery).getBoost());
            }
            add(TYPE_PHRASE_QUERY, props);
            return null;
        }

        @Override
        public Void visit(final RawQuery rawQuery) {
            add(TYPE_RAW_QUERY, rawQuery.toString());
            super.visit(rawQuery);
            return null;
        }

        @Override
        public Void visit(final MatchAllQuery query) {
            add(TYPE_MATCH_ALL, Collections.EMPTY_MAP);
            super.visit(query);
            return null;
        }

        private void add(final String name, final  Object data) {
            if (clauses != null) {
                final Map<String, Object> namedData = new HashMap<>();
                namedData.put(name, data);
                clauses.add(namedData);
            } else {
                snapshot.put(name, data);
            }
        }

        private void reset() {
            snapshot = new HashMap<>();
            clauses = null;
        }
    }

}
