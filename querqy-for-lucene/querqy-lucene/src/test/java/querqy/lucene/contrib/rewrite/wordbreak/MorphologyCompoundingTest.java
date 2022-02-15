package querqy.lucene.contrib.rewrite.wordbreak;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static querqy.lucene.contrib.rewrite.wordbreak.GermanDecompoundingMorphology.GENERATOR_NOOP;

public class MorphologyCompoundingTest {

    private final SuffixGroup suffixGroup = Mockito.mock(SuffixGroup.class);
    private final Morphology morphology = new SuffixGroupMorphology(aFloat ->
            new SuffixGroup(null, Collections.singletonList(new WordGeneratorAndWeight(GENERATOR_NOOP, 0)))
            , aFloat -> suffixGroup
    );
    private final String left = "leftWord";
    private final String right = "rightWord";

    @Test
    public void delegateMorphologyGenerationToSuffixGroup() {
        when(suffixGroup.generateCompoundSuggestions(left, right))
                .thenReturn(Collections.singletonList(
                        new Suggestion(new CharSequence[]{"compound1"}, 1.1f)));
        final Compound[] compounds = morphology.suggestCompounds(left, right);

        assertThat(Arrays.asList(compounds), hasSize(1));
        assertThat(compounds[0].compound, is("compound1"));
        assertThat(compounds[0].probability, is(1.1f));
        assertThat(compounds[0].terms[0], is(left));
        assertThat(compounds[0].terms[1], is(right));
    }

    @Test
    public void reduceDuplicatesProducedBySuffixGroup() {
        when(suffixGroup.generateCompoundSuggestions(left, right))
                .thenReturn(Arrays.asList(
                        new Suggestion(new CharSequence[]{"compound1"}, 1.1f),
                        new Suggestion(new CharSequence[]{"compound1"}, 1.1f))
                );
        final Compound[] compounds = morphology.suggestCompounds(left, right);

        assertThat(Arrays.asList(compounds), hasSize(1));
        assertThat(compounds[0].compound, is("compound1"));
        assertThat(compounds[0].probability, is(1.1f));

    }

    @Test
    public void produceMultipleCompounds() {
        when(suffixGroup.generateCompoundSuggestions(left, right))
                .thenReturn(Arrays.asList(
                        new Suggestion(new CharSequence[]{"compound1"}, 1.1f),
                        new Suggestion(new CharSequence[]{"compound2"}, 1.1f))
                );
        final Compound[] compounds = morphology.suggestCompounds(left, right);

        assertThat(Arrays.asList(compounds), hasSize(2));
        assertThat(compounds[0].compound, is("compound1"));
        assertThat(compounds[1].compound, is("compound2"));

    }
}