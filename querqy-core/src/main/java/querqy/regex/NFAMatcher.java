package querqy.regex;

import querqy.regex.ActiveState.SuffixActiveState;
import querqy.regex.MatchResult.GroupMatch;
import querqy.regex.NFAState.GroupEnd;
import querqy.regex.NFAState.GroupStart;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NFAMatcher<T> {

    private Set<ActiveState<T>> epsilonClosure(final Set<ActiveState<T>> states, final int position) {
        final Set<ActiveState<T>> closure = new HashSet<>(states);
        final Deque<ActiveState<T>> stack = new ArrayDeque<>(states);

        while (!stack.isEmpty()) {
            final ActiveState<T> cur = stack.pop();
            final NFAState.SuffixTransition<T> suffixTransition;
            final int groupDelta;
            if (cur instanceof SuffixActiveState<T> suffixActiveState) {
                suffixTransition = suffixActiveState.suffixTransition;
                groupDelta = suffixTransition.groupsBeforeSuffix();
            } else {
                suffixTransition = null;
                groupDelta = 0;
            }

            final NFAState<T> s = cur.state;

            for (final NFAState<T> next : s.epsilonTransitions) {

                // copy FIRST
                CaptureEvents cap = cur.captures.copy();

                // THEN apply group markers of *next*
                for (final GroupStart gs : next.groupStarts) {
                    cap.start.put(gs.group() + groupDelta, position);
                }
                for (final GroupEnd ge : next.groupEnds) {
                    cap.end.put(ge.group() + groupDelta, position);
                }

                final ActiveState<T> ns = groupDelta > 0
                        ? new SuffixActiveState<>(next, cap, suffixTransition) : new ActiveState<>(next, cap);

                if (closure.add(ns)) {
                    stack.push(ns);
                }
            }


            for (final NFAState.SuffixTransition<T> sTransition: s.getSuffixTransitions()) {
                // copy FIRST
                CaptureEvents cap = cur.captures.copy();

                // THEN apply group markers of *next*
                for (final GroupStart gs : sTransition.suffix().start().groupStarts) {
                    cap.start.put(gs.group() + sTransition.groupsBeforeSuffix(), position);
                }
                for (final GroupEnd ge : sTransition.suffix().start().groupEnds) {
                    cap.end.put(ge.group() +  sTransition.groupsBeforeSuffix(), position);
                }

                final SuffixActiveState<T> ns = new SuffixActiveState<>(sTransition.suffix().start(), cap, sTransition);

                if (closure.add(ns)) {
                    stack.push(ns);
                }
            }

        }

        return closure;
    }

    private static Map<Integer, GroupMatch> materializeGroups(final CaptureEvents events, final int groupCount,
                                                                          final CharSequence input) {
        final Map<Integer, GroupMatch> result = new HashMap<>();

        // group 0 = whole match
        result.put(0, new GroupMatch(input, 0));

        for (int g = 1; g <= groupCount; g++) {
            final Integer s = events.start.get(g);
            final Integer e = events.end.get(g);
            if (s != null && e != null && s <= e) {
                result.put(g, new GroupMatch(input.subSequence(s, e), s));
            }
        }
        return result;
    }

    private ActiveState<T> fromCapture(final NFAState<T> state, CaptureEvents cap, final ActiveState<T> source, final int currentPos) {
        final ActiveState<T> copy;
        final int groupDelta;
        if (source instanceof ActiveState.SuffixActiveState<T> suffixActiveState) {
            copy = new SuffixActiveState<>(state, cap.copy(), suffixActiveState.suffixTransition);
            groupDelta = suffixActiveState.suffixTransition.groupsBeforeSuffix();
        } else {
            copy = new ActiveState<T>(state, cap.copy());
            groupDelta = 0;
        }

        for (final GroupEnd groupEnd: state.groupEnds) {
            copy.captures.end.put(groupEnd.group() + groupDelta, currentPos + 1);
        }
        return copy;
    }

    public Set<MatchResult<T>> matchAll(final NFAState<T> start, final CharSequence input, final int offset) {
        Set<MatchResult<T>> results = new HashSet<>();

        // deal with start node:
        final CaptureEvents capStart = new CaptureEvents();

        for (final GroupStart gs : start.groupStarts) {
            capStart.start.put(gs.group(), offset);
        }
        for (final GroupEnd ge : start.groupEnds) {
            capStart.end.put(ge.group(), offset);
        }


        Set<ActiveState<T>> current = epsilonClosure(Set.of(new ActiveState<>(start, capStart)), offset);

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
                        next.add(fromCapture(t, cap, as, pos));
                    }
                }

                for (final CharClassTransition<T> t: s.charClassTransitions) {
                    if (t.predicate().matches(c)) {
                        next.add(fromCapture(t.target(), cap, as, pos));
                    }
                }

            }

            if (next.isEmpty()) {
                // TODO suffixes
                return results;
            }

            current = epsilonClosure(next, pos + 1);
        }

        // collect matches from all accepting states
        for (final ActiveState<T> as: current) {
            for (final RegexEntry<T> re: as.getAccepting()) {
                results.add(new MatchResult<>(re.value(), materializeGroups(as.captures, re.groupCount(), input)));
            }
        }

        return results;
    }

}


