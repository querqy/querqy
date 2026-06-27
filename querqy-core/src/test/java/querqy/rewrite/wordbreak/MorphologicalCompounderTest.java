/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewrite.contrib.wordbreak;


import org.junit.Test;
import querqy.model.Term;
import querqy.rewrite.contrib.wordbreak.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static querqy.rewrite.contrib.wordbreak.Compounder.CompoundTerm;

public class MorphologicalCompounderTest {
    private final Morphology morphologyMock = mock(Morphology.class);
    private final int minSuggestionFrequency = 1;
    private final int maxCompoundExpansions = 10;
    private final MorphologicalCompounder compounder = new MorphologicalCompounder(morphologyMock, false, minSuggestionFrequency, maxCompoundExpansions);
    private final TermCorpus termCorpus = mock(TermCorpus.class);
    private final Term leftTerm = new Term(null, "field1", "w1");
    private final Term rightTerm = new Term(null, "field1", "w2");

    @Test
    public void emptyCollectionWhenLessThan2Terms() throws Exception {

        final List<CompoundTerm> combine = compounder.combine(new Term[]{}, termCorpus, false);

        assertThat(combine, hasSize(0));
        verifyNoInteractions(termCorpus);
    }

    @Test
    public void returnCompoundTerm() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(termCorpus.docFreq(eq("w1w2"))).thenReturn(minSuggestionFrequency);

        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                }, termCorpus, false);

        assertThat(combine, hasSize(1));
    }


    @Test
    public void compoundFirstTwoTermsOnly() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(termCorpus.docFreq("w1w2")).thenReturn(1);
        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, termCorpus, false);

        verify(termCorpus).docFreq("w1w2");

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
                }, termCorpus, false);

        verify(termCorpus, times(maxCompoundExpansions)).docFreq(any());
    }

    @Test
    public void filterCompoundTermWithMinimumFrequency() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(termCorpus.docFreq(eq("w1w2"))).thenReturn(0);

        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                }, termCorpus, false);

        assertThat(combine, hasSize(0));
    }

    @Test(expected = UncheckedIOException.class)
    public void corpusFailsRethrowUnchecked() throws Exception {
        when(morphologyMock.suggestCompounds(leftTerm, rightTerm)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(termCorpus.docFreq(any())).thenThrow(new UncheckedIOException(new IOException("test")));
        compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, termCorpus, false);

    }

    @Test
    public void returnCompoundTermWithLowerCaseInput() throws Exception {
        final Term leftTermUc = new Term(null, "field1", "W1");
        final Term rightTermUc = new Term(null, "field1", "W2");

        final Term leftTermLc = new Term(null, "field1", "w1");
        final Term rightTermLc = new Term(null, "field1", "w2");

        final MorphologicalCompounder lcCompounder = new MorphologicalCompounder(morphologyMock, true, minSuggestionFrequency, maxCompoundExpansions);


        when(morphologyMock.suggestCompounds(leftTermLc, rightTermLc)).thenReturn(new Compound[]{
                new Compound(new CharSequence[]{"w1", "w2"},
                        "w1w2",
                        1f
                )});
        when(termCorpus.docFreq(eq("w1w2"))).thenReturn(minSuggestionFrequency);

        final List<CompoundTerm> combine = lcCompounder.combine(
                new Term[]{
                        leftTermUc,
                        rightTermUc,
                }, termCorpus, false);

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
        when(termCorpus.docFreq(any())).thenReturn(1);
        final List<CompoundTerm> combine = compounder.combine(
                new Term[]{
                        leftTerm,
                        rightTerm,
                        new Term(null, "field1", "w3"),
                }, termCorpus, false);

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
