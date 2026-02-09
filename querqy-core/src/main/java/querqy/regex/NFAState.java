package querqy.regex;

import java.util.*;

public final class NFAState {

    public record GroupStart(int group) {}
    public record GroupEnd(int group) {}

    // TODO: create these collections on demand only?

    // literal character transitions
    public final Map<Character, Set<NFAState>> charTransitions = new HashMap<>();

    // digit transition (\d)
    public final Set<NFAState> digitTransitions = new HashSet<>();

    // group starts / ends. These do not consume input. We're using them to index group matches.
    public final List<GroupStart> groupStarts = new ArrayList<>();
    public final List<GroupEnd> groupEnds = new ArrayList<>();

    // epsilon transitions
    public final Set<NFAState> epsilonTransitions = new HashSet<>();

    // filled later when combining regexes
    public final Set<RegexEntry> accepting = new HashSet<>();


    public void addCharTransition(final char c, final NFAState target) {
        charTransitions.computeIfAbsent(c, k -> new HashSet<>()).add(target);
    }

    public void addDigitTransition(final NFAState target) {
        digitTransitions.add(target);
    }

    public void addEpsilon(final NFAState target) {
        epsilonTransitions.add(target);
    }

    public void addGroupStart(final int group) {
        groupStarts.add(new GroupStart(group));
    }

    public void addGroupEnd(final int group) {
        groupEnds.add(new GroupEnd(group));
    }
}

