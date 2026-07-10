/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.wordbreak;

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

/**
 * <p>Table test for {@link DutchDecompoundingMorphology} candidate generation only (as in
 * {@link GermanMorphologyDecompoundingTableTest}); no {@link TermCorpus} is consulted, so every split position for
 * a term is tried and each row only asserts that the expected suggestion appears among the candidates at its own
 * split point.</p>
 */
@RunWith(Parameterized.class)
public class DutchMorphologyDecompoundingTableTest {
    public static final int MIN_BREAK_LENGTH = 2;
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("dutch").get();

    @Parameterized.Parameters(name = "Test {index}: Term({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // zero linker
                {"postkantoor", wb("post", "kantoor", "post")},
                // -s-
                {"bevolkingsgroei", wb("bevolkings", "groei", "bevolking")},
                // -en-, no orthographic alternation
                {"boekenkast", wb("boeken", "kast", "boek")},
                // -e-, with degemination (issue #1045)
                {"zonnebril", wb("zonne", "bril", "zon")},
                // -en-, with degemination (issue #1045)
                {"paddenpoel", wb("padden", "poel", "pad")},
                // -e-, with vowel lengthening (older schwa spelling)
                {"pereboom", wb("pere", "boom", "peer")},
                // -en-, with vowel lengthening
                {"schapenvlees", wb("schapen", "vlees", "schaap")},
                // -en-, with devoicing (f/v alternation, issue #1045's own example)
                {"duivenhok", wb("duiven", "hok", "duif")},
                // -en-, with devoicing (s/z alternation)
                {"huizenblok", wb("huizen", "blok", "huis")},
                // -en-, with combined vowel lengthening + devoicing
                {"slavenhandel", wb("slaven", "handel", "slaaf")},
                {"gravenstraat", wb("graven", "straat", "graaf")},
                // -er- (archaic genitive/plural linker, issue #1045)
                {"kinderarts", wb("kinder", "arts", "kind")},
                {"rundergehakt", wb("runder", "gehakt", "rund")},
        });
    }

    private final String inputWord;
    private final ExpectedWordBreak expectedWordBreak;

    public DutchMorphologyDecompoundingTableTest(final String inputWord,
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

        assertThat(String.format("No matching word break for terms left=%s right=%s",
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
