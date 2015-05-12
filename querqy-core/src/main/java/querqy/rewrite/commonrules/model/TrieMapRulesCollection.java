/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import querqy.CompoundCharSequence;
import querqy.model.InputSequenceElement;
import querqy.model.Term;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollection implements RulesCollection {
    
    public static final String BOUNDARY_WORD = "\u0002";
    
    final TrieMap<List<Instructions>> trieMap;
    final boolean ignoreCase;
    
    public TrieMapRulesCollection(TrieMap<List<Instructions>> trieMap, boolean ignoreCase) {
        if (trieMap == null) {
            throw new IllegalArgumentException("trieMap must not be null");
        }
        this.trieMap = trieMap;
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollection#getRewriteActions(querqy.rewrite.commonrules.model.PositionSequence)
     */
    @Override
    public List<Action> getRewriteActions(PositionSequence<InputSequenceElement> sequence) {
        List<Action> result = new ArrayList<>();
        if (sequence.isEmpty()) {
            return result;
        }

        // We have a list of terms (resulting from DisMax alternatives) per
        // position. We now find all the combinations of terms in different 
        // positions and look them up as rules input in the dictionary
        // LinkedList<List<Term>> positions = sequence.getPositions();
        if (sequence.size() == 1) {
            for (Term term : new ClassFilter<>(sequence.getFirst(), Term.class)) {
                
                States<List<Instructions>> states = trieMap.get(term.toCharSequenceWithField(ignoreCase));
                
                State<List<Instructions>> stateExactMatch = states.getStateForCompleteSequence();
                if (stateExactMatch.isFinal() && stateExactMatch.value != null) {
                    result.add(new Action(stateExactMatch.value, new TermMatches(new TermMatch(term)), 0, 1));
                }
                
                List<State<List<Instructions>>> statesForPrefixes = states.getPrefixes();
                if (statesForPrefixes != null) {
                    for (State<List<Instructions>> stateForPrefix: statesForPrefixes) {
                        
                        if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                            result.add(
                                    new Action(stateForPrefix.value, 
                                            new TermMatches(
                                                    new TermMatch(term, 
                                                            true, 
                                                            term.subSequence(stateForPrefix.index + 1, term.length()))), 
                                                0, 1));
                        }
                    }
                }
                
            }
        } else {

            List<Prefix<List<Instructions>>> prefixes = new LinkedList<>();
            List<Prefix<List<Instructions>>> newPrefixes = new LinkedList<>();

            int pos = 0;

            for (List<InputSequenceElement> position : sequence) {
                
                boolean anyTermAtPosition = false;

                for (InputSequenceElement element : position) {
                    
                    boolean isTerm = element instanceof Term;
                    anyTermAtPosition |= isTerm;
                    
                    CharSequence charSequenceForLookup = null;
                    if (isTerm) {
                        charSequenceForLookup = ((Term) element).toCharSequenceWithField(ignoreCase);
                    } else if (element instanceof InputBoundary) {
                        charSequenceForLookup = BOUNDARY_WORD;
                    } else {
                        throw new IllegalArgumentException("Cannot handle type of element in sequence " + element);
                    }

                    // combine term with prefixes (= sequences of terms) that brought us here
                    for (Prefix<List<Instructions>> prefix : prefixes) {
                        
                        States<List<Instructions>> states = trieMap.get(
                                new CompoundCharSequence(null, " ", charSequenceForLookup), prefix.stateInfo);
                        
                        int ofs = isTerm ? 1 : 0;
                        
                        // exact matches 
                        State<List<Instructions>> stateExactMatch = states.getStateForCompleteSequence();
                        if (stateExactMatch.isKnown()) {
                            if (stateExactMatch.isFinal()) {
                                 TermMatches matches = new TermMatches(prefix.matches);
                                 if (isTerm) matches.add(new TermMatch((Term) element));
                                 result.add(new Action( stateExactMatch.value, matches, pos - matches.size() + ofs, pos + ofs));
                            }
                            Prefix<List<Instructions>> newPrefix = new Prefix<List<Instructions>>(prefix, stateExactMatch);
                            if (isTerm) {
                                newPrefix.addTerm(new TermMatch((Term) element));
                            }
                            newPrefixes.add(newPrefix);
                            
                        }
                        
                        // matches for prefixes (= beginnings of terms)
                        List<State<List<Instructions>>> statesForPrefixes = states.getPrefixes();
                        if (statesForPrefixes != null) {
                            for (State<List<Instructions>> stateForPrefix: statesForPrefixes) {
                                
                                if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                    TermMatches matches = new TermMatches(prefix.matches);
                                    if (isTerm) {
                                        Term term = (Term) element;
                                        matches.add(
                                            new TermMatch(term, 
                                                    true, 
                                                    term.subSequence(stateForPrefix.index + 1, term.length())));
                                    }
                                    
                                    result.add(new Action( stateForPrefix.value, matches, pos - matches.size() + ofs, pos + ofs));
                                }
                                
                                // TODO: continue with next match after prefix match
                            }
                        }
                    }

                    // now see whether the term matches on its own...
                    States<List<Instructions>> states = trieMap.get(charSequenceForLookup);
                    
                    State<List<Instructions>> stateExactMatch = states.getStateForCompleteSequence();
                    if (stateExactMatch.isKnown()) {
                        if (stateExactMatch.isFinal()) {
                            // we do not let match the boundary on its own:
                            if (isTerm) {
                                result.add(new Action( stateExactMatch.value, new TermMatches(new TermMatch((Term) element)), pos, pos + 1));
                            }
                        }
                        // ... and save it as a prefix to the following term
                        Prefix<List<Instructions>> newPrefix = isTerm
                                ? new Prefix<List<Instructions>>(new TermMatch((Term) element), stateExactMatch)
                                : new Prefix<List<Instructions>>(stateExactMatch);
                        newPrefixes.add(new Prefix<List<Instructions>>(newPrefix, stateExactMatch));
                    }
                    
                    List<State<List<Instructions>>> statesForPrefixes = states.getPrefixes();
                    if (statesForPrefixes != null) {
                        for (State<List<Instructions>> stateForPrefix: statesForPrefixes) {
                            if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                if (isTerm) {
                                    Term term = (Term) element;
                                    result.add(new Action( stateForPrefix.value, new TermMatches(new TermMatch(term, true, term.subSequence(stateForPrefix.index + 1, term.length()))), pos, pos + 1));
                                    // TODO: continue with next match after prefix match
                                }
                            }
                        }
                    }

                }

                prefixes = newPrefixes;
                newPrefixes = new LinkedList<>();

                if (anyTermAtPosition) {
                    pos++;
                }
            }

        }

        return result;    
    }
    
    @Override
    public Set<Instruction> getInstructions() {
        
        Set<Instruction> result = new HashSet<Instruction>();
        
        for (List<Instructions> instructionsList: trieMap) {
            for (Instructions instructions: instructionsList) {
                result.addAll(instructions);
            }
        }
        
        return result;
    }

    
    CharSequence getCharSequenceForLookup(InputSequenceElement element) {
        if (element instanceof InputBoundary) {
            return BOUNDARY_WORD;
        } else if (element instanceof Term) {
            return ((Term) element).toCharSequenceWithField(ignoreCase);
        } else {
            throw new IllegalArgumentException("Cannot handle sequence element type: " + element.getClass().getName());
        }
    }
    
    public static class Prefix<T> {
        State<T> stateInfo;
        List<TermMatch> matches;

        public Prefix(Prefix<T> prefix, TermMatch match, State<T> stateInfo) {
            matches = new LinkedList<>(prefix.matches);
            addTerm(match);
            this.stateInfo = stateInfo;
        }
        
        public Prefix(Prefix<T> prefix, State<T> stateInfo) {
            matches = new LinkedList<>(prefix.matches);
            this.stateInfo = stateInfo;
        }

        public Prefix(TermMatch match, State<T> stateInfo) {
            matches = new LinkedList<>();
            matches.add(match);
            this.stateInfo = stateInfo;
        }
        
        public Prefix(State<T> stateInfo) {
            matches = new LinkedList<>();
            this.stateInfo = stateInfo;
        }


        private void addTerm(TermMatch term) {
            matches.add(term);
        }

     }

}
