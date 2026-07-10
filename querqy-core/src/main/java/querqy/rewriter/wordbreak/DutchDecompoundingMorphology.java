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

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * <p>Morphological compounding patterns for Dutch. Dutch has a much smaller linker inventory than German
 * ({@code ∅, -s-, -e-, -en-, -er-}), but the schwa linkers ({@code -e-}, {@code -en-}) trigger orthographic
 * alternations on the modifier (degemination, vowel lengthening, final devoicing) that must be reversed to recover
 * the dictionary form.</p>
 *
 * <p>Also see {@link SuffixGroup} and {@link WordGenerator}.</p>
 */
public abstract class DutchDecompoundingMorphology {

    /**
     * <p>Placeholder priors: they only encode a rough ordering of linker frequency (zero linker &gt; -s- &gt; -en-
     * &gt; -e- &gt; -er-) and must be calibrated against real Dutch data (e.g. CELEX or our own index) before the
     * ranking can be trusted. Since every candidate is validated against {@code TermCorpus}, miscalibration only
     * affects suggestion order, never correctness.</p>
     */
    static final float PRIOR_0 = 1f;
    static final float PRIOR_PLUS_S = 0.6f;
    static final float PRIOR_PLUS_E = 0.05f;
    static final float PRIOR_PLUS_EN = 0.3f;
    static final float PRIOR_PLUS_ER = 0.02f;

    public static final WordGenerator GENERATOR_NOOP = NoopWordGenerator.INSTANCE;
    static final WordGenerator GENERATOR_S = new SuffixWordGenerator("s");
    static final WordGenerator GENERATOR_E = new SuffixWordGenerator("e");
    static final WordGenerator GENERATOR_EN = new SuffixWordGenerator("en");
    static final WordGenerator GENERATOR_ER = new SuffixWordGenerator("er");
    static final WordGenerator GENERATOR_DEGEMINATION = new DutchDegeminationWordGenerator();
    static final WordGenerator GENERATOR_LENGTHEN = new DutchVowelLengtheningWordGenerator();
    static final WordGenerator GENERATOR_DEVOICE = new DutchDevoicingWordGenerator();
    static final WordGenerator GENERATOR_LENGTHEN_DEVOICE = new DutchLengtheningDevoicingWordGenerator();
    static final WordGenerator GENERATOR_GEMINATE_E = new DutchGeminationCompoundingWordGenerator("e");
    static final WordGenerator GENERATOR_GEMINATE_EN = new DutchGeminationCompoundingWordGenerator("en");
    static final WordGenerator GENERATOR_SHORTEN_E = new DutchVowelShorteningCompoundingWordGenerator("e");
    static final WordGenerator GENERATOR_SHORTEN_EN = new DutchVowelShorteningCompoundingWordGenerator("en");
    static final WordGenerator GENERATOR_VOICE_E = new DutchVoicingCompoundingWordGenerator("e");
    static final WordGenerator GENERATOR_VOICE_EN = new DutchVoicingCompoundingWordGenerator("en");
    static final WordGenerator GENERATOR_SHORTEN_VOICE_E = new DutchShorteningVoicingCompoundingWordGenerator("e");
    static final WordGenerator GENERATOR_SHORTEN_VOICE_EN = new DutchShorteningVoicingCompoundingWordGenerator("en");

    /**
     * <p>Plain {@code +e}/{@code +en} ({@code pan + en -> panen}) sits alongside the gemination, vowel-shortening,
     * voicing, and combined shortening+voicing alternatives (e.g. {@code pan + en -> pannen}, {@code schaap + en ->
     * schapen}, {@code duif + en -> duiven}, {@code slaaf + en -> slaven}) rather than replacing it, since most
     * modifiers don't need any alternation at all (e.g. {@code post + en -> posten}); {@code TermCorpus}
     * disambiguates between the candidates, same as on the decompounding side.</p>
     */
    public static SuffixGroup createCompoundingMorphemes(final float weight) {
        return new SuffixGroup(null,
                asList(
                        // 0
                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_0, weight)),
                        // +s
                        new WordGeneratorAndWeight(GENERATOR_S, (float) Math.pow(PRIOR_PLUS_S, weight)),
                        // +e
                        new WordGeneratorAndWeight(GENERATOR_E, (float) Math.pow(PRIOR_PLUS_E, weight)),
                        // consonant doubled +e
                        new WordGeneratorAndWeight(GENERATOR_GEMINATE_E, (float) Math.pow(PRIOR_PLUS_E, weight)),
                        // vowel shortened +e
                        new WordGeneratorAndWeight(GENERATOR_SHORTEN_E, (float) Math.pow(PRIOR_PLUS_E, weight)),
                        // final consonant voiced +e
                        new WordGeneratorAndWeight(GENERATOR_VOICE_E, (float) Math.pow(PRIOR_PLUS_E, weight)),
                        // vowel shortened + final consonant voiced +e
                        new WordGeneratorAndWeight(GENERATOR_SHORTEN_VOICE_E, (float) Math.pow(PRIOR_PLUS_E, weight)),
                        // +en
                        new WordGeneratorAndWeight(GENERATOR_EN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                        // consonant doubled +en
                        new WordGeneratorAndWeight(GENERATOR_GEMINATE_EN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                        // vowel shortened +en
                        new WordGeneratorAndWeight(GENERATOR_SHORTEN_EN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                        // final consonant voiced +en
                        new WordGeneratorAndWeight(GENERATOR_VOICE_EN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                        // vowel shortened + final consonant voiced +en
                        new WordGeneratorAndWeight(GENERATOR_SHORTEN_VOICE_EN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                        // +er
                        new WordGeneratorAndWeight(GENERATOR_ER, (float) Math.pow(PRIOR_PLUS_ER, weight))
                )
        );
    }

    public static SuffixGroup createDecompoundingMorphemes(final float weight) {
        return new SuffixGroup(null, // suffix to strip from the term, when null then no suffix removed

                singletonList(
                        // 0 (zero linker)
                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_0, weight))
                ),

                // -s-
                new SuffixGroup("s",
                        singletonList(
                                new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_PLUS_S, weight))
                        )
                ),

                // -e-, with its schwa-linker orthographic alternations
                new SuffixGroup("e",
                        asList(
                                new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_PLUS_E, weight)),
                                new WordGeneratorAndWeight(GENERATOR_DEGEMINATION, (float) Math.pow(PRIOR_PLUS_E, weight)),
                                new WordGeneratorAndWeight(GENERATOR_LENGTHEN, (float) Math.pow(PRIOR_PLUS_E, weight)),
                                new WordGeneratorAndWeight(GENERATOR_DEVOICE, (float) Math.pow(PRIOR_PLUS_E, weight)),
                                new WordGeneratorAndWeight(GENERATOR_LENGTHEN_DEVOICE, (float) Math.pow(PRIOR_PLUS_E, weight))
                        )
                ),

                // bare -n- is not a Dutch linker; it only exists to share the suffix lookup with -en-
                new SuffixGroup("n",
                        Collections.emptyList(),
                        // -en-, with its schwa-linker orthographic alternations
                        new SuffixGroup("en",
                                asList(
                                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                                        new WordGeneratorAndWeight(GENERATOR_DEGEMINATION, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                                        new WordGeneratorAndWeight(GENERATOR_LENGTHEN, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                                        new WordGeneratorAndWeight(GENERATOR_DEVOICE, (float) Math.pow(PRIOR_PLUS_EN, weight)),
                                        new WordGeneratorAndWeight(GENERATOR_LENGTHEN_DEVOICE, (float) Math.pow(PRIOR_PLUS_EN, weight))
                                )
                        )
                ),

                // bare -r- is not a Dutch linker; it only exists to share the suffix lookup with -er-
                new SuffixGroup("r",
                        Collections.emptyList(),
                        // -er- (archaic genitive/plural linker, e.g. kinderarts, rundergehakt)
                        new SuffixGroup("er",
                                singletonList(
                                        new WordGeneratorAndWeight(GENERATOR_NOOP, (float) Math.pow(PRIOR_PLUS_ER, weight))
                                )
                        )
                )
        );
    }
}
