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
import querqy.rewrite.contrib.wordbreak.*;


@RunWith(Parameterized.class)
public class GermanMorphologyCompoundingTableTest {
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("GERMAN").get();

    @Parameterized.Parameters(name = "Test {index}: Term({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"kohl", "suppe", "kohlsuppe"},
                {"staat", "feind", "staatsfeind"},
                {"soziolog", "kongreß", "soziologenkongreß"},
                {"strauß", "ei", "straußenei"},
                {"wöchnerin", "heim", "wöchnerinnenheim"},
                {"aphorismus", "schatz", "aphorismenschatz"},
                {"museum", "verwaltung", "museenverwaltung"},
                {"aphrodisiakum", "verkäufer", "aphrodisiakaverkäufer"},
                {"kirche", "hof", "kirchhof"},
                {"madonna", "kult", "madonnenkult"},
                {"hund", "halter", "hundehalter"},
                {"gans", "klein", "gänseklein"},
                {"stadion", "verbot", "stadienverbot"},
                {"geist", "haltung", "geisteshaltung"},
                {"blatt", "wald", "blätterwald"},
                {"süden", "wind", "südwind"},
                {"pharmakon", "analyse", "pharmakaanalyse"},
                {"geist", "stunde", "geisterstunde"},
                {"prinzip", "reiter", "prinzipienreiter"},
                {"carabiniere", "schule", "carabinierischule"},
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
}
