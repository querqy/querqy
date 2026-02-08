package querqy.regex;

import java.util.List;
import java.util.Set;

public class RegexLookup {

    final NFAState start = new NFAState();

    public void put(final String regex, final Object value) {
        final List<Symbol> ast = RegexParser.parse(regex);
        final NFAFragment frag = NFACompiler.compile(ast);
        start.addEpsilon(frag.start());
        for (final NFAState a: frag.accepts()) {
            a.acceptingValues.add(value);
        }
    }

    public Set get(final String input) {
        return NFARunner.run(start, input);
    }
}
