package querqy.rewrite.commonrules.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Test;

public class FilterCriterionTest {

    @Test
    public void testThatInstructionsWithMatchingNameAndValuePropsIsAccepted() {

        final Instructions instructions = InstructionsTestSupport.instructions(1, "n1", "v1");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertTrue(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesNameOnlyIsNotAccepted() {

        final Instructions instructions = InstructionsTestSupport.instructions(1, "n1", "v2");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesValueOnlyIsNotAccepted() {

        final Instructions instructions = InstructionsTestSupport.instructions(1, "n2", "v1");

        final FilterCriterion filter = new FilterCriterion("n1", "v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsWithNoPropsIsNotAccepted() {

        final Instructions instructions = instructions(1);

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
