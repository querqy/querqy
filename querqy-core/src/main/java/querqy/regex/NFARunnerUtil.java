package querqy.regex;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

final class NFARunnerUtil {

    static Set<NFAState> epsilonClosure(final Set<NFAState> states) {
        final Set<NFAState> closure = new HashSet<>(states);
        final Deque<NFAState> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            final NFAState s = stack.pop();
            for (final NFAState next: s.epsilonTransitions) {
                if (closure.add(next)) {
                    stack.push(next);
                }
            }
        }
        return closure;
    }
}

