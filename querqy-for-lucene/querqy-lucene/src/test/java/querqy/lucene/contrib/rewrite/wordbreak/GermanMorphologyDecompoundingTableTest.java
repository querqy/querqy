package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;


@RunWith(Parameterized.class)
public class GermanMorphologyDecompoundingTableTest {
    public static final int MIN_BREAK_LENGTH = 2;
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("GERMAN").get();

    @Parameterized.Parameters(name = "Test {index}: Term({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"kohlsuppe", wb("kohl", "suppe", "kohl")},
                {"staatsfeind", wb("staats", "feind", "staat")},
                {"soziologenkongreß", wb("soziologen", "kongreß", "soziologe")},
                {"straußenei", wb("straußen", "ei", "strauß")},
                {"wöchnerinnenheim", wb("wöchnerinnen", "heim", "wöchnerin")},
                {"aphorismenschatz", wb("aphorismen", "schatz", "aphorismus")},
                {"museenverwaltung", wb("museen", "verwaltung", "museum")},
                {"aphrodisiakaverkäufer", wb("aphrodisiaka", "verkäufer", "aphrodisiakum")},
                {"kirchhof", wb("kirch", "hof", "kirche")},
                {"madonnenkult", wb("madonnen", "kult", "madonna")},
                {"hundehalter", wb("hunde", "halter", "hund")},
                {"gänseklein", wb("gänse", "klein", "gans")},
                {"stadienverbot", wb("stadien", "verbot", "stadion")},
                {"geisteshaltung", wb("geistes", "haltung", "geist")},
                {"blätterwald", wb("blätter", "wald", "blatt")},
                {"südwind", wb("süd", "wind", "süden")},
                {"pharmakaanalyse", wb("pharmaka", "analyse", "pharmakon")},
                {"geisterstunde", wb("geister", "stunde", "geist")},
                {"prinzipienreiter", wb("prinzipien", "reiter", "prinzip")},
                {"carabinierischule", wb("carabinieri", "schule", "carabiniere")},

        });
    }

    private final String inputWord;
    private final ExpectedWordBreak expectedWordBreak;

    public GermanMorphologyDecompoundingTableTest(final String inputWord,
                                                  final ExpectedWordBreak expectedWordBreak
    ) {
        this.inputWord = inputWord;
        this.expectedWordBreak = expectedWordBreak;
    }

    @Test
    public void decompound() {
        final List<WordBreak> wordBreaks = morphology.suggestWordBreaks(inputWord, MIN_BREAK_LENGTH);

        final List<String> suggestedWordBreaks = wordBreaks.stream()
                .filter(wordBreak -> wordBreak.originalLeft.equals(expectedWordBreak.originalLeft))
                .filter(wordBreak -> wordBreak.originalRight.equals(expectedWordBreak.originalRight))
                .map(wordBreak -> wordBreak.suggestions.stream()
                        .map(breakSuggestion -> breakSuggestion.sequence[0])
                        .map(String::valueOf)
                        .collect(Collectors.toList())
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertThat(String.format("No matching word break for terms left=%s right%s",
                        expectedWordBreak.originalLeft, expectedWordBreak.originalRight),
                suggestedWordBreaks.size(), greaterThanOrEqualTo(1));
        assertThat("No matching suggested word breaks", suggestedWordBreaks, hasItem(expectedWordBreak.suggestion));
    }

    static ExpectedWordBreak wb(final String left, final String right, final String expectedWordBreak) {
        return new ExpectedWordBreak(left, right, expectedWordBreak);
    }

    static class ExpectedWordBreak {
        private final String originalLeft;
        private final String originalRight;
        private final String suggestion;

        ExpectedWordBreak(final String originalLeft, final String originalRight, final String suggestion) {
            this.originalLeft = originalLeft;
            this.originalRight = originalRight;
            this.suggestion = suggestion;
        }

        @Override
        public String toString() {
            return "ExpectedWordBreak{" +
                    "originalLeft='" + originalLeft + '\'' +
                    ", originalRight='" + originalRight + '\'' +
                    ", suggestions=" + suggestion +
                    '}';
        }
    }
}
