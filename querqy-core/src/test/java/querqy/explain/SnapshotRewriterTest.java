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

import org.junit.Test;
import querqy.model.BooleanQuery;
import querqy.model.BoostedPhraseQuery;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.PhraseQuery;
import querqy.model.StringRawQuery;
import querqy.model.convert.builder.BooleanQueryBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static querqy.explain.SnapshotRewriter.BOOST_QUERIES;
import static querqy.explain.SnapshotRewriter.DOWN;
import static querqy.explain.SnapshotRewriter.FILTER_QUERIES;
import static querqy.explain.SnapshotRewriter.MATCHING_QUERY;
import static querqy.explain.SnapshotRewriter.MULT;
import static querqy.explain.SnapshotRewriter.PROP_BOOST;
import static querqy.explain.SnapshotRewriter.PROP_FIELD;
import static querqy.explain.SnapshotRewriter.PROP_GENERATED;
import static querqy.explain.SnapshotRewriter.PROP_OCCUR;
import static querqy.explain.SnapshotRewriter.PROP_SLOP;
import static querqy.explain.SnapshotRewriter.PROP_TERMS;
import static querqy.explain.SnapshotRewriter.TYPE_PHRASE_QUERY;
import static querqy.explain.SnapshotRewriter.UP;

import static java.util.Arrays.asList;

public class SnapshotRewriterTest {
    @Test
    public void testPhraseQuerySnapshot() {
        final PhraseQuery pq = new PhraseQuery(null, Clause.Occur.MUST, false, "f1", asList("hello", "world"), 2);
        final ExpandedQuery eq = new ExpandedQuery(pq);

        final SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        final Map<String, Object> snapshot = rewriter.getSnapshot();
        final Map<String, Object> matchingQuery = (Map<String, Object>) snapshot.get(MATCHING_QUERY);
        final Map<String, Object> phrase = (Map<String, Object>) matchingQuery.get(TYPE_PHRASE_QUERY);

        assertNotNull(phrase);
        assertEquals("MUST", phrase.get(PROP_OCCUR));
        assertEquals(false, phrase.get(PROP_GENERATED));
        assertEquals("f1", phrase.get(PROP_FIELD));
        assertEquals(asList("hello", "world"), phrase.get(PROP_TERMS));
        assertEquals(2, phrase.get(PROP_SLOP));
        assertFalse(phrase.containsKey(PROP_BOOST));
    }

    @Test
    public void testBoostedPhraseQuerySnapshot() {
        final BoostedPhraseQuery bpq = new BoostedPhraseQuery(null, Clause.Occur.SHOULD, null,
                asList("red", "shoes"), 0, 1.5f);
        final ExpandedQuery eq = new ExpandedQuery(BooleanQueryBuilder.bq("dummy").build());
        eq.addBoostUpQuery(new BoostQuery(bpq, 1.5f));

        final SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        final List<Map<String, Object>> upQueries =
                (List<Map<String, Object>>) ((Map<?, ?>) rewriter.getSnapshot().get(BOOST_QUERIES)).get(UP);
        assertNotNull(upQueries);
        assertEquals(1, upQueries.size());

        final Map<String, Object> phrase = (Map<String, Object>)
                ((Map<?, ?>) upQueries.get(0).get("query")).get(TYPE_PHRASE_QUERY);
        assertNotNull(phrase);
        assertEquals(asList("red", "shoes"), phrase.get(PROP_TERMS));
        assertEquals(0, phrase.get(PROP_SLOP));
        assertEquals(1.5f, (float) phrase.get(PROP_BOOST), 0f);
        assertFalse(phrase.containsKey(PROP_FIELD));
    }

    @Test
    public void testBooleanSnapshot() {
        BooleanQuery bq = BooleanQueryBuilder.bq("a", "b", "a", "b").build();
        ExpandedQuery eq = new ExpandedQuery(bq);

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(MATCHING_QUERY);
        assertNotNull(snapshot.get(SnapshotRewriter.TYPE_BOOLEAN_QUERY));
    }

    @Test
    public void testRawSnapshot() {
        StringRawQuery srq = new StringRawQuery(null, "querqy rules!", Clause.Occur.MUST, true);
        ExpandedQuery eq = new ExpandedQuery(srq);

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(MATCHING_QUERY);
        assertNotNull(snapshot.get(SnapshotRewriter.TYPE_RAW_QUERY));
    }


    @Test
    public void testMatchAllSnapshot() {
        ExpandedQuery eq = new ExpandedQuery(new MatchAllQuery());

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(MATCHING_QUERY);
        assertThat(snapshot, hasEntry(SnapshotRewriter.TYPE_MATCH_ALL, Collections.emptyMap()));

    }

    @Test
    public void testComplexQuerySnapshot() {

        final ExpandedQuery eq = new ExpandedQuery(BooleanQueryBuilder.bq("a", "b").build());
        eq.addFilterQuery(new StringRawQuery(null, "raw", Clause.Occur.MUST, true));
        eq.addBoostUpQuery(new BoostQuery(BooleanQueryBuilder.bq("boost1", "boost2").build(), 10f));
        eq.addBoostDownQuery(new BoostQuery(new StringRawQuery(null, "boostdown", Clause.Occur.MUST, true), 20f));
        eq.addMultiplicativeBoostQuery(new BoostQuery(BooleanQueryBuilder.bq("boostmult").build(), 0.5f));

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        final Map<String, Object> snapshot = rewriter.getSnapshot();

        final Map<String, Object> matchingQuery = (Map<String, Object>) snapshot.get(MATCHING_QUERY);
        // we are simply verifying .toString() output below as using matchers would be tedious
        assertEquals("{BOOL={" +
                "occur=SHOULD, " +
                "clauses=[" +
                    "{DISMAX={" +
                        "occur=SHOULD, " +
                        "clauses=[" +
                            "{TERM={generated=false, value=a}}]}}, " +
                            "{DISMAX={occur=SHOULD, clauses=[" +
                                "{TERM={generated=false, value=b}}]}}]}}", matchingQuery.toString());

        final List<Map<String, Object>> filterQueries = (List<Map<String, Object>>) snapshot.get(FILTER_QUERIES);
        assertEquals(1, filterQueries.size());
        assertEquals("{RAW_QUERY=RawQuery [queryString=raw]}", filterQueries.get(0).toString());

        final Map<String, Object> boostQueries = (Map<String, Object>) snapshot.get(BOOST_QUERIES);

        assertNotNull(boostQueries);
        assertEquals(3, boostQueries.size());

        final List<Map<String, Object>> upQueries = (List<Map<String, Object>>) boostQueries.get(UP);
        assertEquals(1, upQueries.size());
        assertEquals("{query={" +
                        "BOOL={" +
                            "occur=SHOULD, " +
                            "clauses=[" +
                                "{DISMAX={" +
                                    "occur=SHOULD, " +
                                    "clauses=[" +
                                        "{TERM={generated=false, value=boost1}}]}}, " +
                                "{DISMAX={" +
                                    "occur=SHOULD, " +
                                    "clauses=[{TERM={generated=false, value=boost2}}]}}]}}, factor=10.0}",
                        upQueries.get(0).toString());

        final List<Map<String, Object>> downQueries = (List<Map<String, Object>>) boostQueries.get(DOWN);
        assertEquals(1, downQueries.size());
        assertEquals("{query={" +
                                "RAW_QUERY=RawQuery [queryString=boostdown]" +
                        "}, factor=20.0}",
                downQueries.get(0).toString());

        final List<Map<String, Object>> multiplicativeBoostQueries = (List<Map<String, Object>>) boostQueries.get(MULT);
        assertEquals(1, multiplicativeBoostQueries.size());
        assertEquals("{query={" +
                        "BOOL={" +
                            "occur=SHOULD, " +
                            "clauses=[" +
                                "{DISMAX={" +
                                    "occur=SHOULD, " +
                                    "clauses=[" +
                                        "{TERM={generated=false, value=boostmult}}]}}]}}, " +
                        "factor=0.5}", multiplicativeBoostQueries.get(0).toString());
    }

}
