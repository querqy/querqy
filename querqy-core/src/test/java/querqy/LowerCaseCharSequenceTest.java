package querqy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by rene on 02/11/2016.
 */
public class LowerCaseCharSequenceTest {

    @Test
    public void testThatToStringReturnsTheLowerCaseString() throws Exception {
        assertEquals("hello world!", new LowerCaseCharSequence("Hello WORLD!").toString());
    }

    @Test
    public void testThatCompareToIsCaseInsensitive() throws Exception {

        assertEquals(0, new LowerCaseCharSequence("Hello WORLD!").compareTo("hello world!"));

        LowerCaseCharSequence upperA = new LowerCaseCharSequence("ABC");
        LowerCaseCharSequence lowerA = new LowerCaseCharSequence("abc");

        LowerCaseCharSequence upperB = new LowerCaseCharSequence("BCD");
        LowerCaseCharSequence lowerB = new LowerCaseCharSequence("bcd");

        assertEquals(upperA.compareTo(upperB), lowerA.compareTo(lowerB));
        assertEquals(upperB.compareTo(upperA), lowerB.compareTo(lowerA));

    }

}
