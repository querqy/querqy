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
 * <p>The compounding-direction inverse of {@link DutchVowelLengtheningWordGenerator}: when a modifier ends in a
 * double-spelled long vowel followed by a single consonant, e.g. {@code schaap}, {@code maan}, {@code peer}, the
 * {@code -e-}/{@code -en-} linker drops one copy of the vowel once the syllable opens up, e.g.
 * {@code schaap + en -> schapen}, {@code maan + en -> manen}, {@code peer + en -> peren}.</p>
 *
 * <p>Only {@code a, e, o, u} alternate this way, matching {@link DutchVowelLengtheningWordGenerator}; {@code i} is
 * excluded because Dutch spells long {@code i} as the digraph {@code ie}, which is not a doubled letter. Requiring
 * the two vowel letters to be identical (rather than just "a vowel") also means no separate digraph guard is needed
 * here: true digraphs ({@code oe}, {@code ui}, {@code ei}, ...) pair two different letters, so they never match.</p>
 */
public class DutchVowelShorteningCompoundingWordGenerator implements WordGenerator {

    private static final String SHORTENABLE_VOWELS = "aeou";
    private static final String VOWELS = "aeiou";

    private final CharSequence suffix;

    public DutchVowelShorteningCompoundingWordGenerator(final CharSequence suffix) {
        this.suffix = suffix;
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence modifier) {
        final int length = modifier.length();
        if (length < 3) {
            return Optional.empty();
        }

        final char finalChar = modifier.charAt(length - 1);
        if (VOWELS.indexOf(finalChar) >= 0) {
            return Optional.empty();
        }

        final char vowel1 = modifier.charAt(length - 2);
        final char vowel2 = modifier.charAt(length - 3);
        if (vowel1 != vowel2 || SHORTENABLE_VOWELS.indexOf(vowel1) < 0) {
            return Optional.empty();
        }

        return Optional.of(new CompoundCharSequence(null,
                modifier.subSequence(0, length - 2), String.valueOf(finalChar), suffix));
    }
}
