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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import querqy.CompoundCharSequence;
import querqy.model.Input;
import querqy.rewriter.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessor;
import querqy.rewrite.lookup.preprocessing.LookupPreprocessorFactory;
import querqy.rewrite.lookup.triemap.suffix.SuffixWildcardRules;
import querqy.rewrite.lookup.triemap.suffix.SuffixWildcardRulesBuilder;
import querqy.rewriter.commonrules.rules.rule.Rule;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {

    public static final String BOUNDARY_WORD = "\u0002";

    final TrieMap<InstructionsSupplier> map = new TrieMap<>();
    private final SuffixWildcardRulesBuilder<InstructionsSupplier> suffixWildcardRulesBuilder =
            new SuffixWildcardRulesBuilder<>();

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

        final List<Term> inputTerms = input.getInputTerms();

        final int suffixTermIndex = indexOfSuffixTerm(inputTerms);
        if (suffixTermIndex >= 0) {
            addSuffixWildcardRule(input, inputTerms, suffixTermIndex, instructionsSupplier);
            return;
        }

        final List<CharSequence> seqs = inputSequenceNormalizer.getNormalizedInputSequences(input);

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

    @Override
    public TrieMap<InstructionsSupplier> getTrieMap() {
        return map;
    }

    @Override
    public SuffixWildcardRules<InstructionsSupplier> getSuffixWildcardRules() {
        return suffixWildcardRulesBuilder.build();
    }

    private int indexOfSuffixTerm(final List<Term> inputTerms) {
        for (int i = 0; i < inputTerms.size(); i++) {
            if (inputTerms.get(i) instanceof SuffixTerm) {
                return i;
            }
        }
        return -1;
    }

    // Note: unlike the main trie path above, registering the exact same leading-wildcard rule input more than
    // once does not merge InstructionsSuppliers into a single rule - each registration produces its own separate
    // match/action. This is an accepted limitation for the (rare) case of duplicate rule definitions.
    private void addSuffixWildcardRule(final Input.SimpleInput input, final List<Term> inputTerms,
                                       final int suffixTermIndex, final InstructionsSupplier instructionsSupplier) {

        final SuffixTerm suffixTerm = (SuffixTerm) inputTerms.get(suffixTermIndex);
        final List<Term> leftTerms = inputTerms.subList(0, suffixTermIndex);
        final List<Term> rightTerms = inputTerms.subList(suffixTermIndex + 1, inputTerms.size());

        final List<CharSequence> leftContextParts = new ArrayList<>();
        if (input.isRequiresLeftBoundary()) {
            leftContextParts.add(BOUNDARY_WORD);
        }
        for (final Term term : leftTerms) {
            leftContextParts.add(lookupPreprocessor.process(term));
        }
        final CharSequence leftContextKey = leftContextParts.isEmpty()
                ? null : new CompoundCharSequence(" ", leftContextParts);

        final List<CharSequence> rightTermKeys = new ArrayList<>();
        for (final Term term : rightTerms) {
            rightTermKeys.add(lookupPreprocessor.process(term));
        }
        if (input.isRequiresRightBoundary()) {
            rightTermKeys.add(BOUNDARY_WORD);
        }

        for (final CharSequence suffixKey : suffixKeys(suffixTerm)) {
            suffixWildcardRulesBuilder.addRule(suffixKey, leftContextKey, rightTermKeys, instructionsSupplier);
        }
    }

    private List<CharSequence> suffixKeys(final SuffixTerm suffixTerm) {
        final CharSequence value = lookupPreprocessor.process(suffixTerm);
        if (!suffixTerm.hasFieldNames()) {
            return Collections.singletonList(value);
        }

        final List<CharSequence> keys = new ArrayList<>();
        for (final String fieldName : suffixTerm.getFieldNames()) {
            keys.add(new CompoundCharSequence(Term.FIELD_CHAR, fieldName, value));
        }
        return keys;
    }

}
