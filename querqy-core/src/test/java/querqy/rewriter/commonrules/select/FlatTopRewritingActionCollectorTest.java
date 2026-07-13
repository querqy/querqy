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
package querqy.rewriter.commonrules.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static querqy.rewriter.commonrules.select.ConfigurationOrderSelectionStrategy.COMPARATORS;
import static querqy.rewriter.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Assert;
import org.junit.Test;
import querqy.rewriter.commonrules.model.Action;
import querqy.rewriter.commonrules.model.TermMatches;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FlatTopRewritingActionCollectorTest {

    @Test
    public void testThatNegativeLimitAcceptsAllInstructions() {

        final TopRewritingActionCollector collector
                = new FlatTopRewritingActionCollector(COMPARATORS, -1, Collections.emptyList());

        final int numActions = 5 + new Random().nextInt(30);

        for (int i = 0; i < numActions; i++) {
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches())
            );
        }

        final List<Action> actions = collector.createActions();
        assertEquals(numActions, actions.size());

    }

    @Test
    public void testThatCollectorAcceptsLimitToInstructionsAndSortsByComparator() {

        final int numActions = 5;// + new Random().nextInt(30);
        final int limit = numActions - 3;
        final TopRewritingActionCollector collector
                = new FlatTopRewritingActionCollector(COMPARATORS, limit, Collections.emptyList());

        for (int i = 0; i < numActions; i++) {
            collector.offer(
                    Collections.singletonList(instructions(numActions - i - 1)), // reverse order
                    instr -> new Action(instr, new TermMatches())
            );
        }


        final List<Action> actions = collector.createActions();
        assertEquals(limit, actions.size());
        for (int i = 0; i < limit; i++) {
            assertEquals(i, actions.get(i).getInstructions().getOrd());
        }


    }


    @Test
    public void testThatNegativeLimitAcceptsNoInstructions() {

        final TopRewritingActionCollector collector
                = new FlatTopRewritingActionCollector(COMPARATORS, 0, Collections.emptyList());

        final int numActions = 5 + new Random().nextInt(30);

        for (int i = 0; i < numActions; i++) {
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches())
            );
        }

        final List<Action> actions = collector.createActions();
        Assert.assertTrue(actions.isEmpty());

    }

    @Test
    public void testThatFiltersAreAppliedAsBooleanAnd() {
        final TopRewritingActionCollector collector
                = new FlatTopRewritingActionCollector(COMPARATORS, -1, Arrays.asList(
                        instructions -> instructions.getOrd() % 2 == 0,
                        instructions -> instructions.getOrd() % 3 == 0));

        final int numActions = 10;

        for (int i = 0; i < numActions; i++) {
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches())
            );
        }

        final int[] ords = collector.createActions().stream()
                .mapToInt(action -> action.getInstructions().getOrd()).toArray();

        assertTrue(Arrays.equals(new int[] {0, 6}, ords));


    }



}
