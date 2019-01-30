/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.LinkedList;
import java.util.List;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.rewrite.commonrules.Properties;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollectionBuilder implements RulesCollectionBuilder {
    
    final TrieMap<List<Instructions>> map = new TrieMap<>();
    
    final boolean ignoreCase;
    
    public TrieMapRulesCollectionBuilder(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }


    @Override
    public void addRule(Input input, Properties properties) {
       this.addRule(input, properties.getInstructions());
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#addRule(querqy.rewrite.commonrules.model.Input, querqy.rewrite.commonrules.model.Instructions)
     */
    @Override
    public void addRule(Input input, Instructions instructions) {
        
        List<Term> inputTerms = input.getInputTerms();
        
        switch (inputTerms.size()) {
        
        case 0: {
            if (!(input.requiresLeftBoundary && input.requiresRightBoundary)) {
                throw new IllegalArgumentException("Empty input!");
            }
            
            ComparableCharSequence seq = new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD, TrieMapRulesCollection.BOUNDARY_WORD);
            States<List<Instructions>> states = map.get(seq);
            State<List<Instructions>> state = states.getStateForCompleteSequence();
            if (state.value != null) {
                state.value.add(instructions);
            } else {
                List<Instructions> instructionsList = new LinkedList<>();
                instructionsList.add(instructions);
                map.put(seq, instructionsList);
            }
            
        }
        break;
        
        case 1: {
            
            Term term = inputTerms.get(0);
            
            boolean isPrefix = term instanceof PrefixTerm;
            
            for (ComparableCharSequence seq: term.getCharSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.requiresLeftBoundary, input.requiresRightBoundary);
                
                States<List<Instructions>> states = map.get(seq);
                
                if (isPrefix) {
                    boolean added = false;
                    
                    List<State<List<Instructions>>> prefixes = states.getPrefixes();
                    
                    if (prefixes != null) {
                        for (State<List<Instructions>> state: prefixes) {
                            if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                                state.value.add(instructions);
                                added = true;
                                break;
                            }
                            
                        }
                    }
                    
                    if (!added) {
                        List<Instructions> instructionsList = new LinkedList<>();
                        instructionsList.add(instructions);
                        map.putPrefix(seq, instructionsList);
                    }
                
                } else {
                    State<List<Instructions>> state = states.getStateForCompleteSequence();
                    if (state.value != null) {
                        state.value.add(instructions);
                    } else {
                        List<Instructions> instructionsList = new LinkedList<>();
                        instructionsList.add(instructions);
                        map.put(seq, instructionsList);
                    }
                    
                }
            }
        }
        break;
        
        default:
            Term lastTerm = input.inputTerms.get(input.inputTerms.size() -1);
            boolean isPrefix = lastTerm instanceof PrefixTerm;
            for (ComparableCharSequence seq : input.getInputSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.requiresLeftBoundary, input.requiresRightBoundary);
                
                States<List<Instructions>> states = map.get(seq);
                
                if (isPrefix) { 
                    
                    boolean added = false;
                    
                    List<State<List<Instructions>>> prefixes = states.getPrefixes();
                    
                    if (prefixes != null) {
                        for (State<List<Instructions>> state: prefixes) {
                            if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                                state.value.add(instructions);
                                added = true;
                                break;
                            }
                            
                        }
                    }
                    
                    if (!added) {
                        List<Instructions> instructionsList = new LinkedList<>();
                        instructionsList.add(instructions);
                        map.putPrefix(seq, instructionsList);
                    }
                } else {
                    State<List<Instructions>> state = states.getStateForCompleteSequence();
                    if (state.value != null) {
                        state.value.add(instructions);
                    } else {
                        List<Instructions> instructionsList = new LinkedList<>();
                        instructionsList.add(instructions);
                        map.put(seq, instructionsList);
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
