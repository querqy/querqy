package querqy;

import static org.junit.Assert.*;

import org.junit.Test;

public class CharSequenceUtilTest {

    @Test
    public void testNullDoesNotCauseExceptionInEquals() {
        assertTrue(CharSequenceUtil.equals(null, null));
        assertFalse(CharSequenceUtil.equals("", null));
        assertFalse(CharSequenceUtil.equals(null, ""));
    }
    
    @Test
    public void testEqualSubstring() throws Exception {
        assertFalse(CharSequenceUtil.equals("", "a"));
        assertFalse(CharSequenceUtil.equals("a", ""));
        assertFalse(CharSequenceUtil.equals("ab", "abc"));
        assertFalse(CharSequenceUtil.equals("abc", "ab"));
    }
    
    @Test
    public void testEqualStrings() throws Exception {
        assertTrue(CharSequenceUtil.equals("abc", "abc"));
        assertTrue(CharSequenceUtil.equals("", ""));
    }
    
    @Test
    public void testEqualIsCaseSensitive() throws Exception {
        assertFalse(CharSequenceUtil.equals("ABC", "abc"));
        assertFalse(CharSequenceUtil.equals("abc", "ABC"));
    }
    
    @Test
    public void testClassHandling() throws Exception {
        
        assertTrue(CharSequenceUtil.equals("abc", new LowerCaseCharSequence("ABC")));
        assertTrue(CharSequenceUtil.equals(new LowerCaseCharSequence("ABC"), "abc"));
        
        assertFalse(CharSequenceUtil.equals("abc", new Object() {@Override
        public String toString() {
            return "abc";
        }}));
        
    }

}
