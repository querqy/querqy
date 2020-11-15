/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInputLiteral;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {
    
    final TrieMap<InstructionsSupplier> map = new TrieMap<>();
    private final Set<Object> seenInstructionIds = new HashSet<>();

    final boolean ignoreCase;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#addRule(querqy.rewrite.commonrules.model.Input, querqy.rewrite.commonrules.model.Instructions)
     */
    @Override
    public void addRule(final Input input, final Instructions instructions) {
        this.addOrMergeInstructionsSupplier(input, new InstructionsSupplier().addInstructions(instructions));
    }

    @Override
    public void addRule(final Input input, final BooleanInputLiteral literal) {
        this.addOrMergeInstructionsSupplier(input, new InstructionsSupplier().setLiteral(literal));
    }

    public void addOrMergeInstructionsSupplier(final Input input, final InstructionsSupplier instructionsSupplier) {
        final List<Term> inputTerms = input.getInputTerms();
        
        switch (inputTerms.size()) {
        
        case 0: {
            if (!(input.requiresLeftBoundary && input.requiresRightBoundary)) {
                throw new IllegalArgumentException("Empty input!");
            }

            final ComparableCharSequence seq = new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD, TrieMapRulesCollection.BOUNDARY_WORD);
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
                
                seq = applyBoundaries(seq, input.requiresLeftBoundary, input.requiresRightBoundary);

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
            final Term lastTerm = input.inputTerms.get(input.inputTerms.size() -1);
            final boolean isPrefix = lastTerm instanceof PrefixTerm;
            for (ComparableCharSequence seq : input.getInputSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.requiresLeftBoundary, input.requiresRightBoundary);

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
    
    ComparableCharSequence applyBoundaries(ComparableCharSequence seq, boolean requiresLeftBoundary, boolean requiresRightBoundary) {
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

}
