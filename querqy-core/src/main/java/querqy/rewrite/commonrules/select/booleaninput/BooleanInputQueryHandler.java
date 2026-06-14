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
package querqy.rewrite.commonrules.select.booleaninput;

import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputEvaluator;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class BooleanInputQueryHandler {

    private Map<BooleanInput, BooleanInputEvaluator> evaluatorMap = null;

    public void notifyLiteral(final BooleanInputLiteral literal) {
        if (this.evaluatorMap == null) {
            this.evaluatorMap = new HashMap<>();
        }

        literal.getReferences().forEach(
                reference -> evaluatorMap
                        .computeIfAbsent(reference.getBooleanInput(),
                                key -> reference.getBooleanInput().createEvaluator())
                        .notify(reference.getReferenceId()));
    }

    public Stream<Instructions> evaluate() {
        if (evaluatorMap == null) {
            return Stream.empty();
        } else {
            return evaluatorMap.values().stream()
                    .map(BooleanInputEvaluator::evaluate)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        }
    }
}
