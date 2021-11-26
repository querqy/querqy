package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;


@RunWith(Parameterized.class)
public class GermanMorphologyCompoundingTableTest {
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("GERMAN");

    @Parameterized.Parameters(name = "Test {index}: Term({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"kohl", "supper",
                        suggest("kohlsupper")},
                {"staat", "feind",
                        suggest("staatsfeind")},
                {"soziolog", "kongreß",
                        suggest("soziologenkongreß")},
                {"strauß", "ei",
                        suggest("straußenei")},
                {"wöchnerin", "heim",
                        suggest("wöchnerinnenheim")},
                {"aphorismus", "schatz",
                        suggest("aphorismenschatz")},
                {"museum", "verwaltung",
                        suggest("museenverwaltung")},
                {"aphrodisiakum", "verkäufer",
                        suggest("aphrodisiakaverkäufer")},
                {"kirche", "hof",
                        suggest("kirchhof")},
                {"madonna", "kult",
                        suggest("madonnenkult")},
                {"hund", "halter",
                        suggest("hundehalter")},
                {"gans", "klein",
                        suggest("gänseklein")},
                {"stadion", "verbot",
                        suggest("stadienverbot")},
                {"geist", "haltung",
                        suggest("geisteshaltung")},
                {"blatt", "wald",
                        suggest("blätterwald")},
                {"süden", "wind",
                        suggest("südwind")},
                {"pharmakon", "analyse",
                        suggest("pharmakaanalyse")},
                {"geist", "stunde",
                        suggest("geisterstunde")},
                {"prinzip", "reiter",
                        suggest("prinzipienreiter")},
                {"carabiniere", "schule",
                        suggest("carabinierischule")},

        });
    }

    private final String leftTerm;
    private final String rightTerm;
    private final String expectedCompound;

    public GermanMorphologyCompoundingTableTest(final String leftTerm, final String rightTerm,
                                                final String expectedCompound) {
        this.leftTerm = leftTerm;
        this.rightTerm = rightTerm;
        this.expectedCompound = expectedCompound;
    }

    @Test
    public void compound() {
        final Compound[] compounds = morphology.suggestCompounds(leftTerm, rightTerm);

        final List<CharSequence> suggestedCompounds = Arrays.stream(compounds).map(c -> c.compound).collect(Collectors.toList());
        assertThat("No matching suggested compounds", suggestedCompounds, hasItem(expectedCompound));
    }

    static String suggest(final String expectedWordBreak) {
        return expectedWordBreak;
    }


}
