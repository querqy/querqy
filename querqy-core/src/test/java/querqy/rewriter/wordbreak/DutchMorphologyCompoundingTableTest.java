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
import static org.hamcrest.Matchers.hasItem;

/**
 * <p>Table test for the compounding direction of {@link DutchDecompoundingMorphology}, in the style of
 * {@link GermanMorphologyCompoundingTableTest}. Covers the gemination, vowel-shortening, voicing and combined
 * shortening+voicing generators, and words where gemination must <em>not</em> fire (zero linker, digraph/long-vowel
 * modifiers).</p>
 */
@RunWith(Parameterized.class)
public class DutchMorphologyCompoundingTableTest {
    private final MorphologyProvider morphologyProvider = new MorphologyProvider();
    private final Morphology morphology = morphologyProvider.get("dutch").get();

    @Parameterized.Parameters(name = "Test {index}: Term({0})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // gemination (issue #1045)
                {"pad", "poel", "paddenpoel"},
                {"zon", "bril", "zonnebril"},
                {"kip", "soep", "kippensoep"},
                // zero linker, no gemination candidate applicable
                {"post", "kantoor", "postkantoor"},
                // -en-, digraph guard must prevent gemination
                {"boek", "kast", "boekenkast"},
                // long vowel already double-spelled; gemination must not misfire
                {"maan", "licht", "maanlicht"},
                // vowel shortening
                {"schaap", "vlees", "schapenvlees"},
                {"peer", "boom", "pereboom"},
                // voicing (f/v and s/z alternations)
                {"duif", "hok", "duivenhok"},
                {"huis", "blok", "huizenblok"},
                // combined vowel shortening + voicing
                {"slaaf", "handel", "slavenhandel"},
                {"graaf", "straat", "gravenstraat"},
        });
    }

    private final String leftTerm;
    private final String rightTerm;
    private final String expectedCompound;

    public DutchMorphologyCompoundingTableTest(final String leftTerm, final String rightTerm,
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
