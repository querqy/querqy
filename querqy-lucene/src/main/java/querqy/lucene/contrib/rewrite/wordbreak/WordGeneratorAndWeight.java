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
package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.Optional;

public class WordGeneratorAndWeight {

    public final WordGenerator generator;
    public final float weight;

    public WordGeneratorAndWeight(final WordGenerator generator, final float weight) {
        this.generator = generator;
        this.weight = weight;
    }

    public Optional<Suggestion> generateSuggestion(final CharSequence reducedModifier) {
        final Optional<CharSequence> modifier = generator.generateModifier(reducedModifier);
        return modifier.map(charSequence -> new Suggestion(new CharSequence[]{charSequence}, weight));
    }

}
