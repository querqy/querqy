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
 * <p>Reverses the combined vowel-shortening + gemination-free devoicing that some Dutch stems undergo together: a
 * stem with both a long vowel and a final devoicing consonant shows the shortened vowel spelling <em>and</em> the
 * voiced consonant once it is no longer word-final, e.g. {@code slavenhandel} (slaaf + en + handel, slaaf &rarr;
 * slaven) or {@code gravenstraat} (graaf + en + straat, graaf &rarr; graven). Stripping the linker leaves
 * {@code slav}/{@code grav}, and this generator devoices the final consonant back, then lengthens the preceding
 * vowel, to recover the base form {@code slaaf}/{@code graaf}.</p>
 *
 * <p>Delegates to {@link DutchDevoicingWordGenerator} and {@link DutchVowelLengtheningWordGenerator} in sequence,
 * so a candidate is only produced when both alternations actually apply &mdash; a stem needing just one of the two
 * (e.g. {@code duiv} &rarr; {@code duif}, no lengthening; {@code schap} &rarr; {@code schaap}, no devoicing) is left
 * to the corresponding single generator and does not get a spurious duplicate here.</p>
 */
public class DutchLengtheningDevoicingWordGenerator implements WordGenerator {

    private final WordGenerator devoicing = new DutchDevoicingWordGenerator();
    private final WordGenerator lengthening = new DutchVowelLengtheningWordGenerator();

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        return devoicing.generateModifier(reducedModifier)
                .flatMap(lengthening::generateModifier);
    }
}
