package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class GermanMorphologyDecompoundingTest {
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("GERMAN").get();

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
        final List<String> breakSuggestions = wordBreaks.get(0).suggestions.stream()
                .map(breakSuggestion -> breakSuggestion.sequence)
                .flatMap(Stream::of)
                .map(CharSequence::toString)
                .collect(Collectors.toList());
        assertThat(breakSuggestions, containsInAnyOrder("a", "ae", "aen"));
    }

    @Test
    public void minBreakLengthSameLengthAsTerm_produceNoSuggestions() {
        final List<WordBreak> wordBreaks = morphology.suggestWordBreaks("a", 1);
        assertThat(wordBreaks, hasSize(0));
    }

    @Test
    public void rightTerm_minLengthCheck_singleSplitPoint() {
        final List<WordBreak> word = morphology.suggestWordBreaks("word", 2);
        assertThat(word, hasSize(1));
        assertThat(word.get(0).originalRight, is("rd"));
        assertThat(word.get(0).originalLeft, is("wo"));
    }

    @Test
    public void rightTerm_minLengthCheck_multipleSplitPoints() {
        final List<WordBreak> word = morphology.suggestWordBreaks("words", 2);

        assertThat(word, hasSize(2));
        assertThat(word.get(0).originalLeft, is("wor"));
        assertThat(word.get(0).originalRight, is("ds"));

        assertThat(word.get(1).originalLeft, is("wo"));
        assertThat(word.get(1).originalRight, is("rds"));
    }
}
