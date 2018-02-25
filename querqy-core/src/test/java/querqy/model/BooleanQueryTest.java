package querqy.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static querqy.model.Clause.Occur.MUST;
import static querqy.model.Clause.Occur.MUST_NOT;
import static querqy.model.Clause.Occur.SHOULD;
import static querqy.QuerqyMatchers.*;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rene on 08/07/2017.
 */
public class BooleanQueryTest {

    private Object TypeSafeMatcher;

    @Test
    public void testThatClonePreservesGeneratedAndOccur() throws Exception {
        BooleanQuery bq = new BooleanQuery(null, MUST_NOT, true);
        final BooleanClause clone = bq.clone(null);
        assertEquals(MUST_NOT, clone.getOccur());
        assertTrue(clone.isGenerated());
    }

    @Test
    public void testThatGeneratedIsPropagatedToClauses() throws Exception {

        BooleanQuery bq = new BooleanQuery(null, SHOULD, false);
        DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(bq, Clause.Occur.SHOULD, false);
        bq.addClause(dmq);
        dmq.addClause(new Term(dmq, "Test", false));

        final BooleanQuery clone = (BooleanQuery) bq.clone(null, MUST, true);

        assertThat(clone, bq(must(), dmq(should(), term("Test", true))));
        assertTrue(clone.isGenerated());
        assertTrue(clone.getClauses().get(0).isGenerated());
        assertEquals(MUST, clone.getOccur());

    }
}
