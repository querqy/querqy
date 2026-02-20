package querqy.regex;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class NFADebugPrinter {


    public static <T> void print(NFAState<T> start) {

        Map<NFAState<T>, Integer> ids = assignIds(start);

        System.out.println("==== NFA Graph ====");

        for (Map.Entry<NFAState<T>, Integer> entry : ids.entrySet()) {

            NFAState<T> state = entry.getKey();
            int id = entry.getValue();

            System.out.println("S" + id + formatStateMeta(state));

            // ε transitions
            for (NFAState<T> next : state.epsilonTransitions) {
                System.out.println("  S" + id + " --ε--> S" + ids.get(next));
            }

            // char transitions
            for (var e : state.charTransitions.entrySet()) {
                char c = e.getKey();
                for (NFAState<T> next : e.getValue()) {
                    System.out.println("  S" + id + " --'" + printable(c) + "'--> S" + ids.get(next));
                }
            }

            // char class transitions
            for (final CharClassTransition<T> t: state.charClassTransitions) {
                final CharPredicate predicate = t.predicate();
                System.out.println("  S" + id + " --");
                if (predicate instanceof CharacterClass cs) {
                    System.out.print("[" + cs.ranges.stream()
                            .map(range -> range.from() + "-" + range.to())
                            .collect(Collectors.joining()) + "]");
                } else System.out.print(t.getClass().getSimpleName());
                System.out.println("--> S" + ids.get(t.target()));
            }
        }

        System.out.println("====================");
    }

    private static <T> Map<NFAState<T>, Integer> assignIds(NFAState<T> start) {

        Map<NFAState<T>, Integer> ids = new LinkedHashMap<>();
        Queue<NFAState<T>> queue = new ArrayDeque<>();

        ids.put(start, 0);
        queue.add(start);

        int nextId = 1;

        while (!queue.isEmpty()) {

            NFAState<T> current = queue.poll();

            for (NFAState<T> next : current.epsilonTransitions) {
                if (!ids.containsKey(next)) {
                    ids.put(next, nextId++);
                    queue.add(next);
                }
            }

            for (var list : current.charTransitions.values()) {
                for (NFAState<T> next : list) {
                    if (!ids.containsKey(next)) {
                        ids.put(next, nextId++);
                        queue.add(next);
                    }
                }
            }

            for (CharClassTransition<T> t : current.charClassTransitions) {
                NFAState<T> next = t.target();
                if (!ids.containsKey(next)) {
                    ids.put(next, nextId++);
                    queue.add(next);
                }
            }
        }

        return ids;
    }

    private static <T> String formatStateMeta(NFAState<T> state) {

        StringBuilder sb = new StringBuilder();

        if (!state.groupStarts.isEmpty()) {
            sb.append("  [groupStart=").append(state.groupStarts).append("]");
        }

        if (!state.groupEnds.isEmpty()) {
            sb.append("  [groupEnd=").append(state.groupEnds).append("]");
        }

//        if (state.accepting != null) {
//            sb.append("  [ACCEPT payload=").append(state.accepting.stream().map(r -> r.)pattern()).append("]");
//        }

        return sb.toString();
    }

    private static String printable(char c) {
        if (c == '\n') return "\\n";
        if (c == '\t') return "\\t";
        return String.valueOf(c);
    }

    public static <T> void printDot(NFAState<T> start) {
        Map<NFAState<T>, Integer> ids = assignIds(start);

        System.out.println("digraph NFA {");

        for (var entry : ids.entrySet()) {
            int id = entry.getValue();
            NFAState<T> state = entry.getKey();

            String shape = !state.accepting.isEmpty() ? "doublecircle" : "circle";
            String label = Integer.toString(id);
            if (!state.accepting.isEmpty()) {
                label += ",v=" + state.accepting.stream().map(v -> v.value().toString()).collect(
                        Collectors.joining(","));
            }
            if (!state.groupStarts.isEmpty()) {
                label += ",gs=" + state.groupStarts.stream().map(gs -> Integer.toString(gs.group())).collect(
                        Collectors.joining(","));
            }
            if (!state.groupEnds.isEmpty()) {
                label += ",ge=" + state.groupEnds.stream().map(gs -> Integer.toString(gs.group())).collect(
                        Collectors.joining(","));
            }
            System.out.println("  S" + id +  "[shape=" + shape + ", label=\"S" + label + "\"];");

            for (NFAState<T> next : state.epsilonTransitions) {
                System.out.println("  S" + id + " -> S" + ids.get(next) + " [label=\"ε\"];");
            }

            for (var e : state.charTransitions.entrySet()) {
                for (NFAState<T> next : e.getValue()) {
                    System.out.println("  S" + id + " -> S" + ids.get(next) +
                            " [label=\"" + printable(e.getKey()) + "\"];");
                }
            }

            for (final CharClassTransition<T> t: state.charClassTransitions) {
                System.out.println("  S" + id + " -> S" + ids.get(t.target()) +
                        " [label=\"CC\"];");


            }
        }

        System.out.println("}");
    }
}
