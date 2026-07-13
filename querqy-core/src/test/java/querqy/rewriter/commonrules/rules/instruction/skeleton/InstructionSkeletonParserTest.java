/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Querqy Contributors
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
package querqy.rewriter.commonrules.rules.instruction.skeleton;

import org.junit.Test;
import querqy.rewriter.commonrules.rules.RuleParseException;
import querqy.rewriter.commonrules.rules.instruction.InstructionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class InstructionSkeletonParserTest {
    
    @Test
    public void testThat_instructionIsParsed_forDefinitionWithoutValue() {
        assertThat(
                parse("delete")).isEqualTo(
                        instruction(InstructionType.DELETE, null));

        assertThat(
                parse("delete:  ")).isEqualTo(
                        instruction(InstructionType.DELETE, null));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithoutParameters() {
        assertThat(
                parse("up: val")).isEqualTo(
                        instruction(InstructionType.UP, "val"));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithParameters() {
        assertThat(
                parse("up(1.0): val")).isEqualTo(
                        instruction(InstructionType.UP, "1.0", "val"));
    }

    @Test
    public void testThat_instructionIsProperlyParsed_forDefinitionWithParametersAndManyWhitespaces() {
        assertThat(
                parse("   up  (    \t 1.0  )  :  val  ")).isEqualTo(
                        instruction(InstructionType.UP, "1.0", "val"));
    }

    @Test
    public void testThat_exceptionIsThrown_forInvalidParameterDefinitionWithMissingClosingBracket() {
        assertIsNotParsable("up(1.0: val");
    }

    @Test
    public void testThat_exceptionIsThrown_forInvalidParameterDefinitionWithMissingOpeningBracket() {
        assertIsNotParsable("up1.0): val");
    }

    @Test
    public void testThat_exceptionIsThrown_forMissingColon() {
        assertIsNotParsable("up(1.0)val");
    }

    @Test
    public void testThat_exceptionIsThrown_forUnknownType() {
        assertThrows(RuleParseException.class,
                () -> parse("uup(1.0): val"));
    }

    private InstructionSkeleton parse(final String instructionDefinition) {
        final InstructionSkeletonParser parser = InstructionSkeletonParser.create();
        parser.setContent(instructionDefinition);
        parser.isParsable();
        parser.parse();
        return parser.finish();
    }

    private void assertIsNotParsable(final String instructionDefinition) {
        final InstructionSkeletonParser parser = InstructionSkeletonParser.create();
        parser.setContent(instructionDefinition);
        assertFalse(parser.isParsable());
    }

    private InstructionSkeleton instruction(final InstructionType type,
                                            final String value) {
        return instruction(type, null, value);
    }

    private InstructionSkeleton instruction(final InstructionType type,
                                            final String parameter,
                                            final String value) {
        return InstructionSkeleton.builder()
                .type(type)
                .parameter(parameter)
                .value(value)
                .build();
    }



}
