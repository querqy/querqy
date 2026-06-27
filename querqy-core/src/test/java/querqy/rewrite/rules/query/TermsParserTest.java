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
package querqy.rewrite.rules.query;

import org.junit.Test;
import querqy.rewriter.commonrules.model.PrefixTerm;
import querqy.rewriter.commonrules.model.Term;
import querqy.rewrite.rules.RuleParseException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class TermsParserTest {

    @Test
    public void testThat_exceptionIsThrown_forMultipleWildcardReferences() {
        final TermsParser parser = parser().with("a$1b$1");
        assertThrows(RuleParseException.class, parser::parse);
    }

    @Test
    public void testThat_exceptionIsThrown_forEmptyValue() {
        final TermsParser parser = parser().with("");
        assertThrows(RuleParseException.class, parser::parse);
    }

    @Test
    public void testThat_exceptionIsThrown_forBlankValue() {
        final TermsParser parser = parser().with("  \t ");
        assertThrows(RuleParseException.class, parser::parse);
    }

    @Test
    public void testThat_exceptionIsThrown_forMultiFieldDefinitionWithoutOpeningBracket() {
        final TermsParser parser = parser().with(" a}:b ");
        assertThrows(RuleParseException.class, parser::parse);
    }

    @Test
    public void testThat_exceptionIsThrown_forMultiFieldDefinitionWithoutClosingBracket() {
        final TermsParser parser = parser().with(" {a:b ");
        assertThrows(RuleParseException.class, parser::parse);
    }

    @Test
    public void testThat_valueIsParsedProperly_forSingleTerm() {
        final List<Term> terms = parser().with(" b ").parse();
        assertThat(terms).containsExactly(
                term("b")
        );
    }

    @Test
    public void testThat_valueIsParsedProperly_forMultipleTerms() {
        final List<Term> terms = parser().with(" b c ").parse();
        assertThat(terms).containsExactly(
                term("b"),
                term("c")
        );
    }

    @Test
    public void testThat_valueIsParsedProperly_forMultipleTermsWithSingleFieldDefinition() {
        final List<Term> terms = parser().with(" a:b c ").parse();
        assertThat(terms).containsExactly(
                term("b", "a"),
                term("c")
        );
    }

    @Test
    public void testThat_valueIsParsedProperly_forMultipleTermsWithMultipleFieldDefinitions() {
        final List<Term> terms = parser().with(" {a,b}:c d:e f ").parse();
        assertThat(terms).containsExactly(
                term("c", "a", "b"),
                term("e", "d"),
                term("f")
        );
    }

    @Test
    public void testThat_valueIsParsedProperly_forPrefixedTerms() {
        final List<Term> terms = parser().with(" {a,b}:c* d:e* f* g ").parse();
        assert terms.get(0) instanceof PrefixTerm;
        assert terms.get(1) instanceof PrefixTerm;
        assert terms.get(2) instanceof PrefixTerm;
        assert !(terms.get(3) instanceof PrefixTerm);

        assertThat(terms).containsExactly(
                prefix("c", "a", "b"),
                prefix("e", "d"),
                prefix("f"),
                term("g")
        );
    }

    private TermsParser parser() {
        return TermsParser.createPrototype();
    }

    private Term term(final String value, final String... fields) {
        return new Term(value.toCharArray(), 0, value.length(), Arrays.asList(fields));
    }

    private Term prefix(final String value, final String... fields) {
        return new PrefixTerm(value.toCharArray(), 0, value.length(), Arrays.asList(fields));
    }
}
