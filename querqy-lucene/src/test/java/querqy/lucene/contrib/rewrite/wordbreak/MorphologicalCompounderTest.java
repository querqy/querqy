package querqy.lucene.contrib.rewrite.wordbreak;


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.junit.Test;
import querqy.model.Term;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static querqy.lucene.LuceneQueryUtil.toLuceneTerm;
import static querqy.lucene.contrib.rewrite.wordbreak.LuceneCompounder.CompoundTerm;

public class MorphologicalCompounderTest {
    private final Morphology morphologyMock = mock(Morphology.class);
    private final int minSuggestionFrequency = 1;
    private final int maxCompoundExpansions = 10;
    private final MorphologicalCompounder compounder = new MorphologicalCompounder(morphologyMock, "field1", false, minSuggestionFrequency, maxCompoundExpansions);
    private final IndexReader indexReader = mock(LeafReader.class);
    private final Term leftTerm = new Term(null, "field1", "w1");
    private final Term rightTerm = new Term(null, "field1", "w2");

    @Test
    public void emptyCollectionWhenLessThan2Terms() {

        final List<CompoundTerm> combine = compounder.combine(new Term[]{}, indexReader, false);

        assertThat(combine, hasSize(0));
        verifyNoInteractions(indexReader);
    }

    @Test
    public void returnCompoundTerm() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(indexReader.docFreq(eq(toLuceneTerm("field1", "w1w2", false)))).thenReturn(minSuggestionFrequency);

        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                }, indexReader, false);

        assertThat(combine, hasSize(1));
    }


    @Test
    public void compoundFirstTwoTermsOnly() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(indexReader.docFreq(toLuceneTerm("field1", "w1w2", false))).thenReturn(1);
        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, indexReader, false);

        verify(indexReader).docFreq(toLuceneTerm("field1", "w1w2", false));

        assertThat(combine, hasSize(1));

    }

    @Test
    public void adhereToLimitOfIndexLookups() throws Exception {
        final Compound[] compounds = IntStream.range(0, 100).mapToObj(this::compoundFromIndex)
                .toArray(Compound[]::new);

        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(compounds);
        compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, indexReader, false);

        verify(indexReader, times(maxCompoundExpansions)).docFreq(any());
    }

    @Test
    public void filterCompoundTermWithMinimumFrequency() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(indexReader.docFreq(eq(toLuceneTerm("field1", "w1w2", false)))).thenReturn(0);

        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                }, indexReader, false);

        assertThat(combine, hasSize(0));
    }

    @Test(expected = UncheckedIOException.class)
    public void indexReaderFailsRethrowUnchecked() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(indexReader.docFreq(toLuceneTerm("field1", "w1w2", false))).thenThrow(IOException.class);
        compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, indexReader, false);

    }

    @Test
    public void returnCompoundTermWithLowerCaseInput() throws Exception {
        final Term leftTerm = new Term(null, "field1", "W1");
        final Term rightTerm = new Term(null, "field1", "W2");

        final Term leftTermLc = new Term(null, "field1", "w1");
        final Term rightTermLc = new Term(null, "field1", "w2");

        final MorphologicalCompounder compounder = new MorphologicalCompounder(morphologyMock, "field1", true, minSuggestionFrequency, maxCompoundExpansions);


        when(morphologyMock.suggestCompounds(leftTermLc, rightTermLc)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(indexReader.docFreq(eq(toLuceneTerm("field1", "w1w2", false)))).thenReturn(minSuggestionFrequency);

        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                }, indexReader, false);

        assertThat(combine, hasSize(1));
    }

    @Test
    public void resultingCompoundsOrderByScore() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"}, "thirdPrioCompound", 0.2f),
                new Compound(new CharSequence[]{"w1", "w2"}, "secondPrioCompound", 0.5f),
                new Compound(new CharSequence[]{"w1", "w2"}, "firstPrioCompound", 1f),
                new Compound(new CharSequence[]{"w1", "w2"}, "lowestPrioCompound", 0.1f),
        });
        when(indexReader.docFreq(any())).thenReturn(1);
        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, indexReader, false);

        assertThat(combine, hasSize(4));
        assertThat(combine.get(0).value, is("firstPrioCompound"));
        assertThat(combine.get(1).value, is("secondPrioCompound"));
        assertThat(combine.get(2).value, is("thirdPrioCompound"));
        assertThat(combine.get(3).value, is("lowestPrioCompound"));
    }

    private Compound compoundFromIndex(final int idx) {
        final String leftWord = "w" + idx;
        final String rightWord = "w" + idx + 1;
        final String compound = "w" + idx + "w" + idx + 1;
        return new Compound(new CharSequence[]{leftWord, rightWord}, compound, 1f);
    }

}
