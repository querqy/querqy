package querqy.regex;

import querqy.regex.Symbol.AlternationSymbol;
import querqy.regex.Symbol.AnyCharSymbol;
import querqy.regex.Symbol.AnyDigitSymbol;
import querqy.regex.Symbol.CharClassSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NFACompiler<T> {

    public NFAFragment<T> compileSequence(final List<Symbol> symbols) {

        NFAFragment<T> fragment = null;

        for (final Symbol symbol: symbols) {
            NFAFragment<T> piece = compileOne(symbol);
            fragment = concatenate(fragment, piece);
        }

        return (fragment == null) ? NFAFragment.empty() : fragment;
    }

    private NFAFragment<T> compileAlternation(AlternationSymbol alt) {
        NFAState<T> start = new NFAState<T>();
        Set<NFAState<T>> accepts = new HashSet<>();

        for (final List<Symbol> branch: alt.alternatives) {
            NFAFragment<T> frag = compileSequence(branch);
            start.addEpsilon(frag.start);
            accepts.addAll(frag.accepts);
        }

        return new NFAFragment<T>(start, accepts);
    }

    private NFAFragment<T> compileOne(Symbol s) {
        if (s instanceof AlternationSymbol a) {
            return compileAlternation(a);
        }
        NFAFragment<T> base = makeBaseFragment(s);
        return applyQuantifier(base, s.getMinOccur(), s.getMaxOccur(), s instanceof GroupSymbol ? ((GroupSymbol)s).getGroupIndex() : -1);
    }

    private NFAFragment<T> makeBaseFragment(final Symbol s) {
        NFAState<T> start = new NFAState<>();
        NFAState<T> accept = new NFAState<>();

        if (s instanceof CharSymbol cs) start.addCharTransition(cs.getValue(), accept);
        else if (s instanceof AnyDigitSymbol) start.addDigitTransition(accept);
        else if (s instanceof AnyCharSymbol) start.anyCharTransitions.add(accept);
        else if (s instanceof CharClassSymbol cc) {
            start.charClassTransitions.add(new CharClassTransition<>(cc::matches, accept));
        } else if (s instanceof GroupSymbol gs) {
            NFAFragment<T> inner = compileSequence(gs.getChildren());
            attachGroupMarkers(inner, gs.getGroupIndex());
            return inner;
        }
        return new NFAFragment<T>(start, Set.of(accept));
    }

    private NFAFragment<T> applyQuantifier(NFAFragment<T> frag, int min, int max, int groupIdx) {
        if (min==1 && max==1) return cloneFragment(frag);

        NFAFragment<T> result;
        if (min == 0) {
            result = NFAFragment.empty();
        } else {
            result = null;
            for (int i = 0; i < min; i++) {
                NFAFragment<T> copy = cloneFragment(frag);
                if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);
                result = concatenate(result, copy);
            }
        }

/*
         if (max == Integer.MAX_VALUE) {
            NFAFragment copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            for (NFAState a : result.accepts) {
                a.addEpsilon(copy.start);
            }
            for (NFAState a : copy.accepts) {
                a.addEpsilon(copy.start);
            }

            return new NFAFragment(result.start, copy.accepts);
        }
         */

        // optional part
        if (max == Integer.MAX_VALUE) {
            NFAFragment<T> copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            for (NFAState<T> a : result.accepts) {
                a.addEpsilon(copy.start);
            }
            for (NFAState<T> a : copy.accepts) {
                a.addEpsilon(copy.start);
            }

            Set<NFAState<T>> accepts = new HashSet<>();
            accepts.addAll(result.accepts);  // allow stopping at min
            accepts.addAll(copy.accepts);    // allow stopping after more

            return new NFAFragment<T>(result.start, accepts);

        }

        Set<NFAState<T>> allAccepts = new HashSet<>(result.accepts);

        int optional = max - min;
        NFAFragment<T> current = result;
        for (int i = 0; i < optional; i++) {
            NFAFragment<T> copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            // allow skipping this repetition
            for (final NFAState<T> a : current.accepts) {
                a.addEpsilon(copy.start);
            }

            current = copy;
            allAccepts.addAll(copy.accepts);

            result = new NFAFragment<T>(result.start, allAccepts);
        }

        return result;

    }


    private static <T> void attachGroupMarkers(final NFAFragment<T> frag, final int groupIndex) {
        frag.start.addGroupStart(groupIndex);
        for (NFAState<T> a : frag.accepts) a.addGroupEnd(groupIndex);
    }

    private static <T> NFAFragment<T> concatenate(final NFAFragment<T> a, final NFAFragment<T> b) {
        if (a == null) return b;
        for (final NFAState<T> as : a.accepts) as.addEpsilon(b.start);
        return new NFAFragment<>(a.start, b.accepts);
    }

    private static <T> NFAFragment<T> cloneFragment(final NFAFragment<T> original) {
        Map<NFAState<T>, NFAState<T>> clones = new IdentityHashMap<>();
        NFAState<T> newStart = cloneState(original.start, clones);
        Set<NFAState<T>> newAccepts = new HashSet<>();
        for (final NFAState<T> a: original.accepts) newAccepts.add(clones.get(a));
        return new NFAFragment<>(newStart, newAccepts);
    }

    private static <T> NFAState<T> cloneState(final NFAState<T> state, final Map<NFAState<T>, NFAState<T>> clones) {
        if (clones.containsKey(state)) return clones.get(state);
        NFAState<T> copy = new NFAState<T>();
        copy.groupStarts.addAll(state.groupStarts);
        copy.groupEnds.addAll(state.groupEnds);
        clones.put(state, copy);

        for (Map.Entry<Character, Set<NFAState<T>>> e : state.charTransitions.entrySet()) {
            for (final NFAState<T> target : e.getValue()) {
                copy.addCharTransition(e.getKey(), cloneState(target, clones));
            }
        }
        for (final NFAState<T> t: state.digitTransitions) copy.digitTransitions.add(cloneState(t, clones));
        for (final NFAState<T> t: state.anyCharTransitions) copy.anyCharTransitions.add(cloneState(t, clones));
        // character class transitions
        for (final CharClassTransition<T> t : state.charClassTransitions) {
            copy.charClassTransitions.add(new CharClassTransition<T>(t.predicate(), cloneState(t.target(), clones)));
        }
        for (final NFAState<T> t : state.epsilonTransitions) copy.epsilonTransitions.add(cloneState(t, clones));

        return copy;
    }

}
