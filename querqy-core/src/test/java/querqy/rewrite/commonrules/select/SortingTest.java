package querqy.rewrite.commonrules.select;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;
import querqy.PriorityComparator;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.InstructionsTestSupport;
import querqy.rewrite.commonrules.select.Sorting.SortOrder;

import java.util.Comparator;

public class SortingTest {

    Sorting sortAsc = new PropertySorting("f1", SortOrder.ASC);
    Sorting sortDesc = new PropertySorting("f1", SortOrder.DESC);

    @Test
    public void testThatSortAscUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = instructions(20);

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortAsc.getComparators());

        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = instructions(20);

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortDesc.getComparators());

        assertThat(comparator.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "some value");
        final Instructions instructions2 = instructions(20);

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortAsc.getComparators());

        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));
        Assert.assertEquals(0, comparator.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "some value");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortAsc.getComparators());

        assertThat(comparator.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));
        Assert.assertEquals(0, comparator.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "some value");
        final Instructions instructions2 = instructions(20);

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortDesc.getComparators());

        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));
        Assert.assertEquals(0, comparator.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "some value");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortDesc.getComparators());

        assertThat(comparator.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));
        Assert.assertEquals(0, comparator.compare(instructions2, instructions2));

    }


    @Test
    public void testThatSortAscUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v1");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortAsc.getComparators());

        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v1");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortDesc.getComparators());

        assertThat(comparator.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }


    @Test
    public void testSortAscByProperty() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "modelv2");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortAsc.getComparators());

        assertThat(comparator.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }

    @Test
    public void testSortDescByProperty() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "modelv2");

        final Comparator<Instructions> comparator = new PriorityComparator<>(sortDesc.getComparators());

        assertThat(comparator.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(comparator.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, comparator.compare(instructions1, instructions1));

    }

    @Test
    public void testThatEqualsDependsOnNameAndOrder() {
        assertEquals(new PropertySorting("n1", SortOrder.DESC), new PropertySorting("n1", SortOrder.DESC));
        assertEquals(new PropertySorting("n1", SortOrder.ASC), new PropertySorting("n1", SortOrder.ASC));
        assertNotEquals(new PropertySorting("n1", SortOrder.DESC), new PropertySorting("n1", SortOrder.ASC));
        assertNotEquals(new PropertySorting("n1", SortOrder.DESC), new PropertySorting("n2", SortOrder.DESC));
        assertNotEquals(new PropertySorting("n1", SortOrder.ASC), new PropertySorting("n2", SortOrder.ASC));
    }

    @Test
    public void testThatHashCodeEqualsForSameNameAndOrder() {

        assertEquals(new PropertySorting("n1", SortOrder.DESC).hashCode(),
                new PropertySorting("n1", SortOrder.DESC).hashCode());
        assertEquals(new PropertySorting("n1", SortOrder.ASC).hashCode(),
                new PropertySorting("n1", SortOrder.ASC).hashCode());

    }

    @Test
    public void testSortOrderFromKnownString() {
        assertSame(SortOrder.ASC, SortOrder.fromString("asc"));
        assertSame(SortOrder.DESC, SortOrder.fromString("desc"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatUnknownSortOrderIsNotAccepted() {
        SortOrder.fromString("misc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatEmptySortOrderIsNotAccepted() {
        SortOrder.fromString("");
    }

}
