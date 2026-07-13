/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.commonrules.rules.input;

import org.junit.Test;
import querqy.rewriter.commonrules.model.Instructions;
import querqy.rewriter.commonrules.rules.rule.Rule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

public class InputParserAdapterTest {

    @Test
    public void testThat_inputWithBooleanOperator_isParsedAsBooleanInput() {
        final InputAdapter result = adapter(true).with("a AND b").parse();

        assertThat(result.isBooleanInput()).isTrue();
        assertThat(result.getInputTerms()).isEmpty();
    }

    @Test
    public void testThat_inputWithoutOperators_fallsBackToSimpleInput_evenWhenBooleanInputIsAllowed() {
        final InputAdapter result = adapter(true).with("a b").parse();

        assertThat(result.isBooleanInput()).isFalse();
        assertThat(result.getInputTerms()).extracting(Object::toString).containsExactly("a", "b");
    }

    @Test
    public void testThat_operatorLookingInput_isParsedAsLiteralTerms_whenBooleanInputIsNotAllowed() {
        final InputAdapter result = adapter(false).with("a AND b").parse();

        assertThat(result.isBooleanInput()).isFalse();
        assertThat(result.getInputTerms()).extracting(Object::toString).containsExactly("a", "AND", "b");
    }

    @Test
    public void testThat_parseThrows_onPrototypeThatWasNeverGivenAnInputSkeleton() {
        final InputParserAdapter prototype = InputParserAdapter.builder()
                .isAllowedToParseBooleanInput(true)
                .build();

        assertThrows(UnsupportedOperationException.class, prototype::parse);
    }

    @Test
    public void testThat_createRulesFromLiterals_returnsEmpty_whenBooleanInputIsNotAllowed() {
        final InputParserAdapter adapter = adapter(false);
        adapter.with("a b").parse();

        assertThat(adapter.createRulesFromLiterals()).isEmpty();
    }

    @Test
    public void testThat_createRulesFromLiterals_returnsEmpty_beforeAnyLiteralWasRegistered() {
        final InputParserAdapter adapter = adapter(true);
        // parsing alone (without InputAdapter#createBooleanInputLiterals) never registers a literal
        adapter.with("a AND b").parse();

        assertThat(adapter.createRulesFromLiterals()).isEmpty();
    }

    @Test
    public void testThat_createRulesFromLiterals_producesOneRulePerDistinctLiteral() {
        final InputParserAdapter adapter = adapter(true);
        final InputAdapter result = adapter.with("a AND b").parse();

        result.createBooleanInputLiterals(mock(Instructions.class));

        final List<Rule> rules = adapter.createRulesFromLiterals();

        assertThat(rules).hasSize(2);
        assertThat(rules)
                .extracting(rule -> rule.getInput().getInputTerms().get(0).toString())
                .containsExactlyInAnyOrder("a", "b");
        assertThat(rules)
                .allSatisfy(rule -> assertThat(rule.getInstructionsSupplier().getLiteral()).isPresent());
    }

    @Test
    public void testThat_createRulesFromLiterals_dedupesRepeatedLiteral() {
        final InputParserAdapter adapter = adapter(true);
        final InputAdapter result = adapter.with("a AND a").parse();

        result.createBooleanInputLiterals(mock(Instructions.class));

        assertThat(adapter.createRulesFromLiterals()).hasSize(1);
    }

    private InputParserAdapter adapter(final boolean allowBooleanInput) {
        return InputParserAdapter.builder().isAllowedToParseBooleanInput(allowBooleanInput).build();
    }

}
