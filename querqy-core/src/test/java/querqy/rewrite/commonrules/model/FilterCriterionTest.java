package querqy.rewrite.commonrules.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class FilterCriterionTest {

    @Test
    public void testThatInstructionsWithMatchingNameAndValuePropsIsAccepted() {

        final Instructions instructions = new Instructions(1);
        instructions.addProperty("n1", "v1");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertTrue(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesNameOnlyIsNotAccepted() {

        final Instructions instructions = new Instructions(1);
        instructions.addProperty("n1", "v2");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesValueOnlyIsNotAccepted() {

        final Instructions instructions = new Instructions(1);
        instructions.addProperty("n2", "v1");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsWithNoPropsIsNotAccepted() {

        final Instructions instructions = new Instructions(1);

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatEqualsDependsOnNameAndValue() {

        assertEquals(new FilterCriterion("n1", "v1"), new FilterCriterion("n1", "v1"));
        assertNotEquals(new FilterCriterion("n1", "v1"), new FilterCriterion("n1", "v2"));
        assertNotEquals(new FilterCriterion("n1", "v1"), new FilterCriterion("n2", "v1"));

    }

    @Test
    public void testThatHashcodeEqualsForSameNameAndValue() {

        assertEquals(new FilterCriterion("n1", "v1").hashCode(), new FilterCriterion("n1", "v1").hashCode());

    }
}
