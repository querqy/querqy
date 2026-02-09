package querqy.regex;

import querqy.regex.NFAState.GroupEnd;
import querqy.regex.NFAState.GroupStart;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NFAMatcher {

    public static MatchResult match(final NFAState start, final String input) {

        Set<ActiveState> current = epsilonClosure(Set.of(new ActiveState(start, new CaptureEvents())), 0);

        for (int pos = 0; pos < input.length(); pos++) {

            final char c = input.charAt(pos);
            final Set<ActiveState> next = new HashSet<>();

            for (final ActiveState as: current) {
                final NFAState s = as.state();
                final CaptureEvents cap = as.captures();

                // literal transitions
                Set<NFAState> literalTargets = s.charTransitions.get(c);
                if (literalTargets != null) {
                    for (final NFAState t: literalTargets) {
                        next.add(new ActiveState(t, cap.copy()));
                    }
                }

                // digit transitions
                if (Character.isDigit(c)) {
                    for (final NFAState t: s.digitTransitions) {
                        next.add(new ActiveState(t, cap.copy()));
                    }
                }
            }

            if (next.isEmpty()) {
                return null;
            }

            current = epsilonClosure(next, pos + 1);
        }

        // acceptance
        for (final ActiveState as: current) {
            for (final RegexEntry re: as.state().accepting) {
                return new MatchResult(re.value(), materializeGroups(as.captures(), re.groupCount(), input));
            }
        }

        return null;
    }

    private static Set<ActiveState> epsilonClosure(final Set<ActiveState> states, final int position) {
        final Set<ActiveState> closure = new HashSet<>(states);
        final Deque<ActiveState> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            final ActiveState cur = stack.pop();
            final NFAState s = cur.state();
            final CaptureEvents cap = cur.captures();

            // overwrite each time: last iteration wins
            for (final GroupStart gs: s.groupStarts) {
                cap.start.put(gs.group(), position);
            }

            // group end markers
            for (final GroupEnd ge: s.groupEnds) {
                cap.end.put(ge.group(), position);
            }

            for (final NFAState next: s.epsilonTransitions) {
                final ActiveState ns = new ActiveState(next, cap.copy());
                if (closure.add(ns)) {
                    stack.push(ns);
                }
            }
        }
        return closure;
    }

    private static Map<Integer, String> materializeGroups(final CaptureEvents events, final int groupCount,
                                                          final String input) {
        final Map<Integer, String> result = new HashMap<>();

        // group 0 = whole match
        result.put(0, input);

        for (int g = 1; g <= groupCount; g++) {
            final Integer s = events.start.get(g);
            final Integer e = events.end.get(g);
            if (s != null && e != null && s <= e) {
                result.put(g, input.substring(s, e));
            }
        }
        return result;
    }

    public static Set<MatchResult> matchAll(final NFAState start, final String input) {
        Set<MatchResult> results = new HashSet<>();

        Set<ActiveState> current = epsilonClosure(Set.of(new ActiveState(start, new CaptureEvents())),0);

        for (int pos = 0; pos < input.length(); pos++) {
            char c = input.charAt(pos);
            Set<ActiveState> next = new HashSet<>();

            for (ActiveState as : current) {
                NFAState s = as.state();
                CaptureEvents cap = as.captures();

                // literal transitions
                Set<NFAState> literalTargets = s.charTransitions.get(c);
                if (literalTargets != null) {
                    for (NFAState t : literalTargets) {
                        next.add(new ActiveState(t, cap.copy()));
                    }
                }

                // digit transitions
                if (Character.isDigit(c)) {
                    for (NFAState t : s.digitTransitions) {
                        next.add(new ActiveState(t, cap.copy()));
                    }
                }
            }

            if (next.isEmpty()) {
                return results;
            }

            current = epsilonClosure(next, pos + 1);
        }

        // collect matches from all accepting states
        for (ActiveState as : current) {
            for (RegexEntry re : as.state().accepting) {
                results.add(new MatchResult(
                        re.value(),
                        materializeGroups(as.captures(), re.groupCount(), input)
                ));
            }
        }

        return results;
    }

}


