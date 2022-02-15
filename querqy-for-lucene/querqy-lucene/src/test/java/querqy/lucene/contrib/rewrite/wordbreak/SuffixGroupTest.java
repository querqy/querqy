package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class SuffixGroupTest {
    private static final float WEIGHT_PATTERN = 0f;

    @Test
    public void nullSuffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word1");
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word1"}));
    }

    @Test
    public void nullSuffix_Plus_E_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("e"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word_");
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word_e"}));
    }

    @Test
    public void minus_E_Suffix_noop_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("e",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word_e");
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word_"}));
    }

    @Test
    public void minus_E_Suffix_Plus_S_WordGenerator() {
        final SuffixGroup suffixGroup = new SuffixGroup("e",
                Collections.singletonList(
                        new WordGeneratorAndWeight(new SuffixWordGenerator("s"), WEIGHT_PATTERN)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word_e");
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word_s"}));
    }

    @Test
    public void noSuggestions_whenRemovableSuffixLargerThanWord() {
        final SuffixGroup suffixGroup = new SuffixGroup("big_suffix",
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word");
        assertThat(suggestions, hasSize(0));
    }

    @Test
    public void delegate_toNestedSuffixGroup() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, WEIGHT_PATTERN)
                ),
                new SuffixGroup("e",
                        Collections.singletonList(
                                new WordGeneratorAndWeight(new SuffixWordGenerator("s"), WEIGHT_PATTERN)
                        )
                )
        );

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word_e");

        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word_e"}));
        assertThat(suggestions.get(1).sequence, is(new CharSequence[]{"word_s"}));
    }

    @Test
    public void passWeightAsScore() {
        final SuffixGroup suffixGroup = new SuffixGroup(null,
                Collections.singletonList(
                        new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, 1.1f)
                ));

        final List<Suggestion> suggestions = suffixGroup.generateSuggestions("word");
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).sequence, is(new CharSequence[]{"word"}));
        assertThat(suggestions.get(0).score, is(1.1f));
    }
}