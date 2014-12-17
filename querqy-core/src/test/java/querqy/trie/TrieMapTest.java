package querqy.trie;

import static org.junit.Assert.*;

import org.junit.Test;

public class TrieMapTest {

    @Test
    public void testThatEmptyMapAlwaysReturnsUnknownState() {
        TrieMap<Integer> map = new TrieMap<>();
        State<Integer> state = map.get("abc");
        assertNotNull(state);
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
    }
    
    @Test
    public void testThatBlankLookupSequenceAlwaysReturnsUnknownState() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("");
        assertNotNull(state);
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
    }
    
    @Test
    public void testThatSubsequenceOfEntryReturnsKnownAndNonFinalState() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("a");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
        
        state = map.get("ab");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
    }
    
    @Test
    public void testThatSequenceOfEntryReturnsKnownAndFinalStateAndValue() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("abc");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 1, state.getValue());
        
    }
    
    @Test
    public void testThatOverlappingSequencesMatchCorrectly() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        map.put("ab", 2);
        
        State<Integer> state = map.get("abc");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 1, state.getValue());
        
        state = map.get("ab");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 2, state.getValue());
    }
    
    @Test
    public void testResumingFromKnwonNonFinalState() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        
        State<Integer> state = map.get("ab");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
        
        State<Integer> state2 = map.get("c", state);
        assertNotNull(state2);
        assertTrue(state2.isKnown());
        assertTrue(state2.isFinal());
        assertEquals((Integer) 1, state2.getValue());
    }
    
    @Test
    public void testResumingFromKnwonAndFinalState() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        map.put("ab", 2);
        
        State<Integer> state = map.get("ab");
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 2, state.getValue());
        
        State<Integer> state2 = map.get("c", state);
        assertNotNull(state2);
        assertTrue(state2.isKnown());
        assertTrue(state2.isFinal());
        assertEquals((Integer) 1, state2.getValue());
    }
    
    @Test
    public void testThatResumingFromUnknownStateThrowsException() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("k");
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
        try {
            map.get("abc", state);
            fail("get() must not resume from unknown state");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testThatMapDoesNotAcceptEmptySequence() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        try {
            map.put("", 1);
            fail("put() must not accept an empty char sequence");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }


}
