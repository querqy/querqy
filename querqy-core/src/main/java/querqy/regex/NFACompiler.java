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

public final class NFACompiler {

    public static NFAFragment compileSequence(final List<Symbol> symbols) {

        NFAFragment fragment = null;

        for (final Symbol symbol: symbols) {
            NFAFragment piece = compileOne(symbol);
            fragment = concatenate(fragment, piece);
        }

        return (fragment == null) ? NFAFragment.empty() : fragment;
    }

    private static NFAFragment compileAlternation(AlternationSymbol alt) {
        NFAState start = new NFAState();
        Set<NFAState> accepts = new HashSet<>();

        for (List<Symbol> branch : alt.alternatives) {
            NFAFragment frag = compileSequence(branch);
            start.addEpsilon(frag.start);
            accepts.addAll(frag.accepts);
        }

        return new NFAFragment(start, accepts);
    }

    private static NFAFragment compileOne(Symbol s) {
        if (s instanceof AlternationSymbol a) {
            return compileAlternation(a);
        }
        NFAFragment base = makeBaseFragment(s);
        return applyQuantifier(base, s.getMinOccur(), s.getMaxOccur(), s instanceof GroupSymbol ? ((GroupSymbol)s).getGroupIndex() : -1);
    }

    private static NFAFragment makeBaseFragment(Symbol s) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();

        if (s instanceof CharSymbol cs) start.addCharTransition(cs.getValue(), accept);
        else if (s instanceof AnyDigitSymbol) start.addDigitTransition(accept);
        else if (s instanceof AnyCharSymbol) start.anyCharTransitions.add(accept);
        else if (s instanceof CharClassSymbol cc) {
            start.charClassTransitions.add(new CharClassTransition(cc::matches, accept));
        } else if (s instanceof GroupSymbol gs) {
            NFAFragment inner = compileSequence(gs.getChildren());
            attachGroupMarkers(inner, gs.getGroupIndex());
            return inner;
        }
        return new NFAFragment(start, Set.of(accept));
    }

    private static NFAFragment applyQuantifier(NFAFragment frag, int min, int max, int groupIdx) {
        if (min==1 && max==1) return cloneFragment(frag);

        NFAFragment result;
        if (min == 0) {
            result = NFAFragment.empty();
        } else {
            result = null;
            for (int i = 0; i < min; i++) {
                NFAFragment copy = cloneFragment(frag);
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
            NFAFragment copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            for (NFAState a : result.accepts) {
                a.addEpsilon(copy.start);
            }
            for (NFAState a : copy.accepts) {
                a.addEpsilon(copy.start);
            }

            Set<NFAState> accepts = new HashSet<>();
            accepts.addAll(result.accepts);  // allow stopping at min
            accepts.addAll(copy.accepts);    // allow stopping after more

            return new NFAFragment(result.start, accepts);

        }

        Set<NFAState> allAccepts = new HashSet<>(result.accepts);

        int optional = max - min;
        NFAFragment current = result;
        for (int i = 0; i < optional; i++) {
            NFAFragment copy = cloneFragment(frag);
            if (groupIdx >= 0) attachGroupMarkers(copy, groupIdx);

            // allow skipping this repetition
            for (NFAState a : current.accepts) {
                a.addEpsilon(copy.start);
            }

            current = copy;
            allAccepts.addAll(copy.accepts);

            result = new NFAFragment(result.start, allAccepts);
        }

        return result;

    }


    private static void attachGroupMarkers(NFAFragment frag, int groupIndex) {
        frag.start.addGroupStart(groupIndex);
        for (NFAState a : frag.accepts) a.addGroupEnd(groupIndex);
    }

    private static NFAFragment concatenate(final NFAFragment a, final NFAFragment b) {
        if (a == null) return b;
        for (NFAState as : a.accepts) as.addEpsilon(b.start);
        return new NFAFragment(a.start, b.accepts);
    }

    private static NFAFragment cloneFragment(final NFAFragment original) {
        Map<NFAState, NFAState> clones = new IdentityHashMap<>();
        NFAState newStart = cloneState(original.start, clones);
        Set<NFAState> newAccepts = new HashSet<>();
        for (NFAState a : original.accepts) newAccepts.add(clones.get(a));
        return new NFAFragment(newStart, newAccepts);
    }

    private static NFAState cloneState(final NFAState state, final Map<NFAState, NFAState> clones) {
        if (clones.containsKey(state)) return clones.get(state);
        NFAState copy = new NFAState();
        copy.groupStarts.addAll(state.groupStarts);
        copy.groupEnds.addAll(state.groupEnds);
        clones.put(state, copy);

        for (Map.Entry<Character, Set<NFAState>> e : state.charTransitions.entrySet()) {
            for (final NFAState target : e.getValue()) {
                copy.addCharTransition(
                        e.getKey(),
                        cloneState(target, clones)
                );
            }
        }
        for (NFAState t : state.digitTransitions) copy.digitTransitions.add(cloneState(t, clones));
        for (NFAState t : state.anyCharTransitions) copy.anyCharTransitions.add(cloneState(t, clones));
        // character class transitions
        for (final CharClassTransition t : state.charClassTransitions) {
            copy.charClassTransitions.add(new CharClassTransition(t.predicate(), cloneState(t.target(), clones)));
        }
        for (final NFAState t : state.epsilonTransitions) copy.epsilonTransitions.add(cloneState(t, clones));

        return copy;
    }

}
