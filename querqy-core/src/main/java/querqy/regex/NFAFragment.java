package querqy.regex;

import java.util.Set;

public class NFAFragment {

    final NFAState start;
    final Set<NFAState> accepts;

    public NFAFragment(final NFAState start, final Set<NFAState> accepts) {
        this.start = start;
        this.accepts = accepts;
    }
    public static NFAFragment single(final NFAState start, final NFAState accept) {
        return new NFAFragment(start, Set.of(accept));
    }
}

