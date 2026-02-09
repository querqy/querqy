package querqy.regex;

import java.util.Map;

public record MatchResult (Object value, Map<Integer, String> groups) {}