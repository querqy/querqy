/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
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
package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.commonrules.model.TermMatches;
import querqy.rewrite.commonrules.select.booleaninput.BooleanInputQueryHandler;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class TopRewritingActionCollector {

    private final BooleanInputQueryHandler booleanInputQueryHandler = new BooleanInputQueryHandler();

    public void collect(final InstructionsSupplier instructionsSupplier, final Function<Instructions, Action> actionCreator) {

        if (instructionsSupplier.hasInstructions()) {
            this.offer(instructionsSupplier.getInstructionsList(), actionCreator);
        }

        instructionsSupplier.getLiteral().ifPresent(booleanInputQueryHandler::notifyLiteral);
    }

    public TopRewritingActionCollector evaluateBooleanInput() {
        booleanInputQueryHandler.evaluate().forEach(
                instructionsFromBooleanInput -> offer(
                        Collections.singletonList(instructionsFromBooleanInput),
                        instructions -> new Action(instructions, TermMatches.empty(), 0, 0)));

        return this;
    }

    public abstract void offer(List<Instructions> instructions, Function<Instructions, Action> actionCreator);

    public abstract List<Action> createActions();

    public abstract int getLimit();

    public abstract List<? extends FilterCriterion> getFilters();
}
