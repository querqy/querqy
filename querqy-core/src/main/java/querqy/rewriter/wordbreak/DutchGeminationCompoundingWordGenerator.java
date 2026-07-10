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

import querqy.CompoundCharSequence;

import java.util.Optional;

/**
 * <p>The compounding-direction inverse of {@link DutchDegeminationWordGenerator}: when a modifier ends in a single
 * short vowel followed by a single doublable consonant, e.g. {@code pan}, {@code zon}, {@code pad}, the {@code
 * -e-}/{@code -en-} linker doubles that consonant to keep the vowel short once the syllable opens up, e.g.
 * {@code pan + en -> pannen}, {@code zon + e -> zonne}, {@code pad + en -> padden}.</p>
 *
 * <p>Only fires when the modifier's last two characters are a standalone short-vowel-plus-consonant nucleus &mdash;
 * the same guard as {@link DutchVowelLengtheningWordGenerator}'s digraph check, so words already spelling a long
 * vowel with a double letter (e.g. {@code maan}) or ending in a digraph (e.g. {@code boek}) are left alone, since
 * those need vowel-shortening rather than gemination (see {@link DutchVowelShorteningCompoundingWordGenerator}).</p>
 */
public class DutchGeminationCompoundingWordGenerator implements WordGenerator {

    private static final String DOUBLABLE_CONSONANTS = "bdfgklmnprst";
    private static final String VOWELS = "aeiou";

    private final CharSequence suffix;

    public DutchGeminationCompoundingWordGenerator(final CharSequence suffix) {
        this.suffix = suffix;
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence modifier) {
        final int length = modifier.length();
        if (length < 2) {
            return Optional.empty();
        }

        final char finalConsonant = modifier.charAt(length - 1);
        if (DOUBLABLE_CONSONANTS.indexOf(finalConsonant) < 0) {
            return Optional.empty();
        }

        final char vowel = modifier.charAt(length - 2);
        if (VOWELS.indexOf(vowel) < 0) {
            return Optional.empty();
        }

        if (length > 2 && VOWELS.indexOf(modifier.charAt(length - 3)) >= 0) {
            return Optional.empty();
        }

        return Optional.of(new CompoundCharSequence(null, modifier, String.valueOf(finalConsonant), suffix));
    }
}
