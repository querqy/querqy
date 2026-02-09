package querqy.regex;

import java.util.List;
import java.util.Set;

public class RegexLookup {

    final NFAState start = new NFAState();

    public void put(final String regex, final Object value) {
        final RegexParser parser = new RegexParser();
        final List<Symbol> ast = parser.parse(regex);

        final NFAFragment frag = NFACompiler.compileSequence(ast);
        start.addEpsilon(frag.start);
        for (final NFAState a: frag.accepts) {
            a.accepting.add(new RegexEntry(value, parser.getGroupCount()));
        }
    }

    public Set<MatchResult> get(final String input) {
        System.out.println(NFAMatcher.match(start, input));
        return NFAMatcher.matchAll(start, input);//NFARunner.run(start, input);
    }
}
