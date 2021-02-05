package querqy.rewrite.commonrules.model;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import querqy.CharSequenceUtil;
import querqy.ComparableCharSequence;
import querqy.SimpleComparableCharSequence;

public class TermTest {

    @Test
    public void testThatToStringReturnsValidCharSequence() {
        final char[] chars = "abcde".toCharArray();
        assertEquals("abcde", new Term(chars, 0, chars.length, null).toString());
        assertEquals("bcd", new Term(chars, 1, 3, null).toString());
    }

    @Test
    public void testParseNoPlaceHolders() {
        char[] chars = "abc".toCharArray();
        Term term = new Term(chars, 0, chars.length, null);
        assertEquals(-1, term.getMaxPlaceHolderRef());
        assertNull(term.getPlaceHolders());
        
        term = new Term(chars, 1, chars.length -1, null);
        assertEquals(-1, term.getMaxPlaceHolderRef());
        assertNull(term.getPlaceHolders());
        
        term = new Term(chars, 1, chars.length -2, null);
        assertEquals(-1, term.getMaxPlaceHolderRef());
        assertNull(term.getPlaceHolders());
    }
    
    @Test
    public void testPlaceHolderOnly() throws Exception {
        char[] chars = "$3".toCharArray();
        Term term = new Term(chars, 0, chars.length, null);
        assertEquals(3, term.getMaxPlaceHolderRef());
        assertThat(term.getPlaceHolders(), contains(new PlaceHolder(0, 2, 3)));
    }
    
    @Test
    public void testPlaceHolderInTerm() throws Exception {
        char[] chars = "a$33b".toCharArray();
        Term term = new Term(chars, 0, chars.length, null);
        assertEquals(33, term.getMaxPlaceHolderRef());
        assertThat(term.getPlaceHolders(), contains(new PlaceHolder(1, 3, 33)));
        
        term = new Term(chars, 1, chars.length - 1, null);
        assertEquals(33, term.getMaxPlaceHolderRef());
        assertThat(term.getPlaceHolders(), contains(new PlaceHolder(1, 3, 33)));
        
        term = new Term(chars, 1, chars.length - 2, null);
        assertEquals(33, term.getMaxPlaceHolderRef());
        assertThat(term.getPlaceHolders(), contains(new PlaceHolder(1, 3, 33)));
    }
    
    @Test
    public void testReplacePlaceHoldersInMiddle() throws Exception {
        char[] chars = "a$1b".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("axyzb", filledPlaceholders));
        
    }
    
    @Test
    public void testReplacePlaceHolderOnly() throws Exception {
        char[] chars = "$1".toCharArray();
        Term term = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = term.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("xyz", filledPlaceholders));
        
    }
    
    @Test
    public void testReplacePlaceHolderAtBeginning() throws Exception {
        char[] chars = "$1a".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("xyza", filledPlaceholders));
        
    }
    
    @Test
    public void testReplacePlaceHolderAtEnd() throws Exception {
        char[] chars = "a$1".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("axyz", filledPlaceholders));
        
    }
    
    @Test
    public void testRepeatedPlaceHolders() throws Exception {
        char[] chars = "a$1$1b".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("axyzxyzb", filledPlaceholders));
        
    }
    
    @Test
    public void testRepeatedPlaceHoldersAtEnd() throws Exception {
        char[] chars = "a$1$1".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("axyzxyz", filledPlaceholders));
        
    }
    
    @Test
    public void testRepeatedPlaceHoldersAtBeginning() throws Exception {
        char[] chars = "$1$1a".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("xyzxyza", filledPlaceholders));
        
    }
    
    @Test
    public void testRepeatedPlaceHoldersOnly() throws Exception {
        char[] chars = "$1$1".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("xyzxyz", filledPlaceholders));
        
    }
    
    @Test
    public void testSamePlaceHolderWithOtherInMiddle() throws Exception {
        char[] chars = "a$1b$1c".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("axyzbxyzc", filledPlaceholders));
    }
    
    @Test
    public void testSamePlaceHolderAtBordersWithOtherInMiddle() throws Exception {
        char[] chars = "$1b$1".toCharArray();
        Term outputTerm = new Term(chars, 0, chars.length, null);
        ComparableCharSequence filledPlaceholders = outputTerm.fillPlaceholders(new TermMatches(new TermMatch(new querqy.model.Term(null, "klxyz"), true, 
                new SimpleComparableCharSequence("xyz".toCharArray(), 0, 3) )));
        assertTrue(CharSequenceUtil.equals("xyzbxyz", filledPlaceholders));
    }
    
    @Test
    public void testMultiplePlaceHolders() throws Exception {
        char[] chars = "a$3$$5b$1".toCharArray();
        Term term = new Term(chars, 0, chars.length, null);
        assertEquals(5, term.getMaxPlaceHolderRef());
        assertThat(term.getPlaceHolders(), 
                contains(
                        new PlaceHolder(4, 2, 5),
                        new PlaceHolder(1, 2, 3),
                        new PlaceHolder(7, 2, 1)
                        
                        ));
        
       
    }

}
