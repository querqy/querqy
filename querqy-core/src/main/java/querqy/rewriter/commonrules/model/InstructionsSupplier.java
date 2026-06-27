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
package querqy.rewriter.commonrules.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode
@ToString(includeFieldNames = false)
public class InstructionsSupplier {

    private final List<Instructions> instructionsList = new LinkedList<>();
    private BooleanInputLiteral literal = null;

    public InstructionsSupplier(final List<Instructions> instructionsList, final BooleanInputLiteral literal) {
        if (instructionsList != null) {
            this.instructionsList.addAll(instructionsList);
        }
        this.literal = literal;
    }

    public InstructionsSupplier(final Instructions instructions) {
        instructionsList.add(instructions);
    }

    public InstructionsSupplier(final BooleanInputLiteral literal) {
        this(Collections.emptyList(), literal);
    }

    public void merge(final InstructionsSupplier instructionsSupplier) {

        if (this.literal != null) {
            instructionsSupplier.getLiteral().ifPresent(otherLiteral -> {
                if (!literal.equals(otherLiteral)) {
                    throw new IllegalArgumentException(String.format("Literals not equal: %s != %s",
                            String.join(" ", literal.getTerms()),
                            String.join(" ", otherLiteral.getTerms())
                    ));
                }
            });
        } else {
            this.literal = instructionsSupplier.literal;
        }

        this.instructionsList.addAll(instructionsSupplier.getInstructionsList());
    }

    public boolean hasInstructions() {
        return !instructionsList.isEmpty();
    }
    public List<Instructions> getInstructionsList() {
        return instructionsList;
    }

    public Optional<BooleanInputLiteral> getLiteral() {
        return Optional.ofNullable(literal);
    }
}
