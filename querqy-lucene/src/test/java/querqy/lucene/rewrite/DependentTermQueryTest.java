package querqy.lucene.rewrite;

import org.apache.lucene.index.Term;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

/**
 * Created by rene on 04/09/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class DependentTermQueryTest {

    FieldBoost fieldBoost1 = new ConstantFieldBoost(1f);


    FieldBoost fieldBoost2  = new ConstantFieldBoost(2f);

    @Mock
    DocumentFrequencyAndTermContextProvider dfc1;

    @Mock
    DocumentFrequencyAndTermContextProvider dfc2;

    Term term1 = new Term("f1", "t1");
    Term term2 = new Term("f1", "t2");


    int tqIndex1 = 1;
    int tqIndex2 = 2;

    @Before
    public void setUp() throws Exception {
        when(dfc1.termIndex()).thenReturn(tqIndex1);
        when(dfc2.termIndex()).thenReturn(tqIndex2);
    }

    @Test
    public void testThatHashCodeAndEqualDoNotDependOnDfc() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc2, tqIndex1, fieldBoost1);
        assertEquals(tq1.hashCode(), tq2.hashCode());

        assertEquals(tq1, tq2);
    }

    @Test
    public void testThatHashCodeAndEqualDependOnTerm() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term2, dfc1, tqIndex1, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnTqIndex() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc1, tqIndex2, fieldBoost1);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }

    @Test
    public void testThatHashCodeAndEqualDependOnFieldBoost() throws Exception {

        DependentTermQuery tq1 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost1);
        DependentTermQuery tq2 = new DependentTermQuery(term1, dfc1, tqIndex1, fieldBoost2);
        assertNotEquals(tq1.hashCode(), tq2.hashCode());

        assertNotEquals(tq1, tq2);

    }


}
