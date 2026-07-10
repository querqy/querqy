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
 * <p>Reverses the Dutch final-obstruent devoicing that word-final position imposes on the dictionary form: Dutch
 * spells {@code f}/{@code s} word-finally but {@code v}/{@code z} when the same obstruent is no longer final, e.g.
 * {@code duivenhok} (duif + en + hok, duif &rarr; duiv) or a compound built on {@code huis} (huis + en, huis
 * &rarr; huiz-, e.g. {@code huizenblok}). Stripping the linker leaves {@code duiv}/{@code huiz}, and this generator
 * devoices the final consonant back to recover the base form {@code duif}/{@code huis}.</p>
 *
 * <p>Unlike {@link DutchDegeminationWordGenerator} and {@link DutchVowelLengtheningWordGenerator}, this alternation
 * is a plain final-letter swap with no digraph or gemination ambiguity to guard against.</p>
 */
public class DutchDevoicingWordGenerator implements WordGenerator {

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        final int length = reducedModifier.length();
        if (length == 0) {
            return Optional.empty();
        }

        final char last = reducedModifier.charAt(length - 1);
        final String devoiced;
        switch (last) {
            case 'v':
                devoiced = "f";
                break;
            case 'z':
                devoiced = "s";
                break;
            default:
                return Optional.empty();
        }

        return Optional.of(new CompoundCharSequence(null,
                reducedModifier.subSequence(0, length - 1), devoiced));
    }
}
