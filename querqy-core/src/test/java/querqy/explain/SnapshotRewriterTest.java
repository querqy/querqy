package querqy.explain;

import org.junit.Test;
import querqy.model.BooleanQuery;
import querqy.model.BoostQuery;
import querqy.model.Clause;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.StringRawQuery;
import querqy.model.convert.builder.BooleanQueryBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static querqy.explain.SnapshotRewriter.BOOST_QUERIES;
import static querqy.explain.SnapshotRewriter.DOWN;
import static querqy.explain.SnapshotRewriter.FILTER_QUERIES;
import static querqy.explain.SnapshotRewriter.MATCHING_QUERY;
import static querqy.explain.SnapshotRewriter.UP;

public class SnapshotRewriterTest {
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
        assertEquals(2, boostQueries.size());
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


    }

}
