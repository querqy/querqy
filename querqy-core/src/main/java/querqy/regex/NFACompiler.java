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

    public static NFAFragment compile(final List<Symbol> symbols) {
        NFAFragment result = null;

        for (final Symbol symbol : symbols) {
            NFAFragment next = compileSymbol(symbol);
            if (result == null) {
                result = next;
            } else {
                // connect result → next
                for (NFAState accept : result.accepts()) {
                    accept.addEpsilon(next.start());
                }
                result = new NFAFragment(
                        result.start(),
                        next.accepts()
                );
            }
        }

        // empty regex case
        if (result == null) {
            NFAState s = new NFAState();
            return NFAFragment.single(s, s);
        }

        return result;
    }

    private static NFAFragment compileSymbol(final Symbol symbol) {
        final NFAFragment base = switch (symbol) {
            case CharSymbol cs -> compileChar(cs);
            case AnyDigitSymbol anyDigitSymbol -> compileDigit();
            case GroupSymbol gs -> compile(gs.getChildren());
            case null, default -> throw new IllegalStateException("Unknown Symbol type");
        };

        return applyQuantifier(base, symbol.getMinOccur(), symbol.getMaxOccur());
    }

    private static NFAFragment compileChar(final CharSymbol cs) {
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
    }

    private static NFAFragment optional(final NFAFragment base) {
        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addEpsilon(base.start());
        start.addEpsilon(accept);

        for (final NFAState a : base.accepts()) {
            a.addEpsilon(accept);
        }

        return NFAFragment.single(start, accept);
    }

    private static NFAFragment oneOrMore(final NFAFragment base) {
        for (final NFAState a : base.accepts()) {
            a.addEpsilon(base.start());
        }
        return base;
    }

    private static NFAFragment zeroOrMore(final NFAFragment base) {
        final NFAState start = new NFAState();
        final NFAState accept = new NFAState();

        start.addEpsilon(base.start());
        start.addEpsilon(accept);

        for (final NFAState a : base.accepts()) {
            a.addEpsilon(base.start());
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
        for (final NFAState s: a.accepts()) {
            s.addEpsilon(b.start());
        }
        return new NFAFragment(a.start(), b.accepts());
    }

    private static NFAFragment cloneFragment(final NFAFragment original) {
        final Map<NFAState, NFAState> map = new HashMap<>();
        cloneState(original.start(), map);

        final Set<NFAState> newAccepts = new HashSet<>();
        for (final NFAState a: original.accepts()) {
            newAccepts.add(map.get(a));
        }

        return new NFAFragment(map.get(original.start()), newAccepts);
    }

    private static void cloneState(final NFAState state, final Map<NFAState, NFAState> map) {
        if (map.containsKey(state)) return;

        final NFAState copy = new NFAState();
        map.put(state, copy);

        for (final var e : state.charTransitions.entrySet()) {
            for (final NFAState tgt : e.getValue()) {
                cloneState(tgt, map);
                copy.addCharTransition(e.getKey(), map.get(tgt));
            }
        }

        for (final NFAState tgt : state.digitTransitions) {
            cloneState(tgt, map);
            copy.addDigitTransition(map.get(tgt));
        }

        for (final NFAState tgt : state.epsilonTransitions) {
            cloneState(tgt, map);
            copy.addEpsilon(map.get(tgt));
        }
    }

}
