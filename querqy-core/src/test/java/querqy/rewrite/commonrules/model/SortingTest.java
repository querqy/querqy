package querqy.rewrite.commonrules.model;

import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;

public class SortingTest {

    Sorting sortAsc = new Sorting("f1", Sorting.SortOrder.ASC);
    Sorting sortDesc = new Sorting("f1", Sorting.SortOrder.DESC);

    @Test
    public void testThatSortAscUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = new Instructions(10);
        final Instructions instructions2 = new Instructions(20);
        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfNoPropertyWasSet() {

        final Instructions instructions1 = new Instructions(10);
        final Instructions instructions2 = new Instructions(20);
        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "some value");
        final Instructions instructions2 = new Instructions(20);

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortAsc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForAscOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = new Instructions(10);
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "some value");

        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortAsc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForLowerOrd() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "some value");
        final Instructions instructions2 = new Instructions(20);

        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortDesc.compare(instructions2, instructions2));

    }

    @Test
    public void testMissingPropertyIsSortedLastForDescOrderIfPropertyExistsForHigherOrd() {

        final Instructions instructions1 = new Instructions(10);
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "some value");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));
        Assert.assertEquals(0, sortDesc.compare(instructions2, instructions2));

    }


    @Test
    public void testThatSortAscUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "v1");
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "v1");

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }


    @Test
    public void testThatSortDescUsesOrdIfPropertyHasSameValueForBoth() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "v1");
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "v1");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }


    @Test
    public void testSortAscByProperty() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "v1");
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "v2");

        assertThat(sortAsc.compare(instructions1, instructions2), Matchers.lessThan(0));
        assertThat(sortAsc.compare(instructions2, instructions1), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortAsc.compare(instructions1, instructions1));

    }

    @Test
    public void testSortDescByProperty() {

        final Instructions instructions1 = new Instructions(10);
        instructions1.addProperty("f1", "v1");
        final Instructions instructions2 = new Instructions(20);
        instructions2.addProperty("f1", "v2");

        assertThat(sortDesc.compare(instructions2, instructions1), Matchers.lessThan(0));
        assertThat(sortDesc.compare(instructions1, instructions2), Matchers.greaterThan(0));
        Assert.assertEquals(0, sortDesc.compare(instructions1, instructions1));

    }

}
