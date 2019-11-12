package querqy.rewrite.commonrules.select;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static querqy.rewrite.commonrules.select.ConfigurationOrderSelectionStrategy.COMPARATORS;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsProperties;
import querqy.rewrite.commonrules.model.TermMatches;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TopLevelRewritingActionCollectorTest {

    @Mock
    Instructions instruction1;
    @Mock
    Instructions instruction2;
    @Mock
    Instructions instruction3;

    @Mock
    InstructionsProperties props1;

    @Mock
    Function<Instructions, Action> func1;

    @Mock
    Function<Instructions, Action> func2;

    @Mock
    Function<Instructions, Action> func3;

    @Mock
    Action action2;

    @Mock
    Action action3;

    @Test
    public void testThatNoInstructionsIsAcceptedIfLimitIsZero() {
        final TopLevelRewritingActionCollector collector = new TopLevelRewritingActionCollector(
                new PropertySorting("p1", Sorting.SortOrder.DESC).getComparators(), 0, Collections.emptyList());

        collector.offer(Collections.singletonList(instruction1),  func1);

        final List<Action> actions = collector.createActions();
        assertTrue(actions.isEmpty());
        Mockito.verify(func1, Mockito.never()).apply(any());
        Mockito.verify(instruction1, Mockito.never()).getProperty(eq("p1"));
        Mockito.verify(props1, Mockito.never()).getProperty(eq("p1"));
    }


    @Test
    public void testThatReturnAllIsFromFirstLayerOnlyIfLimitIsOne() {
        when(instruction1.getProperty(eq("p1"))).thenReturn(Optional.of("v2"));
        when(instruction1.getOrd()).thenReturn(1);

        when(instruction2.getProperty(eq("p1"))).thenReturn(Optional.of("v1"));
        when(instruction2.getOrd()).thenReturn(3);

        when(instruction3.getProperty(eq("p1"))).thenReturn(Optional.of("v1"));
        when(instruction3.getOrd()).thenReturn(2);

        when(func2.apply(any())).thenReturn(action2);
        when(func3.apply(any())).thenReturn(action3);

        final TopLevelRewritingActionCollector collector = new TopLevelRewritingActionCollector(
                new PropertySorting("p1", Sorting.SortOrder.ASC).getComparators(), 1, Collections.emptyList());

        collector.offer(Collections.singletonList(instruction1), func1);
        collector.offer(Collections.singletonList(instruction2), func2);
        collector.offer(Collections.singletonList(instruction3), func3);

        assertThat(collector.createActions(), contains(action3, action2));


    }

    @Test
    public void testThatFiltersAreAppliedAsBooleanAnd() {
        final TopRewritingActionCollector collector
                = new TopLevelRewritingActionCollector(COMPARATORS, 20, Arrays.asList(
                instructions -> instructions.getOrd() % 2 == 0,
                instructions -> instructions.getOrd() % 3 == 0));

        final int numActions = 10;

        for (int i = 0; i < numActions; i++) {
            final int pos = i;
            collector.offer(
                    Collections.singletonList(instructions(i)),
                    instr -> new Action(instr, new TermMatches(), pos, pos + 2)
            );
        }

        final int[] ords = collector.createActions().stream()
                .mapToInt(action -> action.getInstructions().getOrd()).toArray();

        assertTrue(Arrays.equals(new int[] {0, 6}, ords));


    }



}