package querqy.regex;

import querqy.regex.Symbol.AlternationSymbol;
import querqy.regex.Symbol.CharClassSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NFACompiler<T> {

    public NFAFragment<T> compileSequence(final List<Symbol> symbols) {

        NFAFragment<T> fragment = null;

        for (final Symbol symbol: symbols) {
            final NFAFragment<T> piece = compileOne(symbol);
            fragment = concatenate(fragment, piece);
        }

        return (fragment == null) ? NFAFragment.empty() : fragment;
    }

    private NFAFragment<T> compileAlternation(final AlternationSymbol alt) {
        NFAState<T> start = new NFAState<>();
        final Set<NFAState<T>> accepts = new HashSet<>();

        for (final List<Symbol> branch: alt.alternatives) {
            final NFAFragment<T> frag = compileSequence(branch);
            start.addEpsilon(frag.start);
            accepts.addAll(frag.accepts);
        }

        return new NFAFragment<T>(start, accepts);
    }

    private NFAFragment<T> compileOne(final Symbol s) {
        if (s instanceof AlternationSymbol a) {
            return compileAlternation(a);
        }
        final NFAFragment<T> base = makeBaseFragment(s);
        return applyQuantifier(base, s.getMinOccur(), s.getMaxOccur(), s instanceof GroupSymbol ? ((GroupSymbol)s).getGroupIndex() : -1);
    }

    private NFAFragment<T> makeBaseFragment(final Symbol s) {
        if (s instanceof GroupSymbol gs) {
            final NFAFragment<T> inner = compileSequence(gs.getChildren());
            attachGroupMarkers(inner, gs.getGroupIndex());
            return inner;
        }

        final NFAState<T> start = new NFAState<>();
        final NFAState<T> accept = new NFAState<>();

        if (s instanceof CharSymbol cs) {
            start.addCharTransition(cs.getValue(), accept);
        } else if (s instanceof CharClassSymbol cc) {
            start.charClassTransitions.add(new CharClassTransition<>(cc::matches, accept));
        }
        return new NFAFragment<>(start, Set.of(accept));
    }

    private NFAFragment<T> applyQuantifier(final NFAFragment<T> frag, final int min, final int max,
                                           final int groupIdx) {
        if (min==1 && max==1) {
            return cloneFragment(frag);
        }

        NFAFragment<T> result;
        if (min == 0) {
            result = NFAFragment.empty();
        } else {
            result = null;
            for (int i = 0; i < min; i++) {
                final NFAFragment<T> copy = cloneFragment(frag);
                if (groupIdx >= 0) {
                    attachGroupMarkers(copy, groupIdx);
                }
                result = concatenate(result, copy);
            }
        }


        // optional part
        if (max == Integer.MAX_VALUE) {
            final NFAFragment<T> copy = cloneFragment(frag);
            if (groupIdx >= 0) {
                attachGroupMarkers(copy, groupIdx);
            }

            for (final NFAState<T> a: result.accepts) {
                a.addEpsilon(copy.start);
            }
            for (final NFAState<T> a: copy.accepts) {
                a.addEpsilon(copy.start);
            }

            final Set<NFAState<T>> accepts = new HashSet<>();
            accepts.addAll(result.accepts);  // allow stopping at min
            accepts.addAll(copy.accepts);    // allow stopping after more

            return new NFAFragment<T>(result.start, accepts);

        }

        final Set<NFAState<T>> allAccepts = new HashSet<>(result.accepts);

        final int optional = max - min;
        NFAFragment<T> current = result;
        for (int i = 0; i < optional; i++) {
            final NFAFragment<T> copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            // allow skipping this repetition
            for (final NFAState<T> a: current.accepts) {
                a.addEpsilon(copy.start);
            }

            current = copy;
            allAccepts.addAll(copy.accepts);

            result = new NFAFragment<>(result.start, allAccepts);
        }

        return result;

    }


    private static <T> void attachGroupMarkers(final NFAFragment<T> frag, final int groupIndex) {
        frag.start.addGroupStart(groupIndex);
        for (final NFAState<T> a: frag.accepts) {
            a.addGroupEnd(groupIndex);
        }
    }

    private static <T> NFAFragment<T> concatenate(final NFAFragment<T> a, final NFAFragment<T> b) {
        if (a == null) return b;
        for (final NFAState<T> as : a.accepts) as.addEpsilon(b.start);
        return new NFAFragment<>(a.start, b.accepts);
    }

    private static <T> NFAFragment<T> cloneFragment(final NFAFragment<T> original) {
        final Map<NFAState<T>, NFAState<T>> clones = new IdentityHashMap<>();
        final NFAState<T> newStart = cloneState(original.start, clones);
        final Set<NFAState<T>> newAccepts = new HashSet<>();
        for (final NFAState<T> a: original.accepts) {
            newAccepts.add(clones.get(a));
        }
        return new NFAFragment<>(newStart, newAccepts);
    }

    private static <T> NFAState<T> cloneState(final NFAState<T> state, final Map<NFAState<T>, NFAState<T>> clones) {
        if (clones.containsKey(state)) return clones.get(state);
        final NFAState<T> copy = new NFAState<T>();
        copy.groupStarts.addAll(state.groupStarts);
        copy.groupEnds.addAll(state.groupEnds);
        clones.put(state, copy);

        for (final Map.Entry<Character, Set<NFAState<T>>> e: state.charTransitions.entrySet()) {
            for (final NFAState<T> target: e.getValue()) {
                copy.addCharTransition(e.getKey(), cloneState(target, clones));
            }
        }

        for (final CharClassTransition<T> t: state.charClassTransitions) {
            copy.charClassTransitions.add(new CharClassTransition<>(t.predicate(), cloneState(t.target(), clones)));
        }

        for (final NFAState<T> t: state.epsilonTransitions) {
            copy.epsilonTransitions.add(cloneState(t, clones));
        }

        return copy;
    }

}
