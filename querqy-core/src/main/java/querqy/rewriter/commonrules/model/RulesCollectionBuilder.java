/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
package querqy.rewriter.commonrules.model;

import querqy.model.Input;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.lookup.triemap.suffix.SuffixWildcardRules;
import querqy.rewrite.rules.rule.Rule;
import querqy.trie.TrieMap;

public interface RulesCollectionBuilder {

    void addRule(Input.SimpleInput input, Instructions instructions);

    void addRule(Input.SimpleInput input, BooleanInputLiteral literal);

    void addRule(final Rule rule);

    RulesCollection build();

    TrieMap<InstructionsSupplier> getTrieMap();

    SuffixWildcardRules<InstructionsSupplier> getSuffixWildcardRules();

}