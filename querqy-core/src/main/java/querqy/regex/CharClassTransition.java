package querqy.regex;

public record CharClassTransition(CharPredicate predicate, NFAState target) {}
