package querqy.regex;

import java.util.List;
import java.util.Set;

public class RegexLookup<T> {

    final NFAState<T> start = new NFAState<T>();
    final NFAMatcher<T> matcher = new NFAMatcher<>();

    public void put(final String regex, final T value) {
        final RegexParser parser = new RegexParser();
        final List<Symbol> ast = parser.parse(regex);
        final NFACompiler<T> compiler = new NFACompiler<>();

        final NFAFragment<T> frag = compiler.compileSequence(ast);
        start.addEpsilon(frag.start);
        for (final NFAState<T> a: frag.accepts) {
            a.accepting.add(new RegexEntry<>(value, parser.getGroupCount()));
        }
    }

    public Set<MatchResult<T>> getAll(final String input) {

        return matcher.matchAll(start, input, 0);//NFARunner.run(start, input);
    }
}
