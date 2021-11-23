package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class GermanMorphologyDecompoundingTest {
    private final Morphology.MorphologyProvider morphologyProvider = new Morphology.MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("GERMAN");

    @Test
    public void zeroMinBreakLengthProduce_splitsForEachChar() {
        final List<WordBreak> wordBreaks = morphology.suggestWordBreaks("germancompound", 0);
        assertThat(wordBreaks, hasSize(14));
    }

    @Test
    public void oneMinBreakLengthProduce_produceOneWordBreak() {
        final List<WordBreak> wordBreaks = morphology.suggestWordBreaks("ab", 1);
        assertThat(wordBreaks, hasSize(1));
        assertThat(wordBreaks.get(0).originalLeft, is("a"));
        assertThat(wordBreaks.get(0).originalRight, is("b"));
        final List<CharSequence[]> breakSuggestions = wordBreaks.get(0).suggestions.stream().map(breakSuggestion -> breakSuggestion.sequence).collect(Collectors.toList());
        assertThat(breakSuggestions, containsInAnyOrder(Arrays.asList(
                new CharSequence[]{"a"},
                new CharSequence[]{"ae"},
                new CharSequence[]{"aen"})
        ));
    }

    @Test
    public void minBreakLengthSameLengthAsTerm_produceNoSuggestions() {
        final List<WordBreak> wordBreaks = morphology.suggestWordBreaks("a", 1);
        assertThat(wordBreaks, hasSize(0));
    }

}
