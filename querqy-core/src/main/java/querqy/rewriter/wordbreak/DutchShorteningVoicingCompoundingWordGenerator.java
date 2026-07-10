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
 * <p>The compounding-direction inverse of {@link DutchLengtheningDevoicingWordGenerator}: a modifier with both a
 * double-spelled long vowel and a devoicing final consonant undergoes both alternations together, e.g.
 * {@code slaaf + en -> slaven} or {@code graaf + en -> graven}.</p>
 *
 * <p>Composed from {@link DutchVowelShorteningCompoundingWordGenerator} (run first, with an empty suffix so it only
 * transforms without appending anything) and {@link DutchVoicingCompoundingWordGenerator} (run second, which
 * appends the real suffix), the same way {@link DutchLengtheningDevoicingWordGenerator} composes its two
 * decompounding-direction counterparts. A candidate is only produced when both alternations apply in sequence: a
 * modifier needing just one of the two (e.g. {@code duif} &rarr; devoicing only, {@code schaap} &rarr; shortening
 * only) is left to the corresponding single generator.</p>
 */
public class DutchShorteningVoicingCompoundingWordGenerator implements WordGenerator {

    private final WordGenerator shortening = new DutchVowelShorteningCompoundingWordGenerator("");
    private final WordGenerator voicing;

    public DutchShorteningVoicingCompoundingWordGenerator(final CharSequence suffix) {
        this.voicing = new DutchVoicingCompoundingWordGenerator(suffix);
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence modifier) {
        return shortening.generateModifier(modifier)
                .flatMap(voicing::generateModifier);
    }
}
