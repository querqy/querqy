/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollection implements RulesCollection {
    
    final TrieMap<List<Instructions>> trieMap;
    final boolean ignoreCase;
    
    public TrieMapRulesCollection(TrieMap<List<Instructions>> trieMap, boolean ignoreCase) {
        this.trieMap = trieMap;
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollection#getRewriteActions(querqy.rewrite.commonrules.model.PositionSequence)
     */
    @Override
    public List<Action> getRewriteActions(PositionSequence<Term> sequence) {
        List<Action> result = new ArrayList<>();
        if (sequence.isEmpty()) {
            return result;
        }

        // We have a list of terms (resulting from DisMax alternatives) per
        // position. We now find all the combinations of terms in different 
        // positions and look them up as rules input in the dictionary
        // LinkedList<List<Term>> positions = sequence.getPositions();
        if (sequence.size() == 1) {
            for (Term term : sequence.getFirst()) {
                
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

            for (List<Term> position : sequence) {

                for (Term term : position) {

                    // combine term with prefixes (= sequences of terms) that brought us here
                    for (Prefix<List<Instructions>> prefix : prefixes) {
                        
                        States<List<Instructions>> states = trieMap.get(
                                new CompoundCharSequence(null, " ", term.toCharSequenceWithField(ignoreCase)), prefix.stateInfo);
                        
                        // exact matches 
                        State<List<Instructions>> stateExactMatch = states.getStateForCompleteSequence();
                        if (stateExactMatch.isKnown()) {
                            if (stateExactMatch.isFinal()) {
                                 TermMatches matches = new TermMatches(prefix.matches);
                                 matches.add(new TermMatch(term));
                                 result.add(new Action( stateExactMatch.value, matches, pos - matches.size() + 1, pos + 1));
                            }
                            newPrefixes.add(new Prefix<List<Instructions>>(prefix, new TermMatch(term), stateExactMatch));
                        }
                        
                        // matches for prefixes (= beginnings of terms)
                        List<State<List<Instructions>>> statesForPrefixes = states.getPrefixes();
                        if (statesForPrefixes != null) {
                            for (State<List<Instructions>> stateForPrefix: statesForPrefixes) {
                                
                                if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                    TermMatches matches = new TermMatches(prefix.matches);
                                    matches.add(
                                            new TermMatch(term, 
                                                    true, 
                                                    term.subSequence(stateForPrefix.index + 1, term.length())));
                                    
                                    result.add(new Action( stateForPrefix.value, matches, pos - matches.size() + 1, pos + 1));
                                }
                                
                                // TODO: continue with next match after prefix match
                            }
                        }
                    }

                    // now see whether the term matches on its own...
                    States<List<Instructions>> states = trieMap.get(term.toCharSequenceWithField(ignoreCase));
                    
                    State<List<Instructions>> stateExactMatch = states.getStateForCompleteSequence();
                    if (stateExactMatch.isKnown()) {
                        if (stateExactMatch.isFinal()) {
                             result.add(new Action( stateExactMatch.value, new TermMatches(new TermMatch(term)), pos, pos + 1));
                        }
                        // ... and save it as a prefix to the following term
                        newPrefixes.add(new Prefix<List<Instructions>>(new TermMatch(term), stateExactMatch));
                    }
                    
                    List<State<List<Instructions>>> statesForPrefixes = states.getPrefixes();
                    if (statesForPrefixes != null) {
                        for (State<List<Instructions>> stateForPrefix: statesForPrefixes) {
                            if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                result.add(new Action( stateForPrefix.value, new TermMatches(new TermMatch(term, true, term.subSequence(stateForPrefix.index + 1, term.length()))), pos, pos + 1));
                             // TODO: continue with next match after prefix match
                            }
                        }
                    }

                }

                prefixes = newPrefixes;
                newPrefixes = new LinkedList<>();

                pos++;
            }

        }

        return result;    
      }
    
    public static class Prefix<T> {
        State<T> stateInfo;
        List<TermMatch> matches;

        public Prefix(Prefix<T> prefix, TermMatch match, State<T> stateInfo) {
            matches = new LinkedList<>(prefix.matches);
            addTerm(match);
            this.stateInfo = stateInfo;
        }

        public Prefix(TermMatch match, State<T> stateInfo) {
            matches = new LinkedList<>();
            matches.add(match);
            this.stateInfo = stateInfo;
        }

        private void addTerm(TermMatch term) {
            matches.add(term);
        }

     }

}
