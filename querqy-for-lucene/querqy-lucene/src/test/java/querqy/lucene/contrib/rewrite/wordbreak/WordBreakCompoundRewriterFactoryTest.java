package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Assert;
import org.junit.Test;
import querqy.trie.TrieMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static querqy.lucene.rewrite.TestUtil.addNumDocsWithTextField;

public class WordBreakCompoundRewriterFactoryTest extends LuceneTestCase {

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
    public void testLanguageMorphologyIsApplied() throws Exception {
        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);


        addNumDocsWithTextField("field1", "regal", indexWriter, 1);
        addNumDocsWithTextField("field1", "buch", indexWriter, 1);
        addNumDocsWithTextField("field1", "büch", indexWriter, 1);
        addNumDocsWithTextField("field1", "bücher", indexWriter, 1);
        addNumDocsWithTextField("field1", "büchere", indexWriter, 1);
        indexWriter.close();
        try (final IndexReader indexReader = DirectoryReader.open(directory)) {
            final WordBreakCompoundRewriterFactory factory = new WordBreakCompoundRewriterFactory("w2", () -> null,
                    Morphology.GERMAN, "field1", false, 1, 2, 1, Arrays.asList("Word1", "word2"), false, 2, false, Collections.emptyList());

            assertTrue(factory.wordBreaker instanceof MorphologicalWordBreaker);
            final String word = "bücherregal";

            assertThat(factory.wordBreaker.breakWord(word, indexReader, 10, false).stream()
                            .map(charSequences -> charSequences[0])
                            .map(CharSequence::toString)
                            .collect(Collectors.toList()),
                    containsInAnyOrder("buch", "büch", "bücher", "büchere"));


        }
    }
}
