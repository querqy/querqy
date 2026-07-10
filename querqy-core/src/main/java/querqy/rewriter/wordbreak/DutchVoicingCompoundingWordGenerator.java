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
 * <p>The compounding-direction inverse of {@link DutchDevoicingWordGenerator}: some modifiers ending in {@code f}/
 * {@code s} voice that final consonant to {@code v}/{@code z} once it is no longer word-final, e.g. {@code duif +
 * en -> duiven}, {@code huis + en -> huizen}.</p>
 *
 * <p>Unlike {@link DutchGeminationCompoundingWordGenerator} and {@link DutchVowelShorteningCompoundingWordGenerator},
 * this alternation is <strong>not predictable from spelling</strong>: it is a lexically arbitrary fact about each
 * word (compare {@code duif -> duiven} with {@code vis -> vissen}, which geminates instead despite an identical
 * surface shape). This generator therefore always fires for any {@code f-}/{@code s}-final modifier, alongside the
 * plain and geminating candidates; as everywhere else in this class, {@code TermCorpus} is what discards the
 * hypotheses that aren't real words, not the generator.</p>
 */
public class DutchVoicingCompoundingWordGenerator implements WordGenerator {

    private final CharSequence suffix;

    public DutchVoicingCompoundingWordGenerator(final CharSequence suffix) {
        this.suffix = suffix;
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence modifier) {
        final int length = modifier.length();
        if (length == 0) {
            return Optional.empty();
        }

        final char last = modifier.charAt(length - 1);
        final String voiced;
        switch (last) {
            case 'f':
                voiced = "v";
                break;
            case 's':
                voiced = "z";
                break;
            default:
                return Optional.empty();
        }

        return Optional.of(new CompoundCharSequence(null,
                modifier.subSequence(0, length - 1), voiced, suffix));
    }
}
