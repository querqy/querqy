/**
 * 
 */
package querqy.rewrite.commonrules.model;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.LinkedList;
import java.util.List;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesPropertiesCollectionBuilder implements RulesCollectionBuilder {
    
    final TrieMap<List<Properties>> map = new TrieMap<>();
    
    final boolean ignoreCase;
    
    public TrieMapRulesPropertiesCollectionBuilder(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollectionBuilder#addRule(querqy.rewrite.commonrules.model.Input, querqy.rewrite.commonrules.model.Instructions)
     */
    @Override
    public void addRule(Input input, Properties properties) {
        
        List<Term> inputTerms = input.getInputTerms();
        
        switch (inputTerms.size()) {
        
        case 0: {
            if (!(input.requiresLeftBoundary && input.requiresRightBoundary)) {
                throw new IllegalArgumentException("Empty input!");
            }
            
            ComparableCharSequence seq = new CompoundCharSequence(" ", TrieMapRulesCollection.BOUNDARY_WORD, TrieMapRulesCollection.BOUNDARY_WORD);
            States<List<Properties>> states = map.get(seq);
            State<List<Properties>> state = states.getStateForCompleteSequence();
            if (state.value != null) {
                state.value.add(properties);
            } else {
                List<Properties> propertiesList = new LinkedList<>();
                propertiesList.add(properties);
                map.put(seq, propertiesList);
            }
            
        }
        break;
        
        case 1: {
            
            Term term = inputTerms.get(0);
            
            boolean isPrefix = term instanceof PrefixTerm;
            
            for (ComparableCharSequence seq: term.getCharSequences(ignoreCase)) {
                
                seq = applyBoundaries(seq, input.requiresLeftBoundary, input.requiresRightBoundary);
                
                States<List<Properties>> states = map.get(seq);
                
                if (isPrefix) {
                    boolean added = false;
                    
                    List<State<List<Properties>>> prefixes = states.getPrefixes();
                    
                    if (prefixes != null) {
                        for (State<List<Properties>> state: prefixes) {
                            if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                                state.value.add(properties);
                                added = true;
                                break;
                            }
                            
                        }
                    }
                    
                    if (!added) {
                        List<Properties> propertiesList = new LinkedList<>();
                        propertiesList.add(properties);
                        map.putPrefix(seq, propertiesList);
                    }
                
                } else {
                    State<List<Properties>> state = states.getStateForCompleteSequence();
                    if (state.value != null) {
                        state.value.add(properties);
                    } else {
                        List<Properties> propertiesList = new LinkedList<>();
                        propertiesList.add(properties);
                        map.put(seq, propertiesList);
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
                
                States<List<Properties>> states = map.get(seq);
                
                if (isPrefix) { 
                    
                    boolean added = false;
                    
                    List<State<List<Properties>>> prefixes = states.getPrefixes();
                    
                    if (prefixes != null) {
                        for (State<List<Properties>> state: prefixes) {
                            if (state.isFinal() && state.index == (seq.length() - 1) && state.value != null) {
                                state.value.add(properties);
                                added = true;
                                break;
                            }
                            
                        }
                    }
                    
                    if (!added) {
                        List<Properties> propertiesList = new LinkedList<>();
                        propertiesList.add(properties);
                        map.putPrefix(seq, propertiesList);
                    }
                } else {
                    State<List<Properties>> state = states.getStateForCompleteSequence();
                    if (state.value != null) {
                        state.value.add(properties);
                    } else {
                        List<Properties> propertiesList = new LinkedList<>();
                        propertiesList.add(properties);
                        map.put(seq, propertiesList);
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
        return new TrieMapRulesPropertiesCollection(map, ignoreCase);
    }

}
