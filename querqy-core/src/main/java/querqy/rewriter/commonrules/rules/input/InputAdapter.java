/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewriter.commonrules.rules.input;

import lombok.Builder;
import lombok.Getter;
import querqy.model.Input;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewriter.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewriter.commonrules.rules.RuleParseException;
import querqy.rewriter.commonrules.rules.instruction.InstructionType;

import java.util.Collections;
import java.util.List;

import static querqy.rewriter.commonrules.rules.instruction.InstructionType.DELETE;
import static querqy.rewriter.commonrules.rules.instruction.InstructionType.SYNONYM;

@Builder
public class InputAdapter {

    @Getter private final Input input;
    private final BooleanInputParser booleanInputParser;
    private final String inputSkeleton;

    public List<Term> getInputTerms() {
        return input != null ? input.getInputTerms() : Collections.emptyList();
    }

    public boolean isBooleanInput() {
        return input instanceof Input.BooleanInput;
    }

    public void evaluateThatInstructionTypesAreSupported(final List<InstructionType> instructionTypes) {
        if (isBooleanInput() && instructionTypes.stream()
                .anyMatch(instructionType -> instructionType == DELETE || instructionType == SYNONYM)){
            throw new RuleParseException("SYNONYM and DELETE instructions are not allowed for boolean input");
        }
    }

    public void createBooleanInputLiterals(final Instructions instructions) {
        if (booleanInputParser != null && isBooleanInput()) {
            final List<BooleanInputElement> elements = ((Input.BooleanInput) input).getElements();
            try {
                booleanInputParser.createInputBuilder(elements, inputSkeleton).withInstructions(instructions).build();

            } catch (querqy.rewriter.commonrules.RuleParseException e) {
                throw new RuleParseException(e.getMessage());
            }
        }
    }

}
