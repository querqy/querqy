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
package querqy.rewrite.rules.factory;

import org.junit.Test;
import querqy.rewriter.commonrules.WhiteSpaceQuerqyParserFactory;
import querqy.rewriter.commonrules.model.InstructionsSupplier;
import querqy.rewrite.rules.RuleParseException;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class RulesParserFactoryTest {

    @Test
    public void testThat_shorthandTextParser_parsesValidRules() throws IOException {
        final TrieMap<InstructionsSupplier> trieMap = RulesParserFactory.textParser(
                new StringReader("a =>\n\tSYNONYM: b"),
                new WhiteSpaceQuerqyParserFactory(),
                false,
                true
        ).parse();

        assertThat(trieMap.get("a").getStateForCompleteSequence().isFinal()).isTrue();
    }

    @Test
    public void testThat_shorthandTextParser_throwsOnInvalidRules() {
        // the underlying RuleParseException is wrapped in an IOException by the skeleton parser
        assertThrows(IOException.class, () -> RulesParserFactory.textParser(
                new StringReader("a =>\n\tUNKNOWN_INSTRUCTION: b"),
                new WhiteSpaceQuerqyParserFactory(),
                false,
                true
        ).parse());
    }

    @Test
    public void testThat_shorthandTextParser_forwardsAllowBooleanInput() throws IOException {
        final String rulesWithBooleanInput = "a AND b =>\n\tSYNONYM: c";

        // SYNONYM instructions are not allowed for boolean input - this only triggers if boolean
        // input parsing is actually enabled, proving that the flag is forwarded correctly.
        assertThrows(RuleParseException.class, () -> RulesParserFactory.textParser(
                new StringReader(rulesWithBooleanInput),
                new WhiteSpaceQuerqyParserFactory(),
                true,
                true
        ).parse());

        // with boolean input disabled, "a AND b" is parsed as a literal (three-term) input instead,
        // so the same restriction does not apply and parsing succeeds
        RulesParserFactory.textParser(
                new StringReader(rulesWithBooleanInput),
                new WhiteSpaceQuerqyParserFactory(),
                false,
                true
        ).parse();
    }

    @Test
    public void testThat_shorthandTextParser_forwardsIgnoreCase() throws IOException {
        final TrieMap<InstructionsSupplier> ignoreCaseTrieMap = RulesParserFactory.textParser(
                new StringReader("Foo =>\n\tSYNONYM: bar"),
                new WhiteSpaceQuerqyParserFactory(),
                false,
                true
        ).parse();

        assertThat(ignoreCaseTrieMap.get("foo").getStateForCompleteSequence().isFinal()).isTrue();

        final TrieMap<InstructionsSupplier> caseSensitiveTrieMap = RulesParserFactory.textParser(
                new StringReader("Foo =>\n\tSYNONYM: bar"),
                new WhiteSpaceQuerqyParserFactory(),
                false,
                false
        ).parse();

        assertThat(caseSensitiveTrieMap.get("foo").getStateForCompleteSequence().isFinal()).isFalse();
        assertThat(caseSensitiveTrieMap.get("Foo").getStateForCompleteSequence().isFinal()).isTrue();
    }

}
