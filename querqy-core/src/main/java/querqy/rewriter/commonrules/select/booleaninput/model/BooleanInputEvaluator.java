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
package querqy.rewriter.commonrules.select.booleaninput.model;

import querqy.rewriter.commonrules.model.Instructions;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class BooleanInputEvaluator {

    private final boolean[] booleans;
    private final Predicate<boolean[]> predicate;
    private final Instructions instructions;

    public BooleanInputEvaluator(final int numberOfLiterals,
                                 final Predicate<boolean[]> predicate,
                                 final Instructions instructions) {

        booleans = new boolean[numberOfLiterals];
        Arrays.fill(booleans, false);
        this.predicate = predicate;
        this.instructions = instructions;
    }

    public void notify(final int referenceId) {
        this.booleans[referenceId] = true;
    }

    public Optional<Instructions> evaluate() {
        return predicate.test(this.booleans) ? Optional.of(this.instructions) : Optional.empty();
    }
}
