/**
 * 
 */
package querqy;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author rene
 *
 */
public class CompoundCharSequenceTest {
    
    @Test
    public void testCharAt() throws Exception {
        CompoundCharSequence seq = new CompoundCharSequence(Arrays.asList("cde-fgh", "-"));
        assertEquals(8, seq.length());
        for (int i = 0; i < 8; i++) {
            assertEquals("cde-fgh-".charAt(i), seq.charAt(i));
        }
        
    }
    
    
    @Test
    public void testSubSequence() throws Exception {
        CompoundCharSequence seq = new CompoundCharSequence(Arrays.asList("ab", "cd"));
        assertEquals("a", seq.subSequence(0, 1).toString());
        assertEquals("ab", seq.subSequence(0, 2).toString());
        assertEquals("bc", seq.subSequence(1, 3).toString());
        assertEquals("d", seq.subSequence(3, 4).toString());
        assertEquals("", seq.subSequence(0, 0).toString());
        assertEquals("", seq.subSequence(1, 1).toString());
        assertEquals("", seq.subSequence(4, 4).toString());
    }

}
