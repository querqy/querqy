/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import querqy.CompoundCharSequence;
import querqy.model.Term;
import querqy.trie.State;
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
                
                State<List<Instructions>> stateInfo = trieMap.get(term.toCharSequenceWithField(ignoreCase));
                if (stateInfo.isKnown && stateInfo.value != null) {
                    result.add(new Action(stateInfo.value, Arrays.asList(term), 0, 1));
                }
                
            }
        } else {

            List<Prefix<List<Instructions>>> prefixes = new LinkedList<>();
            List<Prefix<List<Instructions>>> newPrefixes = new LinkedList<>();

            int pos = 0;

            for (List<Term> position : sequence) {

                for (Term term : position) {

                    for (Prefix<List<Instructions>> prefix : prefixes) {
                        
                        State<List<Instructions>> stateInfo = trieMap.get(
                                new CompoundCharSequence(null, " ", term.toCharSequenceWithField(ignoreCase)), prefix.stateInfo);
                        
                        if (stateInfo.isKnown()) {
                            if (stateInfo.isFinal()) {
                                 List<Term> terms = new LinkedList<>(prefix.terms);
                                 terms.add(term);
                                 result.add(new Action( stateInfo.value, terms, pos - terms.size() + 1, pos + 1));
                            }
                            newPrefixes.add(new Prefix<List<Instructions>>(prefix, term, stateInfo));
                        }
                    }

                    State<List<Instructions>> stateInfo = trieMap.get(term.toCharSequenceWithField(ignoreCase));
                    if (stateInfo.isKnown()) {
                        if (stateInfo.isFinal()) {
                            result.add(new Action(stateInfo.value, Arrays.asList(term), pos, pos + 1));
                        }
                        newPrefixes.add(new Prefix<List<Instructions>>(term, stateInfo));
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
        List<Term> terms;

        public Prefix(Prefix<T> prefix, Term term, State<T> stateInfo) {
           terms = new LinkedList<>(prefix.terms);
           addTerm(term);
           this.stateInfo = stateInfo;
        }

        public Prefix(Term term, State<T> stateInfo) {
           terms = new LinkedList<>();
           terms.add(term);
           this.stateInfo = stateInfo;
        }

        private void addTerm(Term term) {
           terms.add(term);
        }

     }

}
