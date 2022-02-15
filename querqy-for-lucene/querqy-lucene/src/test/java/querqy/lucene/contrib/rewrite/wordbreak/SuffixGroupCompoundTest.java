package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class SuffixGroupCompoundTest {
    private static final float WEIGHT_PATTERN = 0f;

    @Test
    public void nullSuffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1", "word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1word2"}));
    }

    @Test
    public void nullSuffix_Plus_S_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("s"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1_s_word2"}));
    }

    @Test
    public void en_Suffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("en",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_en", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1__word2"}));
    }

    @Test
    public void um_Suffix_en_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("um",
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("en"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1_um", "_word2");
        assertThat(breakSuggestions, hasSize(1));
        assertThat(breakSuggestions.get(0).sequence, is(new CharSequence[]{"word1_en_word2"}));
    }

    @Test
    public void noSuggestions_whenRemovableSuffixLargerThanWord() {
        final SuffixGroup suffixGroup = new SuffixGroup("big_suffix",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> breakSuggestions = suffixGroup.generateCompoundSuggestions("word1", "word2");
        assertThat(breakSuggestions, hasSize(0));
    }

}