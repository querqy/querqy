package querqy.rewrite.commonrules;

import static org.junit.Assert.*;
import static querqy.rewrite.commonrules.PrimitiveValueSelectionStrategyFactory.criteriaToJsonPathExpressionCriterion;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Test;
import querqy.rewrite.commonrules.model.FilterCriterion;
import querqy.rewrite.commonrules.model.Instructions;

public class PrimitiveValueSelectionStrategyFactoryTest {

    @Test
    public void testThatInstructionsWithMatchingNameAndValuePropsIsAccepted() {

        final Instructions instructions = instructions(1, "n1", "v1");

        final FilterCriterion filter = criteriaToJsonPathExpressionCriterion("n1:v1");
        assertTrue(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesNameOnlyIsNotAccepted() {

        final Instructions instructions = instructions(1, "n1", "v2");

        final FilterCriterion filter = criteriaToJsonPathExpressionCriterion("n1:v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsThatMatchesValueOnlyIsNotAccepted() {

        final Instructions instructions = instructions(1, "n2", "v1");

        final FilterCriterion filter = criteriaToJsonPathExpressionCriterion("n1:v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatInstructionsWithNoPropsIsNotAccepted() {

        final Instructions instructions = instructions(1);

        final FilterCriterion filter = criteriaToJsonPathExpressionCriterion("n1:v1");
        assertFalse(filter.isValid(instructions));
    }

    @Test
    public void testThatEqualsDependsOnNameAndValue() {

        assertEquals(criteriaToJsonPathExpressionCriterion("n1:v1"),
                criteriaToJsonPathExpressionCriterion("n1:v1"));
        assertNotEquals(criteriaToJsonPathExpressionCriterion("n1:v1"),
                criteriaToJsonPathExpressionCriterion("n1:v2"));
        assertNotEquals(criteriaToJsonPathExpressionCriterion("n1:v1"),
                criteriaToJsonPathExpressionCriterion("n2:v1"));

    }

    @Test
    public void testThatHashcodeEqualsForSameNameAndValue() {

        assertEquals(criteriaToJsonPathExpressionCriterion("n1:v1").hashCode(),
                criteriaToJsonPathExpressionCriterion("n1:v1").hashCode());

    }

}