/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.List;

import lombok.Builder;
import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.model.Input;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
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

    final boolean ignoreCase;
    private final LookupPreprocessor lookupPreprocessor;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this(ignoreCase, LookupPreprocessorFactory.identity());
    }

    public TrieMapRulesCollectionBuilder(boolean ignoreCase, final LookupPreprocessor lookupPreprocessor) {
        this.ignoreCase = ignoreCase;
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
        
        switch (inputTerms.size()) {
        
        case 0: {
            if (!(input.isRequiresLeftBoundary() && input.isRequiresRightBoundary())) {
                throw new IllegalArgumentException("Empty input!");
            }

            final ComparableCharSequence seq = new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD,
                    TrieMapRulesCollection.BOUNDARY_WORD);
            final States<InstructionsSupplier> states = map.get(seq);
            final State<InstructionsSupplier> state = states.getStateForCompleteSequence();
            if (state.value != null) {
                state.value.merge(instructionsSupplier);
            } else {
                map.put(seq, instructionsSupplier);
            }
            
        }
        break;
        
        case 1: {

            final Term term = inputTerms.get(0);
            
            boolean isPrefix = term instanceof PrefixTerm;
            
            for (ComparableCharSequence seq: term.getCharSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.isRequiresLeftBoundary(), input.isRequiresRightBoundary());

                final States<InstructionsSupplier> states = map.get(seq);
                
                if (isPrefix) {
                    boolean added = false;

                    final List<State<InstructionsSupplier>> prefixes = states.getPrefixes();
                    
                    if (prefixes != null) {
                        for (final State<InstructionsSupplier> state : prefixes) {
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
        break;
        
        default:

            final Term lastTerm = inputTerms.get(inputTerms.size() -1);
            final boolean isPrefix = lastTerm instanceof PrefixTerm;
            for (ComparableCharSequence seq : input.getInputSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.isRequiresLeftBoundary(), input.isRequiresRightBoundary());

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


    }
    
    ComparableCharSequence applyBoundaries(final ComparableCharSequence seq, final boolean requiresLeftBoundary,
                                           final boolean requiresRightBoundary) {
        if (requiresLeftBoundary == requiresRightBoundary) {
            if (requiresLeftBoundary) {
                return new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD, seq, TrieMapRulesCollection.BOUNDARY_WORD);
            } else {
                return seq;
            }
        } else if (requiresLeftBoundary) {
            return new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD, seq);
        } else {
            return new CompoundCharSequence(" ", seq, TrieMapRulesCollection.BOUNDARY_WORD);
        }
    }
    

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#build()
     */
    @Override
    public RulesCollection build() {
        return new TrieMapRulesCollection(map, ignoreCase);
    }

    @Override
    public TrieMap<InstructionsSupplier> getTrieMap() {
        return map;
    }

}
