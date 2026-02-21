package querqy.regex;

import querqy.trie.Node;

import java.util.*;

public final class NFAState<T> {

    public record GroupStart(int group) {}
    public record GroupEnd(int group) {}

    // TODO: create these collections on demand only?

    // literal character transitions
    public final Map<Character, Set<NFAState<T>>> charTransitions = new HashMap<>();

    public final Set<CharClassTransition<T>> charClassTransitions = new HashSet<>();

    // group starts / ends. These do not consume input. We're using them to index group matches.
    public final List<GroupStart> groupStarts = new ArrayList<>();
    public final List<GroupEnd> groupEnds = new ArrayList<>();

    // epsilon transitions
    public final Set<NFAState<T>> epsilonTransitions = new HashSet<>();

    // filled later when combining regexes
    public final Set<RegexEntry<T>> accepting = new HashSet<>();


    public void addCharTransition(final char c, final NFAState<T> target) {
        charTransitions.computeIfAbsent(c, k -> new HashSet<>()).add(target);
    }

    public void addCharTransitions(final char c, final Set<NFAState<T>> target) {
        charTransitions.computeIfAbsent(c, k -> new HashSet<>()).addAll(target);
    }

    public void addCharClassTransition(final CharClassTransition<T> transition) {
        charClassTransitions.add(transition);
    }

    public void addEpsilon(final NFAState<T> target) {
        epsilonTransitions.add(target);
    }

    public void addGroupStart(final int group) {
        groupStarts.add(new GroupStart(group));
    }

    public void addGroupStart(final GroupStart gs) {
        groupStarts.add(gs);
    }

    public void addGroupEnd(final int group) {
        groupEnds.add(new GroupEnd(group));
    }

    public void addGroupEnd(final GroupEnd ge) {
        groupEnds.add(ge);
    }
}

