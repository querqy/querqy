package querqy.regex;

import java.util.Map;

public record MatchResult<T>(T value, Map<Integer, GroupMatch> groups) {
    public record GroupMatch(CharSequence match, int position) {}
}