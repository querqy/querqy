package querqy.regex;

import java.util.Set;

public record NFAFragment(NFAState start, Set<NFAState> accepts) {

    public static NFAFragment single(final NFAState start, final NFAState accept) {
        return new NFAFragment(start, Set.of(accept));
    }
}

