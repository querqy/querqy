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

import java.util.List;

import querqy.model.Input;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;
import querqy.rewrite.rules.rule.Rule;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {
    
    final TrieMap<InstructionsSupplier> map = new TrieMap<>();

    // we keep this just for the deprecated build() method
    @Deprecated
    private final LookupPreprocessor lookupPreprocessor;
    private final InputSequenceNormalizer inputSequenceNormalizer;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this(ignoreCase ? LookupPreprocessorFactory.lowercase() : LookupPreprocessorFactory.identity());
    }

    public TrieMapRulesCollectionBuilder(final LookupPreprocessor lookupPreprocessor) {
        inputSequenceNormalizer = new InputSequenceNormalizer(lookupPreprocessor);
        this.lookupPreprocessor = lookupPreprocessor;
    }

    @Override
    public void addRule(final Input.SimpleInput input, final Instructions instructions) {
        addOrMergeInstructionsSupplier(input, new InstructionsSupplier(instructions));
    }

    @Override
    public void addRule(final Input.SimpleInput input, final BooleanInputLiteral literal) {
        addOrMergeInstructionsSupplier(input, new InstructionsSupplier(literal));
    }

    @Override
    public void addRule(Rule rule) {
        addOrMergeInstructionsSupplier(rule.getInput(), rule.getInstructionsSupplier());
    }

    public void addOrMergeInstructionsSupplier(final Input.SimpleInput input,
                                               final InstructionsSupplier instructionsSupplier) {

        final List<CharSequence> seqs = inputSequenceNormalizer.getNormalizedInputSequences(input);
        final List<Term> inputTerms = input.getInputTerms();

        final boolean isPrefix = (!inputTerms.isEmpty()) &&  inputTerms.get(inputTerms.size() -1) instanceof PrefixTerm;
        for (final CharSequence seq : seqs) {
                
            final States<InstructionsSupplier> states = map.get(seq);
                
            if (isPrefix) {
                    
                boolean added = false;

                final List<State<InstructionsSupplier>> prefixes = states.getPrefixes();
                    
                if (prefixes != null) {
                    for (final State<InstructionsSupplier> state: prefixes) {
                        if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                            state.value.merge(instructionsSupplier);
                            added = true;
                            break;
                        }
                            
                    }
                }
                    
                if (!added) {
                    map.putPrefix(seq, instructionsSupplier);
                }
            } else {
                final State<InstructionsSupplier> state = states.getStateForCompleteSequence();
                if (state.value != null) {
                    state.value.merge(instructionsSupplier);
                } else {
                    map.put(seq, instructionsSupplier);
                }
            }
                
        }

    }

    /* (non-Javadoc)
     * @see querqy.rewriter.commonrules.model.RulesCollectionBuilder#build()
     */
    @Override
    public RulesCollection build() {
        return new TrieMapRulesCollection(map, lookupPreprocessor);
    }

    @Override
    public TrieMap<InstructionsSupplier> getTrieMap() {
        return map;
    }

}
