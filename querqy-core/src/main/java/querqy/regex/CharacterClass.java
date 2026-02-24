package querqy.regex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CharacterClass {

    record Range(char from, char to) {
        boolean contains(final char c) {
            return c >= from && c <= to;
        }
    }

    final Set<Character> singles = new HashSet<>();
    final List<Range> ranges = new ArrayList<>();
    final List<CharacterClass> intersections = new ArrayList<>();
    boolean negated = false;

    boolean matches(final char c) {
        boolean base = singles.contains(c) || ranges.stream().anyMatch(r -> r.contains(c));

        for (final CharacterClass cc: intersections) {
            base &= cc.matches(c);
        }

        return negated != base;
    }
}
