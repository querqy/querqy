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

import java.util.Optional;

public interface WordGenerator {

    /**
     * <p>Generate the modifier word from the reduced modifier. The reduced modifier input parameter will be the
     * modifier word as it occurs in the compound with the suffix from the compound form already stripped off.
     * For example, in 'blatt + wald = blätterwald' this method would be passed 'blätt' as the reduced modifier and is
     * expected to return 'blatt'. In 'kirche + hof = kirchhof', it would be passed 'kirch' and would have to return
     * 'kirche'</p>
     *
     * <p>As a word break strategy might be inapplicable to a given input, the return parameter is optional</p>
     *
     * @param reducedModifier - the modifier in its compound form without a suffix
     *
     * @return An optional modifier word.
     */
    Optional<CharSequence> generateModifier(CharSequence reducedModifier);
}
