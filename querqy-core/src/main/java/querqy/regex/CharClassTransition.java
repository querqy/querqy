package querqy.regex;

public record CharClassTransition<T>(CharPredicate predicate, NFAState<T> target) {}
