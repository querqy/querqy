package querqy.lucene.rewrite;

import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SingleFieldBoostTest extends TestCase {

    // we cannot stub hashcode/equals via Mockito - use a dummy and count invocations
    final static class DummyFieldBoost implements FieldBoost {

        public int equalsInvocations, hashCodeInvocations = 0;
        final boolean equalsResult;

        DummyFieldBoost(final boolean equalsResult) {
            this.equalsResult = equalsResult;
        }

        @Override
        public float getBoost(final String fieldname, final IndexReader indexReader) throws IOException {
            return 0;
        }

        @Override
        public void registerTermSubQuery(final TermSubQueryFactory termSubQueryFactory) {}

        @Override
        public String toString(final String fieldname) {
            return null;
        }

        @Override
        public int hashCode() {
            hashCodeInvocations++;
            return 0;
        }

        @Override
        public boolean equals(final Object obj) {
            equalsInvocations++;
            return equalsResult;
        }
    };



    @Test
    public void testEqualsNull() {

        final DummyFieldBoost delegate = new DummyFieldBoost(true);

        final SingleFieldBoost fieldBoost = new SingleFieldBoost("field1", delegate);

        assertNotEquals(fieldBoost, null);
    }

    @Test
    public void testEqualsSameFieldDifferentDelegate() {

        final DummyFieldBoost delegate1 = new DummyFieldBoost(false);
        final DummyFieldBoost delegate2 = new DummyFieldBoost(false);

        final SingleFieldBoost fieldBoost1 = new SingleFieldBoost("field1", delegate1);
        final SingleFieldBoost fieldBoost2 = new SingleFieldBoost("field1", delegate2);

        assertNotEquals(fieldBoost1, fieldBoost2);
        assertEquals(1, delegate1.equalsInvocations);
    }

    @Test
    public void testEqualsSameDelegateDifferentFields() {

        final DummyFieldBoost delegate1 = new DummyFieldBoost(true);
        final DummyFieldBoost delegate2 = new DummyFieldBoost(true);

        final SingleFieldBoost fieldBoost1 = new SingleFieldBoost("field1", delegate1);
        final SingleFieldBoost fieldBoost2 = new SingleFieldBoost("field2", delegate2);

        assertNotEquals(fieldBoost1, fieldBoost2);

    }

    @Test
    public void testEqualFieldsAndDelegates() {

        final DummyFieldBoost delegate1 = new DummyFieldBoost(true);
        final DummyFieldBoost delegate2 = new DummyFieldBoost(true);

        final SingleFieldBoost fieldBoost1 = new SingleFieldBoost("field3", delegate1);
        final SingleFieldBoost fieldBoost2 = new SingleFieldBoost("field3", delegate2);

        assertEquals(fieldBoost1, fieldBoost2);

        assertEquals(1, delegate1.equalsInvocations);
    }

    @Test
    public void testEqualsSameObject() {

        final DummyFieldBoost delegate = new DummyFieldBoost(true);

        final SingleFieldBoost fieldBoost = new SingleFieldBoost("field3", delegate);

        assertEquals(fieldBoost, fieldBoost);

    }

    @Test
    public void testEqualsDifferentClass() {

        final DummyFieldBoost delegate = new DummyFieldBoost(true);

        final SingleFieldBoost fieldBoost = new SingleFieldBoost("field3", delegate);

        assertNotEquals(fieldBoost, "I'm not a FieldBoost!");

    }



    @Test
    public void testHashCode() {
        final DummyFieldBoost delegate = new DummyFieldBoost(true);
        final SingleFieldBoost fieldBoost1 = new SingleFieldBoost("field1", delegate);
        final int code = fieldBoost1.hashCode();
        // was this passed on to the delegate field boost?
        assertEquals(1, delegate.hashCodeInvocations);

        // different hashCode for different field?
        assertNotEquals(code, new SingleFieldBoost("field2", delegate));
    }

    @Test
    public void testGetBoost() throws Exception {
        final String matchingField = "field1";
        final String otherField = "field2";

        final float boost = new Random().nextFloat() + 1f;
        final FieldBoost delegate = mock(FieldBoost.class);
        when(delegate.getBoost(ArgumentMatchers.eq(matchingField), any())).thenReturn(boost);

        final SingleFieldBoost fieldBoost = new SingleFieldBoost(matchingField, delegate);

        assertEquals(boost, fieldBoost.getBoost(matchingField, null), 0.0001);
        assertEquals(0f, fieldBoost.getBoost(otherField, null), 0.0001);
    }

    @Test
    public void testRegisterTermSubQuery() {

        final String field = "field";

        final FieldBoost delegate = mock(FieldBoost.class);
        final TermSubQueryFactory factory = mock(TermSubQueryFactory.class);

        final SingleFieldBoost fieldBoost = new SingleFieldBoost(field, delegate);
        fieldBoost.registerTermSubQuery(factory);
        verify(delegate).registerTermSubQuery(factory);

    }

    @Test
    public void testTestToString() {
        final String matchingField = "field1";
        final String otherField = "field2";

        final FieldBoost delegate = mock(FieldBoost.class);
        when(delegate.toString(matchingField)).thenReturn("Passed-through");

        final SingleFieldBoost fieldBoost = new SingleFieldBoost(matchingField, delegate);
        final String toStringFromDelegate = fieldBoost.toString(matchingField);
        final String toStringFromOtherField = fieldBoost.toString(otherField);

        assertTrue(toStringFromDelegate.contains("Passed-through"));
        assertFalse(toStringFromOtherField.contains("Passed-through"));
        assertTrue(toStringFromOtherField.contains("0.0"));
        verify(delegate, times(1)).toString(any());

        
    }
}