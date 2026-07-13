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
package querqy.rewrite.rules.input;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import querqy.model.Input;
import querqy.rewriter.commonrules.InputTermParser;
import querqy.rewriter.commonrules.model.InstructionsSupplier;
import querqy.rewriter.commonrules.select.booleaninput.BooleanInputParser;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputElement;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.rules.rule.Rule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// TODO: This adapter is only a temporary solution to allow a stepwise migration to a new parsing solution
//  To make the structure of this parser comparable to the other parsers, several refactorings for code in Input
//  and BooleanInputParser are required.
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class InputParserAdapter {

    private final BooleanInputParser booleanInputParser;

    private final String inputSkeleton;

    private Input input;

    @Builder
    private InputParserAdapter(final boolean isAllowedToParseBooleanInput) {
        this.booleanInputParser = isAllowedToParseBooleanInput ? new BooleanInputParser() : null;
        inputSkeleton = null;
    }

    public InputParserAdapter with(final String inputSkeleton) {
        return of(booleanInputParser, inputSkeleton);
    }

    public InputAdapter parse() {
        assertThatThisIsNotPrototype();

        if (booleanInputParser != null) {
            parsePotentiallyAsBooleanInput();

        } else {
            parseAsSingleInput();
        }

        return InputAdapter.builder()
                .input(input)
                .booleanInputParser(booleanInputParser)
                .inputSkeleton(inputSkeleton)
                .build();
    }

    private void assertThatThisIsNotPrototype() {
        if (inputSkeleton == null) {
            throw new UnsupportedOperationException("Methods cannot be used on prototype");
        }
    }


    private void parsePotentiallyAsBooleanInput() {
        final List<BooleanInputElement> elements = booleanInputParser.parseInputStringToElements(inputSkeleton);
        // not all instructions can have boolean input -> use boolean input only if we have boolean predicates.
        if (elements.stream().anyMatch(element -> (element.type == BooleanInputElement.Type.OR)
                || (element.type == BooleanInputElement.Type.AND)
                || (element.type == BooleanInputElement.Type.NOT))) {

            input = new Input.BooleanInput(elements, booleanInputParser, inputSkeleton);

        } else {
            parseAsSingleInput();
        }
    }

    private void parseAsSingleInput() {
        input = Input.parseSimpleInput(inputSkeleton);
    }

    public List<Rule> createRulesFromLiterals() {
        if (booleanInputParser == null) {
            return Collections.emptyList();
        }

        return booleanInputParser.getLiteralRegister().values()
                .stream()
                .map(this::createRule)
                .collect(Collectors.toList());
    }

    private Rule createRule(final BooleanInputLiteral literal) {
        final Input.SimpleInput literalInput = parseLiteral(literal);
        final InstructionsSupplier instructionsSupplier = new InstructionsSupplier(literal);

        return Rule.of(literalInput, instructionsSupplier);

    }

    private Input.SimpleInput parseLiteral(final BooleanInputLiteral literal) {
        final String termString = String.join(" ", literal.getTerms());
        return InputTermParser.parseInput(termString);
    }

}
