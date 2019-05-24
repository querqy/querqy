package querqy.rewrite.commonrules.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static querqy.rewrite.commonrules.model.ConfigurationOrderSelectionStrategy.COMPARATOR;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TopRewritingActionCollectorTest {

    @Test
    public void testThatNegativeLimitAcceptsAllInstructions() {

        final TopRewritingActionCollector collector
                = new TopRewritingActionCollector(COMPARATOR, -1, Collections.emptyList());

        final int numActions = 5 + new Random().nextInt(30);

        for (int i = 0; i < numActions; i++) {
            final int pos = i;
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches(), pos, pos + 2)
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
                = new TopRewritingActionCollector(COMPARATOR, limit, Collections.emptyList());

        for (int i = 0; i < numActions; i++) {
            final int pos = i;
            collector.offer(
                    Collections.singletonList(instructions(numActions - i - 1)), // reverse order
                    instr -> new Action(instr, new TermMatches(), pos, pos + 2)
            );
        }


        final List<Action> actions = collector.createActions();
        assertEquals(limit, actions.size());
        for (int i = 0; i < limit; i++) {
            assertEquals(i, actions.get(i).getInstructions().ord);
        }


    }


    @Test
    public void testThatNegativeLimitAcceptsNoInstructions() {

        final TopRewritingActionCollector collector
                = new TopRewritingActionCollector(COMPARATOR, 0, Collections.emptyList());

        final int numActions = 5 + new Random().nextInt(30);

        for (int i = 0; i < numActions; i++) {
            final int pos = i;
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches(), pos, pos + 2)
            );
        }

        final List<Action> actions = collector.createActions();
        Assert.assertTrue(actions.isEmpty());

    }

    @Test
    public void testThatFiltersAreAppliedAsBooleanAnd() {
        final TopRewritingActionCollector collector
                = new TopRewritingActionCollector(COMPARATOR, -1, Arrays.asList(
                        instructions -> instructions.ord % 2 == 0,
                        instructions -> instructions.ord % 3 == 0));

        final int numActions = 10;

        for (int i = 0; i < numActions; i++) {
            final int pos = i;
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches(), pos, pos + 2)
            );
        }

        final int[] ords = collector.createActions().stream()
                .mapToInt(action -> action.getInstructions().ord).toArray();

        assertTrue(Arrays.equals(new int[] {0, 6}, ords));


    }



}
