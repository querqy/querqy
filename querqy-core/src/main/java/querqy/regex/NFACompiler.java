package querqy.regex;

import querqy.regex.Symbol.AnyDigitSymbol;
import querqy.regex.Symbol.CharSymbol;
import querqy.regex.Symbol.GroupSymbol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NFACompiler {

    public static NFAFragment compileSequence(final List<Symbol> symbols) {
        NFAFragment fragment = null;

        for (final Symbol symbol: symbols) {
            NFAFragment frag;
            if (symbol instanceof GroupSymbol gs) {
                frag = compileGroupSymbol(gs);
            } else {
                frag = compileAtomicSymbol(symbol);
            }
            fragment = concatenate(fragment, frag);

        }

        // empty regex case
        if (fragment == null) {
            NFAState s = new NFAState();
            return NFAFragment.single(s, s);
        }

        return fragment;
    }

    private static NFAFragment compileGroupSymbol(final GroupSymbol g) {
        NFAFragment inner = compileSequence(g.getChildren());

        // attach group markers if no quantifier
        if (g.getMinOccur() == 1 && g.getMaxOccur() == 1) {
            attachGroupMarkers(inner, g.getGroupIndex());
            return inner;
        }

        return applyGroupQuantifier(inner, g.getMinOccur(), g.getMaxOccur(), g.getGroupIndex());
    }

    // ---------- group quantifier: repetition with group markers ----------
    private static NFAFragment applyGroupQuantifier(NFAFragment frag, int min, int max, int groupIndex) {
        NFAFragment result = null;

        // mandatory repetitions
        for (int i = 0; i < min; i++) {
            NFAFragment copy = cloneFragment(frag);
            attachGroupMarkers(copy, groupIndex);
            result = concatenate(result, copy);
        }

        // optional repetitions
        int optionalCount = (max == Integer.MAX_VALUE) ? Integer.MAX_VALUE : max - min;
        NFAFragment current = result;
        for (int i = 0; i < optionalCount; i++) {
            NFAFragment copy = cloneFragment(frag);
            attachGroupMarkers(copy, groupIndex);

            NFAState skip = new NFAState();
            skip.addEpsilon(copy.start);
            for (NFAState a : current.accepts) a.addEpsilon(skip);
            current = NFAFragment.single(copy.start, skip);

            if (max != Integer.MAX_VALUE && i >= optionalCount - 1) break;
        }

        return current;
    }

    private static void attachGroupMarkers(NFAFragment frag, int groupIndex) {
        frag.start.addGroupStart(groupIndex);
        for (NFAState a : frag.accepts) a.addGroupEnd(groupIndex);
    }



    private static NFAFragment compileAtomicSymbol(final Symbol symbol) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();

        if (symbol instanceof CharSymbol cs) {
            start.addCharTransition(cs.getValue(), accept);
        } else if (symbol instanceof AnyDigitSymbol) {
            start.addDigitTransition(accept);
        } else {
            throw new IllegalArgumentException("Unexpected Symbol type");
        }

        NFAFragment frag = NFAFragment.single(start, accept);

        // handle repetition for atomic symbols (simple linear copy)
        if (symbol.getMinOccur() != 1 || symbol.getMaxOccur() != 1) {
            frag = applyAtomicQuantifier(frag, symbol.getMinOccur(), symbol.getMaxOccur());
        }

        return frag;
    }

    // ---------- atomic quantifier: simple repetition ----------
    private static NFAFragment applyAtomicQuantifier(NFAFragment frag, int min, int max) {
        NFAFragment result = null;
        // mandatory
        for (int i = 0; i < min; i++) {
            NFAFragment copy = cloneFragment(frag);
            result = concatenate(result, copy);
        }
        // optional
        int optional = (max == Integer.MAX_VALUE) ? Integer.MAX_VALUE : max - min;
        NFAFragment current = result;
        for (int i = 0; i < optional; i++) {
            NFAFragment copy = cloneFragment(frag);
            NFAState skip = new NFAState();
            skip.addEpsilon(copy.start);
            for (NFAState a : current.accepts) a.addEpsilon(skip);
            current = NFAFragment.single(copy.start, skip);
            if (max != Integer.MAX_VALUE && i >= max - min - 1) break;
        }
        return current;
    }

  /*  private static NFAFragment compileChar(final CharSymbol cs) {
        NFAState start = new NFAState();
        NFAState accept = new NFAState();

        start.addCharTransition(cs.getValue(), accept);
        return NFAFragment.single(start, accept);
    }

    private static NFAFragment compileDigit() {
        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addDigitTransition(accept);
        return NFAFragment.single(start, accept);
    }

    private static NFAFragment compileGroup(final GroupSymbol group) {
        NFAFragment inner = compileSequence(group.getChildren());

        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addGroupStart(group.getGroupIndex());
        start.addEpsilon(inner.start);

        for (final NFAState a: inner.accepts) {
            a.addGroupEnd(group.getGroupIndex());
            a.addEpsilon(accept);
        }

        return NFAFragment.single(start, accept);
    }

    private static NFAFragment applyQuantifier(final NFAFragment base, final int min, final int max) {
        // exactly once
        if (min == 1 && max == 1) {
            return base;
        }

        // ?
        if (min == 0 && max == 1) {
            return optional(base);
        }

        // +
        if (min == 1 && max == Integer.MAX_VALUE) {
            return oneOrMore(base);
        }

        // *
        if (min == 0 && max == Integer.MAX_VALUE) {
            return zeroOrMore(base);
        }

        // general {min,max}
        return boundedRepeat(base, min, max);
    }*/

    private static NFAFragment optional(final NFAFragment base) {
        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addEpsilon(base.start);
        start.addEpsilon(accept);

        for (final NFAState a : base.accepts) {
            a.addEpsilon(accept);
        }

        return NFAFragment.single(start, accept);
    }

    private static NFAFragment oneOrMore(final NFAFragment base) {
        for (final NFAState a : base.accepts) {
            a.addEpsilon(base.start);
        }
        return base;
    }

    private static NFAFragment zeroOrMore(final NFAFragment base) {
        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addEpsilon(base.start);
        start.addEpsilon(accept);

        for (final NFAState a : base.accepts) {
            a.addEpsilon(base.start);
            a.addEpsilon(accept);
        }

        return NFAFragment.single(start, accept);
    }

    private static NFAFragment boundedRepeat(final NFAFragment base, final int min, final int max) {
        NFAFragment result = null;

        // mandatory copies
        for (int i = 0; i < min; i++) {
            NFAFragment copy = cloneFragment(base);
            result = (result == null)
                    ? copy
                    : concatenate(result, copy);
        }

        // optional copies
        int optional = max - min;
        for (int i = 0; i < optional; i++) {
            NFAFragment copy = cloneFragment(base);
            result = concatenate(result, optional(copy));
        }

        return result;
    }

    private static NFAFragment concatenate(final NFAFragment a, final NFAFragment b) {
        if (a == null) return b;
        for (NFAState as : a.accepts) as.addEpsilon(b.start);
        return new NFAFragment(a.start, b.accepts);
    }

    private static NFAFragment cloneFragment(final NFAFragment original) {
        Map<NFAState, NFAState> map = new HashMap<>();
        NFAState newStart = cloneState(original.start, map);
        Set<NFAState> newAccepts = new HashSet<>();
        for (NFAState a : original.accepts) newAccepts.add(map.get(a));
        return new NFAFragment(newStart, newAccepts);
    }

    private static NFAState cloneState(final NFAState state, final Map<NFAState, NFAState> map) {
        if (map.containsKey(state)) return map.get(state);
        NFAState copy = new NFAState();
        copy.groupStarts.addAll(state.groupStarts);
        copy.groupEnds.addAll(state.groupEnds);
        map.put(state, copy);

        for (Map.Entry<Character, Set<NFAState>> e : state.charTransitions.entrySet()) {
            copy.charTransitions.put(e.getKey(), new HashSet<>());
            for (NFAState t : e.getValue()) copy.charTransitions.get(e.getKey()).add(cloneState(t, map));
        }
        for (NFAState t : state.digitTransitions) copy.digitTransitions.add(cloneState(t, map));
        for (NFAState t : state.epsilonTransitions) copy.epsilonTransitions.add(cloneState(t, map));
        copy.accepting.addAll(state.accepting);

        return copy;
    }

}
