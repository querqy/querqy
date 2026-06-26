/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewrite.contrib.wordbreak;

import querqy.CompoundCharSequence;

import java.util.Optional;

public class SuffixWordGenerator implements WordGenerator {

    final CharSequence suffix;

    public SuffixWordGenerator(final CharSequence suffix) {
        if (suffix == null || suffix.length() == 0) {
            throw new IllegalArgumentException("suffix with length > 0 expected");
        }
        this.suffix = suffix;
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {
        return Optional.of(new CompoundCharSequence(null, reducedModifier, suffix));
    }

}
