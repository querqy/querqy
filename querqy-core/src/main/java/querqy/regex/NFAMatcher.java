package querqy.regex;

import querqy.regex.NFAState.GroupEnd;
import querqy.regex.NFAState.GroupStart;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NFAMatcher<T> {

    public MatchResult<T> match(final NFAState<T> start, final String input, final int offset) {

        Set<ActiveState<T>> current = epsilonClosure(Set.of(new ActiveState<>(start, new CaptureEvents())), 0);

        for (int pos = offset; pos < input.length(); pos++) {

            final char c = input.charAt(pos);
            final Set<ActiveState<T>> next = new HashSet<>();

            for (final ActiveState<T> as: current) {
                final NFAState<T> s = as.state;
                final CaptureEvents cap = as.captures;

                // literal transitions
                Set<NFAState<T>> literalTargets = s.charTransitions.get(c);
                if (literalTargets != null) {
                    for (final NFAState<T> t: literalTargets) {
                        next.add(fromCapture(t, cap, pos + 1));
                    }
                }

                for (final CharClassTransition<T> t: s.charClassTransitions) {
                    if (t.predicate().matches(c)) {
                        next.add(fromCapture(t.target(), cap, pos + 1));
//                        next.add(new ActiveState(t.target(), cap.copy()));
                    }
                }

                // digit transitions
                if (Character.isDigit(c)) {
                    for (final NFAState<T> t: s.digitTransitions) {
                        next.add(fromCapture(t, cap, pos + 1));
//                        next.add(new ActiveState(t, cap.copy()));
                    }
                }

                for (final NFAState<T> t: s.anyCharTransitions) {
                    next.add(fromCapture(t, cap, pos + 1));
                   // next.add(new ActiveState(t, cap.copy()));
                }
            }

            if (next.isEmpty()) {
                return null;
            }

            current = epsilonClosure(next, pos + 1);
        }

        // acceptance
        for (final ActiveState<T> as: current) {
            for (final RegexEntry<T> re: as.state.accepting) {
                return new MatchResult<T>(re.value(), materializeGroups(as.captures, re.groupCount(), input));
            }
        }

        return null;
    }

    private Set<ActiveState<T>> epsilonClosure(final Set<ActiveState<T>> states, final int position) {
        final Set<ActiveState<T>> closure = new HashSet<>(states);
        final Deque<ActiveState<T>> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            final ActiveState<T> cur = stack.pop();
            final NFAState<T> s = cur.state;

            for (final NFAState<T> next : s.epsilonTransitions) {

                // copy FIRST
                CaptureEvents cap = cur.captures.copy();

                // THEN apply group markers of *next*
                for (final GroupStart gs : next.groupStarts) {
                    cap.start.put(gs.group(), position);
                }
                for (final GroupEnd ge : next.groupEnds) {
                    cap.end.put(ge.group(), position);
                }

                final ActiveState<T> ns = new ActiveState<>(next, cap);

                if (closure.add(ns)) {
                    stack.push(ns);
                }
            }
        }


//
//        while (!stack.isEmpty()) {
//            final ActiveState cur = stack.pop();
//            final NFAState s = cur.state();
//            final CaptureEvents cap = cur.captures();
//
//            // overwrite each time: last iteration wins
//            for (final GroupStart gs: s.groupStarts) {
//                cap.start.put(gs.group(), position);
//            }
//
//            // group end markers
//            for (final GroupEnd ge: s.groupEnds) {
//                cap.end.put(ge.group(), position);
//            }
//
//            for (final NFAState next: s.epsilonTransitions) {
//                final ActiveState ns = new ActiveState(next, cap.copy());
//                if (closure.add(ns)) {
//                    stack.push(ns);
//                }
//            }
//        }
        return closure;
    }

    private static Map<Integer, CharSequence> materializeGroups(final CaptureEvents events, final int groupCount,
                                                          final String input) {
        final Map<Integer, CharSequence> result = new HashMap<>();

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

    private ActiveState<T> fromCapture(final NFAState<T> state, CaptureEvents cap, final int currentPos) {
        final ActiveState<T> copy = new ActiveState<T>(state, cap.copy());
        for (final GroupEnd groupEnd: state.groupEnds) {
            copy.captures.end.put(groupEnd.group(), currentPos + 1);
        }
        return copy;
    }

    public Set<MatchResult<T>> matchAll(final NFAState<T> start, final String input, final int offset) {
        Set<MatchResult<T>> results = new HashSet<>();

        Set<ActiveState<T>> current = epsilonClosure(Set.of(new ActiveState<>(start, new CaptureEvents())),0);

        for (int pos = offset; pos < input.length(); pos++) {
            char c = input.charAt(pos);
            Set<ActiveState<T>> next = new HashSet<>();

            for (ActiveState<T> as : current) {
                NFAState<T> s = as.state;
                CaptureEvents cap = as.captures;

                // literal transitions
                Set<NFAState<T>> literalTargets = s.charTransitions.get(c);
                if (literalTargets != null) {
                    for (final NFAState<T> t: literalTargets) {
                        next.add(fromCapture(t, cap, pos));
//                        next.add(new ActiveState(t, cap.copy()));
                    }
                }

                for (final CharClassTransition<T> t: s.charClassTransitions) {
                    if (t.predicate().matches(c)) {
                        next.add(fromCapture(t.target(), cap, pos));
                    }
                }

                // digit transitions
                if (Character.isDigit(c)) {
                    for (NFAState<T> t : s.digitTransitions) {
                        next.add(fromCapture(t, cap, pos));
                    }
                }

                for (final NFAState<T> t: s.anyCharTransitions) {
                    next.add(fromCapture(t, cap, pos));
//                    next.add(new ActiveState(t, cap.copy()));
                }
            }

            if (next.isEmpty()) {
                return results;
            }

            current = epsilonClosure(next, pos + 1);
        }

        // collect matches from all accepting states
        for (ActiveState<T> as : current) {
            for (RegexEntry<T> re : as.state.accepting) {
                results.add(new MatchResult<T>(re.value(), materializeGroups(as.captures, re.groupCount(), input)
                ));
            }
        }

        return results;
    }

}


