package querqy.solr.rewriter.wordbreak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_ALWAYS_ADD_REVERSE_COMPOUNDS;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND_MAX_EXPANSIONS;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DECOMPOUND_VERIFY_COLLATION;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_DICTIONARY_FIELD;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_LOWER_CASE_INPUT;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MAX_COMBINE_WORD_LENGTH;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MIN_BREAK_LENGTH;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MIN_SUGGESTION_FREQ;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_MORPHOLOGY;
import static querqy.solr.rewriter.wordbreak.WordBreakCompoundRewriterFactory.CONF_REVERSE_COMPOUND_TRIGGER_WORDS;

import org.junit.Test;
import querqy.lucene.contrib.rewrite.wordbreak.MorphologyImpl;

import java.util.List;
import java.util.Map;

public class WordBreakCompoundConfigRequestBuilderTest {

    @Test
    public void testThatDictionaryFieldMustBeSet() {

        try {
            new WordBreakCompoundConfigRequestBuilder().buildConfig();
            fail("dictionaryField==null must not be allowed");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(CONF_DICTIONARY_FIELD));
        }

        try {
            new WordBreakCompoundConfigRequestBuilder().dictionaryField(null);
            fail("dictionaryField==null must not be allowed");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(CONF_DICTIONARY_FIELD));
        }

        try {
            new WordBreakCompoundConfigRequestBuilder().dictionaryField("");
            fail("empty dictionaryField must not be allowed");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(CONF_DICTIONARY_FIELD));
        }

    }

    @Test
    public void testThatMinBreakMustBeGreaterOrEqual1() {
        try {
            new WordBreakCompoundConfigRequestBuilder().dictionaryField("f1").minBreakLength(0);
            fail("minBreakLength<1 must not be allowed");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("minBreakLength"));
        }
    }

    @Test
    public void testMinimalConfig() {
        final Map<String, Object> config = new WordBreakCompoundConfigRequestBuilder().dictionaryField("f1")
                .buildConfig();
        final List<String> errors = new WordBreakCompoundRewriterFactory("id").validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());
    }

    @Test
    public void testSetAllProperties() {
        final Map<String, Object> config = new WordBreakCompoundConfigRequestBuilder()
                .dictionaryField("f1")
                .minBreakLength(1)
                .alwaysAddReverseCompounds(true)
                .lowerCaseInput(false)
                .maxCombineWordLength(10)
                .maxDecompoundExpansions(4)
                .minSuggestionFrequency(2)
                .morphology(MorphologyImpl.GERMAN)
                .reverseCompoundTriggerWords("from", "of")
                .verifyDecompoundCollation(false)
                .buildConfig();

        final List<String> errors = new WordBreakCompoundRewriterFactory("id").validateConfiguration(config);
        assertTrue(errors == null || errors.isEmpty());

        assertThat(config, hasEntry(CONF_DICTIONARY_FIELD, "f1"));
        assertThat(config, hasEntry(CONF_MIN_BREAK_LENGTH, 1));
        assertThat(config, hasEntry(CONF_ALWAYS_ADD_REVERSE_COMPOUNDS, Boolean.TRUE));
        assertThat(config, hasEntry(CONF_LOWER_CASE_INPUT, Boolean.FALSE));
        assertThat(config, hasEntry(CONF_MAX_COMBINE_WORD_LENGTH, 10));
        assertThat(config, hasEntry(CONF_MIN_SUGGESTION_FREQ, 2));
        assertThat(config, hasEntry(CONF_MORPHOLOGY, MorphologyImpl.GERMAN.name()));

        final Map<String, Object> decompound = (Map<String, Object>) config.get(CONF_DECOMPOUND);

        assertThat(decompound, hasEntry(CONF_DECOMPOUND_VERIFY_COLLATION, Boolean.FALSE));
        assertThat(decompound, hasEntry(CONF_DECOMPOUND_MAX_EXPANSIONS, 4));

        final List<String> reverseCompoundTriggerWords = (List<String>) config.get(CONF_REVERSE_COMPOUND_TRIGGER_WORDS);

        assertThat(reverseCompoundTriggerWords, contains("from", "of"));


    }

}