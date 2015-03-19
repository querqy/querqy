package querqy.trie;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class TrieMapTest {

    @Test
    public void testThatEmptyMapAlwaysReturnsUnknownState() {
        TrieMap<Integer> map = new TrieMap<>();
        State<Integer> state = map.get("abc").getStateForCompleteSequence();
        assertNotNull(state);
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(-1, state.getIndex());
    }
    
    @Test
    public void testThatBlankLookupSequenceAlwaysReturnsUnknownState() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("").getStateForCompleteSequence();
        assertNotNull(state);
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(-1, state.getIndex());
    }
    
    @Test
    public void testThatSubsequenceOfEntryReturnsKnownAndNonFinalState() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("a").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(0, state.getIndex());
        
        state = map.get("ab").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(1, state.getIndex());
    }
    
    @Test
    public void testThatSequenceOfEntryReturnsKnownAndFinalStateAndValue() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("abc").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 1, state.getValue());
        assertEquals(2, state.getIndex());
        
    }
    
    @Test
    public void testThatOverlappingSequencesMatchCorrectly() {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        map.put("ab", 2);
        
        State<Integer> state = map.get("abc").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 1, state.getValue());
        assertEquals(2, state.getIndex());
        
        state = map.get("ab").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 2, state.getValue());
        assertEquals(1, state.getIndex());
    }
    
    @Test
    public void testResumingFromKnwonNonFinalState() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        
        State<Integer> state = map.get("ab").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(1, state.getIndex());
        
        State<Integer> state2 = map.get("c", state).getStateForCompleteSequence();
        assertNotNull(state2);
        assertTrue(state2.isKnown());
        assertTrue(state2.isFinal());
        assertEquals((Integer) 1, state2.getValue());
        assertEquals(0, state2.getIndex());
    }
    
    @Test
    public void testResumingFromKnwonAndFinalState() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        map.put("ab", 2);
        
        State<Integer> state = map.get("ab").getStateForCompleteSequence();
        assertNotNull(state);
        assertTrue(state.isKnown());
        assertTrue(state.isFinal());
        assertEquals((Integer) 2, state.getValue());
        assertEquals(1, state.getIndex());
        
        State<Integer> state2 = map.get("c", state).getStateForCompleteSequence();
        assertNotNull(state2);
        assertTrue(state2.isKnown());
        assertTrue(state2.isFinal());
        assertEquals((Integer) 1, state2.getValue());
        assertEquals(0, state2.getIndex());
    }
    
    @Test
    public void testThatUnknownSequenceReturnsUnknownAndNonFinal() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("ak").getStateForCompleteSequence();
        assertNotNull(state);
        assertFalse(state.isKnown());
        assertFalse(state.isFinal());
        assertEquals(-1, state.getIndex());
    }
    
    @Test
    public void testThatResumingFromUnknownStateThrowsException() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.put("abc", 1);
        State<Integer> state = map.get("k").getStateForCompleteSequence();
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
    
    @Test
    public void testGetPrefixIfAloneInMap() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.putPrefix("a", 1);
        States<Integer> states = map.get("ab");
        State<Integer> completeSequenceState = states.getStateForCompleteSequence();
        assertFalse(completeSequenceState.isFinal());
        assertFalse(completeSequenceState.isKnown());
        List<State<Integer>> prefixes = states.getPrefixes();
        assertNotNull(prefixes);
        assertThat(prefixes, contains(state(true, true, 0, 1)));
    }
    
    @Test
    public void testGetPrefixAsSubstringOfOther() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.putPrefix("a", 1);
        map.put("abc", 2);
        States<Integer> states = map.get("ab");
        State<Integer> completeSequenceState = states.getStateForCompleteSequence();
        assertFalse(completeSequenceState.isFinal());
        assertTrue(completeSequenceState.isKnown());
        List<State<Integer>> prefixes = states.getPrefixes();
        assertNotNull(prefixes);
        assertThat(prefixes, contains(state(true, true, 0, 1)));
    }
    
    @Test
    public void testGetPrefixWithLongerCompleteSequence() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.putPrefix("ab", 1);
        map.put("abc", 2);
        States<Integer> states = map.get("abc");
        State<Integer> completeSequenceState = states.getStateForCompleteSequence();
        assertThat(completeSequenceState, state(true, true, 2, 2));
        
        List<State<Integer>> prefixes = states.getPrefixes();
        assertNotNull(prefixes);
        assertThat(prefixes, contains(state(true, true, 1, 1)));
        
    }
    
    @Test
    public void testGetPrefixWithOtherOfSameLength() throws Exception {
        TrieMap<Integer> map = new TrieMap<>();
        map.putPrefix("ab", 1);
        map.put("ab", 2);
        States<Integer> states = map.get("ab");
        State<Integer> completeSequenceState = states.getStateForCompleteSequence();
        assertThat(completeSequenceState, state(true, true, 1, 2));
        
        List<State<Integer>> prefixes = states.getPrefixes();
        assertNull(prefixes);
    }
    
    public static <T> StateMatcher<T> state(boolean isKnown, boolean isFinal, int index, T value) {
        return new StateMatcher<T>(isKnown, isFinal, index, value);
    }

    public static class StateMatcher<T> extends TypeSafeMatcher<State<T>> {
        
        final boolean isKnown;
        final boolean isFinal;
        final int index;
        final T value;
        
        public StateMatcher(boolean isKnown, boolean isFinal, int index, T value) {
            this.isKnown = isKnown;
            this.isFinal = isFinal;
            this.index = index;
            this.value = value;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("isKnown=" + isKnown + ", isFinal=" + isFinal
                    + ", index=" + index + ", value=" + value );
        }

        @Override
        protected boolean matchesSafely(State<T> item) {
            if (index != item.getIndex())
                return false;
            if (isFinal != item.isFinal())
                return false;
            if (isKnown != item.isKnown())
                return false;
            if (value == null) {
                if (item.getValue() != null)
                    return false;
            } else if (!value.equals(item.getValue()))
                return false;
            return true;
                    
        }
        
    }

}
