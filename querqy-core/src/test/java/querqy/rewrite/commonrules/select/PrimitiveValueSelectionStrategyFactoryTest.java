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

import static org.junit.Assert.*;
import static querqy.rewrite.commonrules.select.PrimitiveValueSelectionStrategyFactory.criteriaToJsonPathExpressionCriterion;
import static querqy.rewrite.commonrules.model.InstructionsTestSupport.instructions;

import org.junit.Test;
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