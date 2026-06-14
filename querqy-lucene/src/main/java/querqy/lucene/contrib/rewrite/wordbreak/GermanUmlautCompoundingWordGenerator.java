/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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

import querqy.CompoundCharSequence;

import java.util.Optional;

public class GermanUmlautCompoundingWordGenerator implements WordGenerator {

    private final CharSequence suffix;

    public GermanUmlautCompoundingWordGenerator(final CharSequence suffix) {
        this.suffix = suffix;
    }
    public GermanUmlautCompoundingWordGenerator() {
        this.suffix = "";
    }

    @Override
    public Optional<CharSequence> generateModifier(final CharSequence reducedModifier) {

        String replacement = null;
        int position = reducedModifier.length() - 1;
        while ((position > -1) && (replacement == null)) {
            switch (reducedModifier.charAt(position)) {
                case 'a':
                    replacement = "ä";
                    break;
                case 'o':
                    replacement = "ö";
                    break;
                case 'u':
                    replacement = "ü";
                    break;
                default:
                    position--;
            }
        }

        if (replacement == null) {
            return Optional.empty();
        }
        if (position == 0) {
            return Optional.of(new CompoundCharSequence(null, replacement,
                    reducedModifier.subSequence(1, reducedModifier.length())));
        }


        return Optional.of(
                new CompoundCharSequence(null, reducedModifier.subSequence(0, position), replacement,
                        reducedModifier.subSequence(position + 1, reducedModifier.length()), suffix));


    }
}
