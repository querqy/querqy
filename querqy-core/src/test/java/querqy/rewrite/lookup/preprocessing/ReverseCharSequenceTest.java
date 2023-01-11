package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;
import querqy.rewrite.lookup.preprocessing.ReverseCharSequence;

import static org.junit.Assert.*;

public class ReverseCharSequenceTest {

    final static ReverseCharSequence TEST_SEQ = new ReverseCharSequence("1234567890");


    @Test
    public void testToString() {
        assertEquals("0987654321", TEST_SEQ.toString());
    }

    @Test
    public void testSubSequence() {
        assertEquals("987", TEST_SEQ.subSequence(1, 4).toString());
        assertEquals("0987654321", TEST_SEQ.subSequence(0, 10).toString());
    }

    @Test
    public void testCharAt() {
        assertEquals('0', TEST_SEQ.charAt(0));
        assertEquals('6', TEST_SEQ.charAt(4));
        assertEquals('1', TEST_SEQ.charAt(9));
    }

    @Test
    public void testEmptySequence() {
        assertEquals("", new ReverseCharSequence("").toString());
        assertEquals(0, new ReverseCharSequence("").length());
    }

    @Test
    public void testEquals() {
        assertEquals(new ReverseCharSequence("abcd"), new ReverseCharSequence("abcd"));
    }

    @Test
    public void testHashCode() {
        assertEquals(new ReverseCharSequence("abcd").hashCode(),
                new ReverseCharSequence("abcd").hashCode());
        assertNotEquals(new ReverseCharSequence("abcd").hashCode(),
                new ReverseCharSequence("d").hashCode());
    }

}