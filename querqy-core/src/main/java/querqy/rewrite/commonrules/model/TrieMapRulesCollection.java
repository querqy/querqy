/**
 * 
 */
package querqy.rewrite.commonrules.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import querqy.CompoundCharSequence;
import querqy.model.InputSequenceElement;
import querqy.model.Term;
import querqy.rewrite.commonrules.select.TopRewritingActionCollector;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class TrieMapRulesCollection implements RulesCollection {
    
    public static final String BOUNDARY_WORD = "\u0002";
    
    final TrieMap<InstructionsSupplier> trieMap;
    final boolean ignoreCase;
    
    public TrieMapRulesCollection(TrieMap<InstructionsSupplier> trieMap, boolean ignoreCase) {
        if (trieMap == null) {
            throw new IllegalArgumentException("trieMap must not be null");
        }
        this.trieMap = trieMap;
        this.ignoreCase = ignoreCase;
    }

    /* (non-Javadoc)
     * @see querqy.rewrite.commonrules.model.RulesCollection#collectRewriteActions(querqy.rewrite.commonrules.model.PositionSequence, querqy.rewrite.commonrules.select.FlatTopRewritingActionCollector)
     */
    @Override
    public void collectRewriteActions(final PositionSequence<InputSequenceElement> sequence,
                                      final TopRewritingActionCollector collector) {

        if (sequence.isEmpty()) {
            return;
        }

        // We have a list of terms (resulting from DisMax alternatives) per
        // position. We now find all the combinations of terms in different 
        // positions and look them up as rules input in the dictionary
        // LinkedList<List<Term>> positions = sequence.getPositions();
        if (sequence.size() == 1) {
            for (final Term term : new ClassFilter<>(sequence.getFirst(), Term.class)) {

                final States<InstructionsSupplier> states = trieMap.get(term.toCharSequenceWithField(ignoreCase));

                final State<InstructionsSupplier> stateExactMatch = states.getStateForCompleteSequence();
                if (stateExactMatch.isFinal() && stateExactMatch.value != null) {

                    collector.collect(stateExactMatch.value,
                            instructions -> new Action(instructions, new TermMatches(new TermMatch(term)), 0, 1));

                }

                final List<State<InstructionsSupplier>> statesForPrefixes = states.getPrefixes();
                if (statesForPrefixes != null) {
                    for (final State<InstructionsSupplier> stateForPrefix: statesForPrefixes) {
                        
                        if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                            collector.collect(stateForPrefix.value,
                                    instructions -> new Action(instructions, new TermMatches(
                                            new TermMatch(term, true,
                                            term.subSequence(stateForPrefix.index + 1, term.length()))), 0, 1));

                        }
                    }
                }
                
            }
        } else {

            List<Prefix<InstructionsSupplier>> prefixes = new LinkedList<>();
            List<Prefix<InstructionsSupplier>> newPrefixes = new LinkedList<>();

            int pos = 0;

            for (final List<InputSequenceElement> position : sequence) {

                final int pos1 = pos;
                
                boolean anyTermAtPosition = false;

                for (final InputSequenceElement element : position) {

                    final boolean isTerm = element instanceof Term;
                    anyTermAtPosition |= isTerm;

                    final CharSequence charSequenceForLookup;
                    if (isTerm) {
                        charSequenceForLookup = ((Term) element).toCharSequenceWithField(ignoreCase);
                    } else if (element instanceof InputBoundary) {
                        charSequenceForLookup = BOUNDARY_WORD;
                    } else {
                        throw new IllegalArgumentException("Cannot handle type of element in sequence " + element);
                    }

                    // combine term with prefixes (= sequences of terms) that brought us here
                    for (final Prefix<InstructionsSupplier> prefix : prefixes) {

                        final States<InstructionsSupplier> states = trieMap.get(
                                new CompoundCharSequence(null, " ", charSequenceForLookup), prefix.stateInfo);

                        final int ofs = isTerm ? 1 : 0;
                        
                        // exact matches 
                        final State<InstructionsSupplier> stateExactMatch = states.getStateForCompleteSequence();
                        if (stateExactMatch.isKnown()) {
                            if (stateExactMatch.isFinal()) {
                                final int start;
                                if (isTerm) {
                                    start = pos - (prefix.matches.size() + 1) + ofs;
                                } else {
                                    start = pos - prefix.matches.size() + ofs;
                                }

                                collector.collect(stateExactMatch.value, instructions -> {
                                    final TermMatches matches = new TermMatches(prefix.matches);
                                    if (isTerm) {
                                        matches.add(new TermMatch((Term) element));
                                    }
                                    return new Action(instructions, matches, start, pos1 + ofs);
                                });

                            }
                            final Prefix<InstructionsSupplier> newPrefix = new Prefix<InstructionsSupplier>(prefix, stateExactMatch);
                            if (isTerm) {
                                newPrefix.addTerm(new TermMatch((Term) element));
                            }
                            newPrefixes.add(newPrefix);
                            
                        }
                        
                        // matches for prefixes (= beginnings of terms)
                        final List<State<InstructionsSupplier>> statesForPrefixes = states.getPrefixes();
                        if (statesForPrefixes != null) {
                            for (final State<InstructionsSupplier> stateForPrefix: statesForPrefixes) {
                                
                                if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                    final int start;
                                    if (isTerm) {
                                        start = pos - (prefix.matches.size() + 1) + ofs;
                                    } else {
                                        start = pos - prefix.matches.size() + ofs;
                                    }


                                    collector.collect(stateForPrefix.value, instructions -> {
                                        final TermMatches matches = new TermMatches(prefix.matches);
                                        if (isTerm) {
                                            final Term term = (Term) element;
                                            matches.add(
                                                    new TermMatch(term,
                                                            true,
                                                            term.subSequence(stateForPrefix.index + 1, term.length())));
                                        }
                                        return new Action(instructions, matches, start, pos1 + ofs);
                                    });

                                }
                                
                                // TODO: continue with next match after prefix match
                            }
                        }
                    }

                    // now see whether the term matches on its own...
                    final States<InstructionsSupplier> states = trieMap.get(charSequenceForLookup);

                    final State<InstructionsSupplier> stateExactMatch = states.getStateForCompleteSequence();
                    if (stateExactMatch.isKnown()) {
                        if (stateExactMatch.isFinal()) {
                            // we do not let match the boundary on its own:
                            if (isTerm) {
                                collector.collect(stateExactMatch.value,
                                        instructions ->
                                                new Action(instructions, new TermMatches(new TermMatch((Term) element)),
                                                        pos1, pos1 + 1));
                            }
                        }
                        // ... and save it as a prefix to the following term
                        final Prefix<InstructionsSupplier> newPrefix = isTerm
                                ? new Prefix<>(new TermMatch((Term) element), stateExactMatch)
                                : new Prefix<>(stateExactMatch);
                        newPrefixes.add(new Prefix<>(newPrefix, stateExactMatch));
                    }

                    final List<State<InstructionsSupplier>> statesForPrefixes = states.getPrefixes();
                    if (statesForPrefixes != null) {
                        for (final State<InstructionsSupplier> stateForPrefix: statesForPrefixes) {
                            if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                if (isTerm) {
                                    collector.collect(stateForPrefix.value, instructions -> {
                                                final Term term = (Term) element;
                                                return new Action(instructions,
                                                        new TermMatches(
                                                                new TermMatch(term, true,
                                                                term.subSequence(stateForPrefix.index + 1,
                                                                        term.length()))), pos1, pos1 + 1);
                                            });
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

    }
    
    @Override
    public Set<Instruction> getInstructions() {

        final Set<Instruction> result = new HashSet<>();
        
        for (InstructionsSupplier instructionsSupplier : trieMap) {
            for (Instructions instructions : instructionsSupplier.getInstructionsList()) {
                result.addAll(instructions);
            }
        }
        
        return result;
    }

    public static class Prefix<T> {
        final State<T> stateInfo;
        final List<TermMatch> matches;

        public Prefix(final Prefix<T> prefix, final TermMatch match, final State<T> stateInfo) {
            matches = new LinkedList<>(prefix.matches);
            addTerm(match);
            this.stateInfo = stateInfo;
        }
        
        public Prefix(final Prefix<T> prefix, final State<T> stateInfo) {
            matches = new LinkedList<>(prefix.matches);
            this.stateInfo = stateInfo;
        }

        public Prefix(final TermMatch match, final State<T> stateInfo) {
            matches = new LinkedList<>();
            matches.add(match);
            this.stateInfo = stateInfo;
        }
        
        public Prefix(final State<T> stateInfo) {
            matches = new LinkedList<>();
            this.stateInfo = stateInfo;
        }


        private void addTerm(final TermMatch term) {
            matches.add(term);
        }

     }

}
