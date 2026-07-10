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

import java.util.Optional;

/**
 * <p>Reverses the Dutch consonant gemination that the {@code -e-}/{@code -en-} linker triggers on the modifier: to
 * keep the preceding vowel short, the linker doubles the modifier's final consonant, e.g. {@code zonnebril}
 * (zon + e + bril, zon &rarr; zonn) or {@code paddenpoel} (pad + en + poel, pad &rarr; padd). Stripping the linker
 * leaves {@code zonn}/{@code padd}, and this generator drops the duplicate to recover the base form
 * {@code zon}/{@code pad}.</p>
 *
 * <p>Only consonants that actually surface as written geminates in Dutch are considered; {@code v}/{@code z} are
 * excluded because they alternate with {@code f}/{@code s} instead (final devoicing, a separate generator).</p>
 */
public class DutchDegeminationWordGenerator implements WordGenerator {

    private static final String DOUBLABLE_CONSONANTS = "bdfgklmnprst";

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        final int length = reducedModifier.length();
        if (length < 3) {
            return Optional.empty();
        }

        final char last = reducedModifier.charAt(length - 1);
        if (last != reducedModifier.charAt(length - 2) || DOUBLABLE_CONSONANTS.indexOf(last) < 0) {
            return Optional.empty();
        }

        return Optional.of(reducedModifier.subSequence(0, length - 1));
    }
}
