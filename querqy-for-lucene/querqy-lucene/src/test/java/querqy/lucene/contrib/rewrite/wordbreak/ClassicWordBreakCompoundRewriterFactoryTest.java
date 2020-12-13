package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Assert;
import org.junit.Test;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.Collections;

public class ClassicWordBreakCompoundRewriterFactoryTest {

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputTrue() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", true, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        Assert.assertTrue(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatTriggerWordsAreTurnedToLowerCaseForFlagLowerCaseInputFalse() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w2", () -> null,
                "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

        final TrieMap<Boolean> triggerWords = factory.getReverseCompoundTriggerWords();
        Assert.assertFalse(triggerWords.get("word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("Word1").getStateForCompleteSequence().isFinal());
        Assert.assertTrue(triggerWords.get("word2").getStateForCompleteSequence().isFinal());

    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseInsensitiveForFlagLowerCaseInputTrue() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", true, 1, 2, 1, Collections.emptyList(), false, 2, false,
                Collections.singletonList("Protected"));

        Assert.assertTrue(factory.getProtectedWords().contains("Protected"));
        Assert.assertTrue(factory.getProtectedWords().contains("protected"));

    }

    @Test
    public void testThatProtectedWordsAreMatchedCaseSensitiveForFlagLowerCaseInputFalse() {
        final ClassicWordBreakCompoundRewriterFactory factory = new ClassicWordBreakCompoundRewriterFactory("w1", () -> null,
                "field1", false, 1, 2, 1, Collections.emptyList(), false, 2, false,
                Collections.singletonList("Protected"));

        Assert.assertTrue(factory.getProtectedWords().contains("Protected"));
        Assert.assertFalse(factory.getProtectedWords().contains("protected"));

    }
}
