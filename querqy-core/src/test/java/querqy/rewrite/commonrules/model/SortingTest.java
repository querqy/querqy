package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;
import querqy.rewrite.commonrules.model.Sorting.SortOrder;

public class SortingTest {

    Sorting sortAsc = new Sorting("f1", SortOrder.ASC);
    Sorting sortDesc = new Sorting("f1", SortOrder.DESC);

    @Test
    public void testThatSortAscUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = instructions(20);
        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = instructions(20);
        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "some value");
        final Instructions instructions2 = instructions(20);

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortAsc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "some value");

        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortAsc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "some value");
        final Instructions instructions2 = instructions(20);

        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortDesc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = instructions(10);
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "some value");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortDesc.compare(instructions2, instructions2));

    }


    @Test
    public void testThatSortAscUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v1");

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v1");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }


    @Test
    public void testSortAscByProperty() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v2");

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }

    @Test
    public void testSortDescByProperty() {

        final Instructions instructions1 = InstructionsTestSupport.instructions(10, "f1", "v1");
        final Instructions instructions2 = InstructionsTestSupport.instructions(20, "f1", "v2");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }

    @Test
    public void testThatEqualsDependsOnNameAndOrder() {
        assertEquals(new Sorting("n1", SortOrder.DESC), new Sorting("n1", SortOrder.DESC));
        assertEquals(new Sorting("n1", SortOrder.ASC), new Sorting("n1", SortOrder.ASC));
        assertNotEquals(new Sorting("n1", SortOrder.DESC), new Sorting("n1", SortOrder.ASC));
        assertNotEquals(new Sorting("n1", SortOrder.DESC), new Sorting("n2", SortOrder.DESC));
        assertNotEquals(new Sorting("n1", SortOrder.ASC), new Sorting("n2", SortOrder.ASC));
    }

    @Test
    public void testThatHashcodeEqualsForSameNameOrder() {

        assertEquals(new Sorting("n1", SortOrder.DESC).hashCode(), new Sorting("n1", SortOrder.DESC).hashCode());
        assertEquals(new Sorting("n1", SortOrder.ASC).hashCode(), new Sorting("n1", SortOrder.ASC).hashCode());

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
