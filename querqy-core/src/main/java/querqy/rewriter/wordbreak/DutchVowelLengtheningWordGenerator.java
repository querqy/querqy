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
 * <p>Reverses the Dutch open-syllable vowel shortening that the {@code -e-}/{@code -en-} linker triggers on the
 * modifier: resyllabification into an open syllable drops one letter of a long vowel's double spelling, e.g.
 * {@code schapenvlees} (schaap + en + vlees, schaap &rarr; schap). Stripping the linker leaves {@code schap}, and
 * this generator doubles the vowel back to recover the base form {@code schaap}.</p>
 *
 * <p>Only {@code a, e, o, u} alternate this way ({@code aa/a}, {@code ee/e}, {@code oo/o}, {@code uu/u}); {@code i}
 * is excluded because Dutch spells long {@code i} as the digraph {@code ie}, which does not double. A vowel that is
 * itself preceded by another vowel is also excluded, since that signals a digraph ({@code oe}, {@code ui}, ...)
 * rather than a standalone short-vowel nucleus, e.g. {@code boek} must not become {@code boeek}.</p>
 */
public class DutchVowelLengtheningWordGenerator implements WordGenerator {

    private static final String LENGTHENABLE_VOWELS = "aeou";
    private static final String VOWELS = "aeiou";

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        final int length = reducedModifier.length();
        if (length < 2) {
            return Optional.empty();
        }

        final char finalChar = reducedModifier.charAt(length - 1);
        if (VOWELS.indexOf(finalChar) >= 0) {
            return Optional.empty();
        }

        final char vowel = reducedModifier.charAt(length - 2);
        if (LENGTHENABLE_VOWELS.indexOf(vowel) < 0) {
            return Optional.empty();
        }

        if (length > 2 && VOWELS.indexOf(reducedModifier.charAt(length - 3)) >= 0) {
            return Optional.empty();
        }

        return Optional.of(new CompoundCharSequence(null,
                reducedModifier.subSequence(0, length - 1),
                String.valueOf(vowel),
                reducedModifier.subSequence(length - 1, length)));
    }
}
