package querqy.rewrite.commonrules.model;

import querqy.CompoundCharSequence;
import querqy.model.InputSequenceElement;
import querqy.model.Term;
import querqy.rewrite.commonrules.Properties;
import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.util.*;


/**
 * @author René Kriegler, @renekrie
 */
public class TrieMapRulesPropertiesCollection implements RulesCollection {

    public static final String BOUNDARY_WORD = "\u0002";

    final TrieMap<List<Properties>> trieMap;
    final boolean ignoreCase;

    public TrieMapRulesPropertiesCollection(TrieMap<List<Properties>> trieMap, boolean ignoreCase) {
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
    public List<Action> getRewriteActions(final PositionSequence<InputSequenceElement> sequence) {
        final List<Action> result = new ArrayList<>();
        if (sequence.isEmpty()) {
            return result;
        }


        // We have a list of terms (resulting from DisMax alternatives) per
        // position. We now find all the combinations of terms in different
        // positions and look them up as rules input in the dictionary
        // LinkedList<List<Term>> positions = sequence.getPositions();
        if (sequence.size() == 1) {
            for (final Term term : new ClassFilter<>(sequence.getFirst(), Term.class)) {

                final States<List<Properties>> states = trieMap
                        .get(term.toCharSequenceWithField(ignoreCase));

                final State<List<Properties>> stateExactMatch = states.getStateForCompleteSequence();
                if (stateExactMatch.isFinal() && stateExactMatch.value != null) {
                    result.add(
                            new Action(stateExactMatch.value, new TermMatches(new TermMatch(term)), 0, 1, true));
                }

                final List<State<List<Properties>>> statesForPrefixes = states.getPrefixes();
                if (statesForPrefixes != null) {
                    for (final State<List<Properties>> stateForPrefix : statesForPrefixes) {

                        if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                            result.add(
                                    new Action(stateForPrefix.value,
                                            new TermMatches(
                                                    new TermMatch(term,
                                                            true,
                                                            term.subSequence(stateForPrefix.index + 1, term.length()))),
                                            0, 1, true));
                        }
                    }
                }

            }
        } else {

            List<Prefix<List<Properties>>> prefixes = new LinkedList<>();
            List<Prefix<List<Properties>>> newPrefixes = new LinkedList<>();

            int pos = 0;

            for (final List<InputSequenceElement> position : sequence) {

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
                        throw new IllegalArgumentException(
                                "Cannot handle type of element in sequence " + element);
                    }

                    // combine term with prefixes (= sequences of terms) that brought us here
                    for (final Prefix<List<Properties>> prefix : prefixes) {

                        final States<List<Properties>> states = trieMap.get(
                                new CompoundCharSequence(null, " ", charSequenceForLookup), prefix.stateInfo);

                        final int ofs = isTerm ? 1 : 0;

                        // exact matches
                        final State<List<Properties>> stateExactMatch = states.getStateForCompleteSequence();
                        if (stateExactMatch.isKnown()) {
                            if (stateExactMatch.isFinal()) {
                                final TermMatches matches = new TermMatches(prefix.matches);
                                if (isTerm) {
                                    matches.add(new TermMatch((Term) element));
                                }
                                result.add(new Action(stateExactMatch.value, matches, pos - matches.size() + ofs,
                                        pos + ofs, true));
                            }
                            final Prefix<List<Properties>> newPrefix = new Prefix<List<Properties>>(prefix,
                                    stateExactMatch);
                            if (isTerm) {
                                newPrefix.addTerm(new TermMatch((Term) element));
                            }
                            newPrefixes.add(newPrefix);

                        }

                        // matches for prefixes (= beginnings of terms)
                        final List<State<List<Properties>>> statesForPrefixes = states.getPrefixes();
                        if (statesForPrefixes != null) {
                            for (final State<List<Properties>> stateForPrefix : statesForPrefixes) {

                                if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                    final TermMatches matches = new TermMatches(prefix.matches);
                                    if (isTerm) {
                                        final Term term = (Term) element;
                                        matches.add(
                                                new TermMatch(term,
                                                        true,
                                                        term.subSequence(stateForPrefix.index + 1, term.length())));
                                    }

                                    result.add(new Action(stateForPrefix.value, matches, pos - matches.size() + ofs,
                                            pos + ofs, true));
                                }

                                // TODO: continue with next match after prefix match
                            }
                        }
                    }

                    // now see whether the term matches on its own...
                    final States<List<Properties>> states = trieMap.get(charSequenceForLookup);

                    final State<List<Properties>> stateExactMatch = states.getStateForCompleteSequence();
                    if (stateExactMatch.isKnown()) {
                        if (stateExactMatch.isFinal()) {
                            // we do not let match the boundary on its own:
                            if (isTerm) {
                                result.add(new Action(stateExactMatch.value,
                                        new TermMatches(new TermMatch((Term) element)), pos, pos + 1, true));
                            }
                        }
                        // ... and save it as a prefix to the following term
                        final Prefix<List<Properties>> newPrefix = isTerm
                                ? new Prefix<>(new TermMatch((Term) element), stateExactMatch)
                                : new Prefix<>(stateExactMatch);
                        newPrefixes.add(new Prefix<>(newPrefix, stateExactMatch));
                    }

                    final List<State<List<Properties>>> statesForPrefixes = states.getPrefixes();
                    if (statesForPrefixes != null) {
                        for (final State<List<Properties>> stateForPrefix : statesForPrefixes) {
                            if (stateForPrefix.isFinal() && stateForPrefix.value != null) {
                                if (isTerm) {
                                    final Term term = (Term) element;
                                    result.add(new Action(stateForPrefix.value, new TermMatches(
                                            new TermMatch(term, true,
                                                    term.subSequence(stateForPrefix.index + 1, term.length()))), pos, pos + 1,
                                            true));
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

        final Set<Instruction> result = new HashSet<Instruction>();

        for (List<Properties> propertiesList : trieMap) {
            for (Properties properties : propertiesList) {
                result.addAll(properties.getInstructions());
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

