package querqy.lucene.contrib.rewrite.wordbreak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.lucene.index.Term;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.Collections;

public class WordBreakCompoundRewriterFactoryTest {

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputTrue() {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w1", () -> null,
                Morphology.DEFAULT, "field1", true, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        assertTrue(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputFalse() {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w2", () -> null,
                Morphology.GERMAN, "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        Assert.assertFalse(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("Word1").getStateForCompleteSequence().isFinal());
        assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());
    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseInsensitiveForFlagLowerCaseInputTrue() {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w1", () -> null,
                Morphology.DEFAULT, "field1", true, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.singletonList("Protected"));

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        Assert.assertTrue(protectedWords.get("protected").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseSensitiveForFlagLowerCaseInputFalse() {
        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w2", () -> null,
                Morphology.GERMAN, "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false,
                Collections.singletonList("Protected"));

        final TrieMap<Boolean> protectedWords = factory.getProtectedWords();
        Assert.assertTrue(protectedWords.get("Protected").getStateForCompleteSequence().isFinal());
        Assert.assertFalse(protectedWords.get("protected").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testLanguageMorphologyIsApplied() {

        Collector collector = mock(Collector.class);
        when(collector.collect(any(CharSequence.class), any(CharSequence.class), any(), anyInt(), anyFloat()))
                .thenReturn(Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED);

        ArgumentCaptor<String> leftCaptor = ArgumentCaptor.forClass(String.class);

        final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w2", () -> null,
                Morphology.GERMAN, "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        assertTrue(factory.wordBreaker instanceof MorphologicalWordBreaker);
        final SuffixGroup suffixGroup = ((MorphologicalWordBreaker) factory.wordBreaker).suffixGroup;

        final String left = "bücher";
        final String right = "right";
        final Term rightTerm = new Term("f1", "right");

        assertTrue(suffixGroup.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("buch", "büch", "bücher","büchere", "bücheren"));


    }
}
