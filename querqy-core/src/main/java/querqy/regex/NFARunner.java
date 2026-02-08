package querqy.regex;

import java.util.*;

public final class NFARunner {

    public static <T> Set<T> run(final NFAState start, final String input) {
        // initial ε-closure
        Set<NFAState> current = NFARunnerUtil.epsilonClosure(Set.of(start));

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final Set<NFAState> next = new HashSet<>();

            for (final NFAState state: current) {

                // literal transitions
                final Set<NFAState> charTargets = state.charTransitions.get(c);
                if (charTargets != null) {
                    next.addAll(charTargets);
                }

                // digit transitions
                if (Character.isDigit(c)) {
                    next.addAll(state.digitTransitions);
                }
            }

            // no states reachable → dead
            if (next.isEmpty()) {
                return Collections.emptySet();
            }

            // ε-closure again
            current = NFARunnerUtil.epsilonClosure(next);
        }

        // collect accepting values
        final Set<T> results = new HashSet<>();
        for (final NFAState state : current) {
            results.addAll(state.acceptingValues);
        }

        return results;
    }
}

