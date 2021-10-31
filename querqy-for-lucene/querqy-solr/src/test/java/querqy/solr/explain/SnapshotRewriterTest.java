package querqy.solr.explain;

import org.junit.Test;
import querqy.model.BooleanQuery;
import querqy.model.Clause;
import querqy.model.ExpandedQuery;
import querqy.model.MatchAllQuery;
import querqy.model.RawQuery;
import querqy.model.StringRawQuery;
import querqy.model.convert.builder.BooleanQueryBuilder;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class SnapshotRewriterTest {
    @Test
    public void testBooleanSnapshot() {
        BooleanQuery bq = BooleanQueryBuilder.bq("a", "b", "a", "b").build();
        ExpandedQuery eq = new ExpandedQuery(bq);

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(SnapshotRewriter.MATCHING_QUERY);
        assertNotNull(snapshot.get(SnapshotRewriter.TYPE_BOOLEAN_QUERY));
    }

    @Test
    public void testRawSnapshot() {
        StringRawQuery srq = new StringRawQuery(null, "querqy rules!", Clause.Occur.MUST, true);
        ExpandedQuery eq = new ExpandedQuery(srq);

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(SnapshotRewriter.MATCHING_QUERY);
        assertNotNull(snapshot.get(SnapshotRewriter.TYPE_RAW_QUERY));
    }


    @Test
    public void testMatchAllSnapshot() {
        ExpandedQuery eq = new ExpandedQuery(new MatchAllQuery());

        SnapshotRewriter rewriter = new SnapshotRewriter();
        rewriter.rewrite(eq, null);

        Map<String, Object> snapshot = (Map<String, Object>) rewriter.getSnapshot().get(SnapshotRewriter.MATCHING_QUERY);
        assertNotNull(snapshot.get(SnapshotRewriter.TYPE_MATCH_ALL));
    }
}
