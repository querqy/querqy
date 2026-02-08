package querqy.regex;

import java.util.*;

public final class NFAState {

    // literal character transitions
    public final Map<Character, Set<NFAState>> charTransitions = new HashMap<>();

    // digit transition (\d)
    public final Set<NFAState> digitTransitions = new HashSet<>();

    // epsilon transitions
    public final Set<NFAState> epsilonTransitions = new HashSet<>();

    // filled later when combining regexes
    public final Set acceptingValues = new HashSet<>();

    public void addCharTransition(final char c, final NFAState target) {
        charTransitions
                .computeIfAbsent(c, k -> new HashSet<>())
                .add(target);
    }

    public void addDigitTransition(final NFAState target) {
        digitTransitions.add(target);
    }

    public void addEpsilon(final NFAState target) {
        epsilonTransitions.add(target);
    }
}

