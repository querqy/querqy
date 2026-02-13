package querqy.regex;

import java.util.Map;

public record MatchResult<T>(T value, Map<Integer, CharSequence> groups) {}